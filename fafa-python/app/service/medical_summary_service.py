"""医疗摘要事实汇总与可选 AI 润色。"""

import asyncio
from datetime import date
from typing import Any

from langchain_core.messages import HumanMessage, SystemMessage
from loguru import logger

from app.agent.tools.java_client import JavaQueryClient, JavaToolError
from app.core.config import get_settings
from app.core.llm import Purpose, get_chat_model
from app.schemas.medical import MedicalSummaryRequest


DISCLAIMER = "以上内容仅整理 FaFa 中已记录的事实，不构成诊断、处方或治疗建议；如有担忧请咨询兽医。"


class MedicalSummaryService:
    """跨 Java 查询事实并生成可追溯摘要。"""

    def __init__(self, java_client: JavaQueryClient | None = None) -> None:
        self.java_client = java_client or JavaQueryClient()

    async def generate(self, request: MedicalSummaryRequest) -> dict[str, Any]:
        start = date.fromisoformat(request.start_date)
        end = date.fromisoformat(request.end_date)
        facts, warnings = await self._load_facts(request, start, end)
        summary = self._build_summary(request, facts, warnings, start, end)
        summary["overview"] = await self._generate_overview(request, summary, warnings)
        summary["plainText"] = self._plain_text(summary)
        return summary

    async def _load_facts(self, request: MedicalSummaryRequest,
                          start: date, end: date) -> tuple[dict[str, Any], list[str]]:
        calls = [
            self.java_client.get_profile(request.user_id, request.pet_id, request.trace_id),
            self.java_client.get_daily_status(request.user_id, request.pet_id, start, end,
                                               request.trace_id),
            self.java_client.get_medical_visits(request.user_id, request.pet_id, start, end,
                                                request.trace_id),
            self.java_client.get_medication_plans(request.user_id, request.pet_id, start, end,
                                                  request.trace_id),
            self.java_client.get_health_alerts(request.user_id, request.pet_id, request.trace_id),
        ]
        results = await asyncio.gather(*calls, return_exceptions=True)
        names = ("profile", "daily", "visits", "medications", "alerts")
        facts: dict[str, Any] = {}
        warnings: list[str] = []
        for name, result in zip(names, results):
            if isinstance(result, Exception):
                logger.warning("医疗摘要事实查询失败: section={}, error={}", name, result)
                warnings.append(f"{name} 数据暂时无法读取")
            else:
                facts[name] = result
        if not facts:
            raise JavaToolError("医疗摘要没有可用事实")
        return facts, warnings

    def _build_summary(self, request: MedicalSummaryRequest, facts: dict[str, Any],
                       warnings: list[str], start: date, end: date) -> dict[str, Any]:
        daily = facts.get("daily", {})
        visits = _records(facts.get("visits", {}))
        medications = facts.get("medications", {})
        alerts = _records(facts.get("alerts", {}))
        symptom_timeline = _records(daily)
        attention = [item.get("message", "") for item in alerts if item.get("message")]
        if not symptom_timeline:
            warnings.append("所选时间范围没有每日状态记录")
        if not visits:
            warnings.append("所选时间范围没有就医档案")
        return {
            "pet": facts.get("profile", {}),
            "range": {"startDate": start.isoformat(), "endDate": end.isoformat()},
            "overview": "",
            "symptomTimeline": symptom_timeline,
            "medicalVisits": _trim_media(visits, request.include_media),
            "medications": _medication_facts(medications),
            "attentionItems": attention,
            "warnings": warnings,
            "plainText": "",
            "disclaimer": DISCLAIMER,
        }

    async def _generate_overview(self, request: MedicalSummaryRequest,
                                 summary: dict[str, Any], warnings: list[str]) -> str:
        settings = get_settings()
        if not settings.dashscope_api_key:
            return _fallback_overview(summary)
        prompt = (
            "请用简体中文概括以下宠物医疗事实，最多三句话。只能复述记录到的事实，"
            "不能诊断、不能给出药方或剂量建议；如果资料不足，请明确说资料不足。\n"
            f"宠物：{summary['pet']}\n每日状态：{summary['symptomTimeline']}\n"
            f"就医：{summary['medicalVisits']}\n用药：{summary['medications']}\n"
            f"预警：{summary['attentionItems']}"
        )
        try:
            model = get_chat_model(Purpose.CHAT_DEEP)
            response = await asyncio.wait_for(
                model.ainvoke([SystemMessage(content=DISCLAIMER), HumanMessage(content=prompt)]),
                timeout=settings.agent_timeout_seconds,
            )
            text = _content_text(response.content).strip()
            if text and not any(word in text for word in ("诊断为", "确诊", "处方")):
                return text
        except Exception:
            logger.warning("医疗摘要 AI 润色失败，使用事实摘要", exc_info=True)
            warnings.append("AI 润色暂不可用，已返回事实摘要")
        return _fallback_overview(summary)

    def _plain_text(self, summary: dict[str, Any]) -> str:
        lines = [summary["overview"]]
        for item in summary["attentionItems"]:
            lines.append(f"需要关注：{item}")
        if summary["warnings"]:
            lines.append("；".join(summary["warnings"]))
        return "\n".join(line for line in lines if line)


def _records(data: Any) -> list[dict[str, Any]]:
    if not isinstance(data, dict):
        return []
    records = data.get("records", [])
    return records if isinstance(records, list) else []


def _trim_media(visits: list[dict[str, Any]], include_media: bool) -> list[dict[str, Any]]:
    if include_media:
        return visits
    return [{key: value for key, value in item.items() if key != "attachments"}
            for item in visits]


def _medication_facts(data: dict[str, Any]) -> list[dict[str, Any]]:
    plans = _records(data)
    executions = data.get("executions", []) if isinstance(data, dict) else []
    return [{"plans": plans, "executions": executions}]


def _fallback_overview(summary: dict[str, Any]) -> str:
    return (f"本时间范围内记录了 {len(summary['medicalVisits'])} 次就医档案、"
            f"{len(summary['symptomTimeline'])} 条每日状态记录。"
            f"当前有 {len(summary['attentionItems'])} 条健康预警需要关注。")


def _content_text(content: Any) -> str:
    if isinstance(content, list):
        return "".join(str(item.get("text", item)) if isinstance(item, dict) else str(item)
                       for item in content)
    return str(content)
