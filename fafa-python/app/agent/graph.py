"""Phase 5 LangGraph Agent。"""

from datetime import date
from typing import Any

from langchain_core.messages import HumanMessage, SystemMessage
from langgraph.graph import END, StateGraph
from loguru import logger

from app.agent.date_utils import parse_date_range
from app.agent.intent import Intent, classify_intent
from app.agent.prompts import SYSTEM_PROMPT, build_evidence_prompt
from app.agent.state import ChatState
from app.agent.tools.registry import ToolRegistry
from app.agent.tools.java_client import JavaToolError
from app.core.llm import Purpose, get_chat_model
from app.schemas.chat import ChatSendRequest
from app.service.record_parser import parse_record_message


class AgentError(RuntimeError):
    """Agent 无法生成可靠响应。"""


class ChatAgent:
    """以 LangGraph 编排意图、工具和回答节点。"""

    def __init__(self, tool_registry: ToolRegistry | None = None) -> None:
        self.tool_registry = tool_registry or ToolRegistry()
        self.graph = self._build_graph()

    async def chat(self, request: ChatSendRequest) -> dict[str, Any]:
        state = await self.graph.ainvoke({"request": request})
        return {
            "answer": state.get("answer", "暂时无法回答这个问题。"),
            "blocks": state.get("blocks", []),
            "citations": state.get("citations", []),
            "suggested_questions": state.get("suggested_questions", []),
            "record_draft": state.get("record_draft"),
            "tool_calls": state.get("tool_calls", []),
            "usage": state.get("usage"),
            "trace_id": request.trace_id,
        }

    def _build_graph(self):
        graph = StateGraph(ChatState)
        graph.add_node("normalize_context", self._normalize_context)
        graph.add_node("plan_tools", self._plan_tools)
        graph.add_node("execute_tools", self._execute_tools)
        graph.add_node("synthesize_answer", self._synthesize_answer)
        graph.add_node("safety_check", self._safety_check)
        graph.set_entry_point("normalize_context")
        graph.add_edge("normalize_context", "plan_tools")
        graph.add_edge("plan_tools", "execute_tools")
        graph.add_edge("execute_tools", "synthesize_answer")
        graph.add_edge("synthesize_answer", "safety_check")
        graph.add_edge("safety_check", END)
        return graph.compile()

    async def _normalize_context(self, state: ChatState) -> dict[str, Any]:
        request = state["request"]
        start, end = parse_date_range(request.message)
        return {
            "intent": classify_intent(request.message),
            "start_date": start.isoformat(),
            "end_date": end.isoformat(),
        }

    async def _plan_tools(self, state: ChatState) -> dict[str, Any]:
        intent = state["intent"]
        message = state["request"].message
        plans = {
            Intent.DATA_QUERY: _data_tools(message),
            Intent.TREND_ANALYSIS: ["getPetProfile", "getWeightHistory"],
            Intent.EXPENSE_QUERY: _expense_tools(message),
            Intent.REPORT_QUERY: ["getPetReports"],
            Intent.MEDIA_SEARCH: ["searchPetPhotos"],
            Intent.RECORD_DRAFT: [],
            Intent.UNSUPPORTED: [],
            Intent.GENERAL: [],
        }
        return {"tool_plan": plans[intent]}

    async def _execute_tools(self, state: ChatState) -> dict[str, Any]:
        request = state["request"]
        intent = state["intent"]
        if intent == Intent.RECORD_DRAFT:
            return {"evidence": [], "tool_calls": []}
        if intent in (Intent.UNSUPPORTED, Intent.GENERAL):
            return {"evidence": [], "tool_calls": []}
        if request.pet_id is None:
            raise AgentError("缺少宠物上下文")
        start = date.fromisoformat(state["start_date"])
        end = date.fromisoformat(state["end_date"])
        try:
            evidence, calls = await self.tool_registry.execute(
                state["tool_plan"], request.user_id, request.pet_id, request.message,
                start, end, request.trace_id,
            )
        except JavaToolError as exc:
            logger.error("Agent 工具调用失败: trace_id={}, status={}",
                         request.trace_id, exc.status_code)
            raise AgentError("工具调用失败") from exc
        return {"evidence": evidence, "tool_calls": calls}

    async def _synthesize_answer(self, state: ChatState) -> dict[str, Any]:
        request = state["request"]
        intent = state["intent"]
        if intent == Intent.RECORD_DRAFT:
            parsed = parse_record_message(request.pet_id or 0, request.message)
            records = parsed["records"]
            return {
                "answer": "我整理了一份记录草稿，请在对应页面确认后保存。" if records else parsed["warnings"][0],
                "blocks": [{"type": "link", "title": "去确认记录", "pageUrl": records[0]["pageUrl"]}]
                if records else [],
                "record_draft": parsed if records else None,
                "citations": [],
                "suggested_questions": [],
            }
        if intent == Intent.UNSUPPORTED:
            return {"answer": "报告和内容创作将在后续阶段开放，当前可以先查询记录或查找照片/视频。"}
        if intent == Intent.GENERAL:
            return {"answer": "我可以帮你查询豆包的历史记录、体重趋势、喂食和照片/视频。请告诉我想查询的时间范围。"}
        answer, usage = await self._ask_model(request, state.get("evidence", []))
        return {
            "answer": answer,
            "blocks": _build_blocks(state.get("evidence", []), intent),
            "citations": _build_citations(state.get("evidence", [])),
            "suggested_questions": _suggestions(intent),
            "usage": usage,
        }

    async def _ask_model(self, request: ChatSendRequest,
                         evidence: list[dict[str, Any]]) -> tuple[str, dict[str, Any]]:
        try:
            model = get_chat_model(Purpose.CHAT_DEEP)
            messages = [SystemMessage(content=SYSTEM_PROMPT)]
            messages.extend(HumanMessage(content=item.content) for item in request.history)
            messages.append(HumanMessage(content=build_evidence_prompt(evidence)))
            messages.append(HumanMessage(content=request.message))
            response = await model.ainvoke(messages)
        except Exception as exc:
            logger.error("Agent 模型调用失败: trace_id={}", request.trace_id, exc_info=True)
            raise AgentError("模型调用失败") from exc
        content = response.content
        if isinstance(content, list):
            content = "".join(str(item.get("text", item)) if isinstance(item, dict) else str(item)
                                for item in content)
        answer = str(content).strip()
        if not answer:
            raise AgentError("模型返回内容为空")
        usage = getattr(response, "response_metadata", {}).get("token_usage", {})
        return answer, {
            "model": get_settings_model_name(),
            "prompt_tokens": usage.get("prompt_tokens"),
            "completion_tokens": usage.get("completion_tokens"),
        }

    async def _safety_check(self, state: ChatState) -> dict[str, Any]:
        answer = state.get("answer", "")
        if any(word in answer for word in ("诊断为", "确诊", "处方", "服用剂量")):
            logger.warning("Agent 回答触发医疗边界: trace_id={}", state["request"].trace_id)
            return {"answer": "我只能描述 FaFa 中记录到的数据变化，不能进行诊断或给出用药处方；如有担忧请咨询兽医。"}
        return {}


def _data_tools(message: str) -> list[str]:
    if any(word in message for word in ("食品卡", "猫粮", "狗粮", "配方", "蛋白", "营养")):
        return ["getFoodInfo"]
    if any(word in message for word in ("喂食计划", "喂养计划", "计划餐", "今日喂食")):
        return ["getFeedingPlan", "getFeedingHistory"]
    if any(word in message for word in ("拒食", "食欲", "精神", "饮水", "喝水", "排便")):
        return ["getDailyStatusHistory"]
    if any(word in message for word in ("喂食", "吃了", "吃多少", "猫粮")):
        return ["getFeedingHistory"]
    if "提醒" in message:
        return ["getReminderHistory"]
    if "事件" in message or "发生" in message:
        return ["getEventHistory"]
    return ["getDailyStatusHistory"]


def _expense_tools(message: str) -> list[str]:
    if any(word in message for word in ("统计", "总共", "合计", "趋势", "占比", "多少")):
        return ["getExpenseStatistics", "getExpenseHistory"]
    return ["getExpenseHistory"]


def _build_blocks(evidence: list[dict[str, Any]], intent: Intent) -> list[dict[str, Any]]:
    if intent == Intent.MEDIA_SEARCH:
        media = evidence[0].get("data", {}).get("results", []) if evidence else []
        return [{"type": "media", "items": media}] if media else []
    if intent == Intent.TREND_ANALYSIS:
        records = evidence[-1].get("data", {}).get("records", []) if evidence else []
        return [{"type": "chart", "chartType": "line", "records": records}] if records else []
    if intent == Intent.EXPENSE_QUERY:
        for item in evidence:
            data = item.get("data", {})
            if "categoryBreakdown" in data or "trend" in data:
                return [{"type": "chart", "chartType": "expense", "statistics": data}]
        return []
    if intent == Intent.REPORT_QUERY:
        records = evidence[0].get("data", {}).get("records", []) if evidence else []
        return [{"type": "report", "items": records}] if records else []
    
    return []


def _build_citations(evidence: list[dict[str, Any]]) -> list[dict[str, Any]]:
    citations: list[dict[str, Any]] = []
    for item in evidence:
        data = item.get("data", {})
        source_type = "media_asset" if "results" in data else item.get("tool", "agent_tool")
        records = data.get("records") or data.get("results") or []
        for record in records[:20] if isinstance(records, list) else []:
            if isinstance(record, dict):
                source_id = record.get("id") or record.get("mediaId") or record.get("recordDate")
                if source_id is not None:
                    citations.append({"source_type": source_type, "source_id": str(source_id)})
    return citations


def _suggestions(intent: Intent) -> list[str]:
    if intent == Intent.MEDIA_SEARCH:
        return ["再按标签筛选一下", "找这段时间的成长事件"]
    if intent == Intent.TREND_ANALYSIS:
        return ["这段时间有几天拒食？", "最近喂食次数是多少？"]
    if intent == Intent.EXPENSE_QUERY:
        return ["今年医疗费用占比是多少？", "最近一个月买食品花了多少钱？"]
    if intent == Intent.REPORT_QUERY:
        return ["查看最近一份月报", "帮我找最近的成长事件"]
    return ["豆包最近体重怎么样？", "帮我找最近的照片和视频"]


def get_settings_model_name() -> str:
    """读取当前对话模型名称，保持模型路由的单一入口。"""

    from app.core.config import get_settings

    return get_settings().model_chat_deep
