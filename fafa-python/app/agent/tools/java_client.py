"""Python Agent 调用 Java 内部只读接口的客户端。"""

from datetime import date
from typing import Any

import httpx
from loguru import logger

from app.core.config import get_settings


class JavaToolError(RuntimeError):
    """Java 内部工具调用失败。"""

    def __init__(self, message: str, status_code: int | None = None) -> None:
        super().__init__(message)
        self.status_code = status_code


class JavaQueryClient:
    """只封装本阶段 Agent 使用的 Java 查询端点。"""

    async def _request(self, method: str, path: str, **kwargs: Any) -> dict[str, Any]:
        settings = get_settings()
        headers = {
            "X-Internal-Token": settings.internal_token,
            "X-Trace-Id": kwargs.pop("trace_id", "no-trace"),
        }
        timeout = httpx.Timeout(settings.tool_timeout_seconds)
        try:
            async with httpx.AsyncClient(
                base_url=settings.java_base_url.rstrip("/"),
                headers=headers,
                timeout=timeout,
            ) as client:
                response = await client.request(method, path, **kwargs)
        except httpx.TimeoutException as exc:
            raise JavaToolError("Java 查询工具超时") from exc
        except httpx.RequestError as exc:
            raise JavaToolError("Java 查询工具网络不可用") from exc
        if response.status_code == 404:
            return {"records": [], "missing_data": True}
        if response.status_code in (401, 403):
            raise JavaToolError("Java 查询工具拒绝访问", response.status_code)
        if response.is_error:
            logger.error("Java 查询工具失败: path={}, status={}", path, response.status_code)
            raise JavaToolError("Java 查询工具返回错误", response.status_code)
        try:
            data = response.json()
        except ValueError as exc:
            raise JavaToolError("Java 查询工具响应不是 JSON", response.status_code) from exc
        if not isinstance(data, dict):
            raise JavaToolError("Java 查询工具响应格式无效", response.status_code)
        if isinstance(data.get("code"), int) and data["code"] != 0:
            raise JavaToolError(data.get("message", "Java 查询工具业务失败"), data["code"])
        return data

    async def get_profile(self, user_id: int, pet_id: int, trace_id: str) -> dict[str, Any]:
        return await self._request("GET", f"/internal/ai/pets/{pet_id}/profile",
                                   params={"userId": user_id}, trace_id=trace_id)

    async def get_daily_status(self, user_id: int, pet_id: int, start: date,
                               end: date, trace_id: str) -> dict[str, Any]:
        return await self._request("GET", f"/internal/ai/pets/{pet_id}/daily-status",
                                   params={"userId": user_id, "startDate": start.isoformat(),
                                           "endDate": end.isoformat()}, trace_id=trace_id)

    async def get_weight(self, user_id: int, pet_id: int, start: date,
                         end: date, trace_id: str) -> dict[str, Any]:
        return await self._request("GET", f"/internal/ai/pets/{pet_id}/weight",
                                   params={"userId": user_id, "startDate": start.isoformat(),
                                           "endDate": end.isoformat()}, trace_id=trace_id)

    async def get_feeding(self, user_id: int, pet_id: int, start: date,
                          end: date, trace_id: str) -> dict[str, Any]:
        return await self._request("GET", f"/internal/ai/pets/{pet_id}/feeding",
                                   params={"userId": user_id, "startDate": start.isoformat(),
                                           "endDate": end.isoformat()}, trace_id=trace_id)

    async def search_media(self, user_id: int, pet_id: int | None, query: str,
                           start: date, end: date, limit: int, trace_id: str) -> dict[str, Any]:
        return await self._request("POST", "/internal/ai/media/search", json={
            "user_id": user_id,
            "query": query,
            "pet_id": pet_id,
            "start_date": start.isoformat(),
            "end_date": end.isoformat(),
            "limit": limit,
        }, trace_id=trace_id)

    async def get_events(self, user_id: int, pet_id: int, start: date,
                         end: date, trace_id: str) -> dict[str, Any]:
        return await self._request("GET", f"/internal/ai/pets/{pet_id}/events",
                                   params={"userId": user_id, "startDate": start.isoformat(),
                                           "endDate": end.isoformat()}, trace_id=trace_id)

    async def get_reminders(self, user_id: int, pet_id: int, start: date,
                            end: date, trace_id: str) -> dict[str, Any]:
        return await self._request("GET", f"/internal/ai/pets/{pet_id}/reminders/history",
                                   params={"userId": user_id, "startDate": start.isoformat(),
                                           "endDate": end.isoformat()}, trace_id=trace_id)

    async def get_medical_visits(self, user_id: int, pet_id: int, start: date,
                                 end: date, trace_id: str) -> dict[str, Any]:
        return await self._request("GET", f"/internal/ai/pets/{pet_id}/medical-visits",
                                   params={"userId": user_id, "startDate": start.isoformat(),
                                           "endDate": end.isoformat()}, trace_id=trace_id)

    async def get_medication_plans(self, user_id: int, pet_id: int, start: date,
                                   end: date, trace_id: str) -> dict[str, Any]:
        return await self._request("GET", f"/internal/ai/pets/{pet_id}/medication-plans",
                                   params={"userId": user_id, "startDate": start.isoformat(),
                                           "endDate": end.isoformat()}, trace_id=trace_id)

    async def get_health_alerts(self, user_id: int, pet_id: int,
                                trace_id: str) -> dict[str, Any]:
        return await self._request("GET", f"/internal/ai/pets/{pet_id}/health-alerts",
                                   params={"userId": user_id}, trace_id=trace_id)

    async def get_foods(self, user_id: int, pet_id: int, trace_id: str,
                        status: str = "active") -> dict[str, Any]:
        return await self._request("GET", f"/internal/ai/pets/{pet_id}/foods",
                                   params={"userId": user_id, "status": status}, trace_id=trace_id)

    async def get_feeding_plans(self, user_id: int, pet_id: int, trace_id: str,
                                status: str = "active") -> dict[str, Any]:
        return await self._request("GET", f"/internal/ai/pets/{pet_id}/feeding-plans",
                                   params={"userId": user_id, "status": status}, trace_id=trace_id)

    async def get_expenses(self, user_id: int, pet_id: int, start: date,
                           end: date, trace_id: str) -> dict[str, Any]:
        return await self._request("GET", f"/internal/ai/pets/{pet_id}/expenses",
                                   params={"userId": user_id, "startDate": start.isoformat(),
                                           "endDate": end.isoformat()}, trace_id=trace_id)

    async def get_expense_statistics(self, user_id: int, pet_id: int, period: str,
                                     anchor_date: date, trace_id: str) -> dict[str, Any]:
        return await self._request("GET", f"/internal/ai/pets/{pet_id}/expense-statistics",
                                   params={"userId": user_id, "period": period,
                                           "anchorDate": anchor_date.isoformat()}, trace_id=trace_id)

    async def get_reports(self, user_id: int, pet_id: int, period_type: str | None,
                          limit: int, trace_id: str) -> dict[str, Any]:
        params: dict[str, Any] = {"userId": user_id, "limit": limit}
        if period_type:
            params["periodType"] = period_type
        return await self._request("GET", f"/internal/ai/pets/{pet_id}/reports",
                                   params=params, trace_id=trace_id)
