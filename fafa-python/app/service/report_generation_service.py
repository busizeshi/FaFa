"""基于 Java 事实快照生成结构化报告表达层。"""

import asyncio
import json
from typing import Any

from langchain_core.messages import HumanMessage, SystemMessage
from loguru import logger

from app.core.config import get_settings
from app.core.llm import Purpose, get_chat_model
from app.schemas.report import ReportContent, ReportGenerationRequest


class ReportGenerationService:
    """不查询业务数据库，只处理 Java 传入的不可变快照。"""

    async def generate(self, request: ReportGenerationRequest) -> dict[str, Any]:
        facts = self._parse_facts(request.fact_snapshot_json)
        settings = get_settings()
        if not settings.dashscope_api_key:
            return self._response(self._fallback(facts, request), "deterministic", settings.model_chat_deep, request)
        try:
            content = await self._ask_model(facts, request)
            return self._response(content, "ai", settings.model_chat_deep, request)
        except Exception:
            logger.warning("报告 AI 生成失败，返回确定性报告", exc_info=True)
            return self._response(self._fallback(facts, request), "deterministic", settings.model_chat_deep, request)

    async def _ask_model(self, facts: dict[str, Any], request: ReportGenerationRequest) -> ReportContent:
        settings = get_settings()
        model = get_chat_model(Purpose.CHAT_DEEP)
        prompt = (
            "你是宠物成长报告编辑。只允许根据下面的 Java 事实快照写报告，不能诊断、不能推断未记录的事实。"
            "只返回 JSON，字段必须为 title, summary, highlights, careSuggestions, timeline, mediaSuggestions, sourceKeys。"
            "timeline 每项包含 date,title,detail,sourceKey；mediaSuggestions 每项包含 mediaId,reason,sourceKey。"
            f"周期：{request.period_type} {request.start_date} 至 {request.end_date}\n事实快照：{request.fact_snapshot_json}"
        )
        response = await asyncio.wait_for(
            model.ainvoke([SystemMessage(content="只做事实整理，不做医疗诊断。"), HumanMessage(content=prompt)]),
            timeout=settings.agent_timeout_seconds,
        )
        value = _parse_json(_content_text(response.content))
        return ReportContent.model_validate(value)

    def _fallback(self, facts: dict[str, Any], request: ReportGenerationRequest) -> ReportContent:
        pet = facts.get("pet", {})
        name = pet.get("name", "宠物") if isinstance(pet, dict) else "宠物"
        daily = _list(facts.get("dailyStatuses"))
        feeding = _list(facts.get("feeding"))
        events = _list(facts.get("events"))
        media = _list(facts.get("media"))
        timeline = [{"date": item.get("date"), "title": "每日状态", "detail": item.get("notes", "已记录"),
                     "sourceKey": f"dailyStatuses:{item.get('date')}"} for item in daily[:20]]
        media_suggestions = [{"mediaId": item.get("id"), "reason": "周期内素材", "sourceKey": f"media:{item.get('id')}"}
                             for item in media[:12] if item.get("id") is not None]
        return ReportContent(
            title=f"{name}的{request.period_type}报告",
            summary=f"本周期记录每日状态 {len(daily)} 天、喂食 {len(feeding)} 次、事件 {len(events)} 条。",
            highlights=["报告内容仅基于已记录事实"],
            careSuggestions=["继续保持规律记录，便于后续比较变化"],
            timeline=timeline, mediaSuggestions=media_suggestions,
            sourceKeys=[item["sourceKey"] for item in timeline + media_suggestions],
        )

    @staticmethod
    def _parse_facts(value: str) -> dict[str, Any]:
        parsed = json.loads(value)
        if not isinstance(parsed, dict):
            raise ValueError("事实快照必须是 JSON 对象")
        return parsed

    @staticmethod
    def _response(content: ReportContent, mode: str, model: str,
                  request: ReportGenerationRequest) -> dict[str, Any]:
        return {"content": content.model_dump(by_alias=True), "mode": mode,
                "modelName": model, "promptVersion": request.prompt_version}


def _list(value: Any) -> list[dict[str, Any]]:
    return value if isinstance(value, list) else []


def _parse_json(text: str) -> dict[str, Any]:
    cleaned = text.strip().removeprefix("```json").removeprefix("```").removesuffix("```").strip()
    value = json.loads(cleaned)
    if not isinstance(value, dict):
        raise ValueError("模型输出必须是 JSON 对象")
    return value


def _content_text(content: Any) -> str:
    if isinstance(content, str):
        return content
    if isinstance(content, list):
        return "".join(str(item.get("text", item)) if isinstance(item, dict) else str(item) for item in content)
    return str(content)


report_generation_service = ReportGenerationService()
