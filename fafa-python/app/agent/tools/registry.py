"""Agent 工具静态注册表。"""

from datetime import date
from typing import Any

from app.agent.tools.java_client import JavaQueryClient
from app.core.config import get_settings


class ToolRegistry:
    """按意图调用固定的只读工具，拒绝模型动态生成的工具名。"""

    def __init__(self, client: JavaQueryClient | None = None) -> None:
        self.client = client or JavaQueryClient()

    async def execute(self, names: list[str], user_id: int, pet_id: int,
                      query: str, start: date, end: date, trace_id: str) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
        if len(names) > get_settings().max_tool_calls:
            raise ValueError("Agent 工具调用次数超过上限")
        evidence: list[dict[str, Any]] = []
        calls: list[dict[str, Any]] = []
        for name in names:
            result = await self._execute_one(name, user_id, pet_id, query, start, end, trace_id)
            evidence.append({"tool": name, "data": result})
            calls.append({"name": name, "success": True, "result_count": _result_count(result)})
        return evidence, calls

    async def _execute_one(self, name: str, user_id: int, pet_id: int,
                           query: str, start: date, end: date, trace_id: str) -> dict[str, Any]:
        if name == "getPetProfile":
            return await self.client.get_profile(user_id, pet_id, trace_id)
        if name == "getDailyStatusHistory":
            return await self.client.get_daily_status(user_id, pet_id, start, end, trace_id)
        if name == "getWeightHistory":
            return await self.client.get_weight(user_id, pet_id, start, end, trace_id)
        if name == "getFeedingHistory":
            return await self.client.get_feeding(user_id, pet_id, start, end, trace_id)
        if name == "getEventHistory":
            return await self.client.get_events(user_id, pet_id, start, end, trace_id)
        if name == "getReminderHistory":
            return await self.client.get_reminders(user_id, pet_id, start, end, trace_id)
        if name == "getFoodInfo":
            return await self.client.get_foods(user_id, pet_id, trace_id)
        if name == "getFeedingPlan":
            return await self.client.get_feeding_plans(user_id, pet_id, trace_id)
        if name == "getExpenseHistory":
            return await self.client.get_expenses(user_id, pet_id, start, end, trace_id)
        if name == "getExpenseStatistics":
            period = "year" if any(word in query for word in ("年", "年度", "全年")) else "month"
            return await self.client.get_expense_statistics(user_id, pet_id, period, end, trace_id)
        if name == "getPetReports":
            period_type = "month" if "月报" in query or "月度" in query else None
            if "季报" in query or "季度" in query:
                period_type = "quarter"
            if "年报" in query or "年度" in query:
                period_type = "year"
            return await self.client.get_reports(user_id, pet_id, period_type, 5, trace_id)
        if name == "searchPetPhotos":
            return await self.client.search_media(
                user_id, pet_id, query, start, end, get_settings().media_search_limit, trace_id
            )
        raise ValueError(f"未注册的 Agent 工具: {name}")


def _result_count(result: dict[str, Any]) -> int:
    records = result.get("records") or result.get("results") or []
    return len(records) if isinstance(records, list) else 0
