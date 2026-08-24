"""食品包装视觉识别服务。"""

import base64
import asyncio
import json
from typing import Any

import httpx
from langchain_core.messages import HumanMessage
from loguru import logger

from app.core.config import get_settings
from app.core.llm import Purpose, get_chat_model
from app.schemas.food import FoodRecognitionRequest


ALLOWED_FIELDS = {
    "name",
    "brand",
    "foodType",
    "specification",
    "packageWeight",
    "packageWeightUnit",
    "nutritionBasis",
    "proteinPercent",
    "fatPercent",
    "fiberPercent",
    "moisturePercent",
    "ashPercent",
    "calciumPercent",
    "phosphorusPercent",
    "energyKcal",
    "energyUnit",
    "proteinSources",
    "ingredients",
    "additiveFlags",
    "lifeStageLabel",
}


class FoodRecognitionError(RuntimeError):
    """食品识别结果无法安全解析。"""


class FoodRecognitionService:
    """调用视觉模型并返回受限食品字段，不访问 Java 数据库。"""

    async def recognize(self, request: FoodRecognitionRequest) -> dict[str, Any]:
        settings = get_settings()
        image_source = await self._prepare_image(request.image_url, settings.food_recognition_max_image_bytes,
                                                 settings.food_recognition_timeout_seconds)
        model = get_chat_model(Purpose.VISION)
        response = await asyncio.wait_for(
            model.ainvoke([self._build_message(image_source, request.prompt_version)]),
            timeout=settings.food_recognition_timeout_seconds,
        )
        raw_text = self._response_text(response)
        draft, warnings = self._parse_and_sanitize(raw_text)
        if not draft.get("name") and not draft.get("brand") and not draft.get("ingredients"):
            raise FoodRecognitionError("包装图片没有解析出可确认字段")
        usage = getattr(response, "response_metadata", {}).get("token_usage", {})
        logger.info("食品包装识别完成: task_id={}, media_id={}, field_count={}",
                    request.task_id, request.media_id, len(draft))
        return {
            "task_id": request.task_id,
            "status": "completed",
            "model": settings.model_vision,
            "prompt_version": request.prompt_version,
            "draft": draft,
            "warnings": warnings,
            "usage": usage,
            "trace_id": request.trace_id,
        }

    @staticmethod
    def _build_message(image_source: str, prompt_version: str) -> HumanMessage:
        prompt = (
            f"你是食品包装信息读取器，提示词版本：{prompt_version}。"
            "请只读取图片中清晰可见的食品名称、品牌、类型、规格、保证分析、能量、配料和阶段标注。"
            "看不清或没有出现的字段必须返回 null，禁止根据常识补全；不输出医疗诊断、治疗建议或食品绝对安全结论。"
            "只返回 JSON，字段使用 camelCase。JSON 顶层允许字段："
            "name, brand, foodType, specification, packageWeight, packageWeightUnit, nutritionBasis, "
            "proteinPercent, fatPercent, fiberPercent, moisturePercent, ashPercent, calciumPercent, "
            "phosphorusPercent, energyKcal, energyUnit, proteinSources, ingredients, additiveFlags, lifeStageLabel, "
            "confidence, warnings。"
        )
        return HumanMessage(content=[
            {"type": "text", "text": prompt},
            {"type": "image_url", "image_url": {"url": image_source}},
        ])

    async def _prepare_image(self, url: str, max_bytes: int, timeout_seconds: float) -> str:
        if url.startswith("data:image/"):
            return url
        try:
            async with httpx.AsyncClient(timeout=timeout_seconds) as client:
                response = await client.get(url)
                response.raise_for_status()
        except httpx.HTTPError as exc:
            raise FoodRecognitionError("包装图片下载失败") from exc
        if not response.content or len(response.content) > max_bytes:
            raise FoodRecognitionError("包装图片为空或超过处理大小")
        content_type = response.headers.get("content-type", "image/jpeg").split(";", 1)[0].lower()
        if not content_type.startswith("image/"):
            raise FoodRecognitionError("食品识别只支持图片")
        encoded = base64.b64encode(response.content).decode("ascii")
        return f"data:{content_type};base64,{encoded}"

    def _parse_and_sanitize(self, raw_text: str) -> tuple[dict[str, Any], list[str]]:
        value = self._parse_json(raw_text)
        draft: dict[str, Any] = {}
        warnings = self._string_list(value.get("warnings"))
        confidence = value.get("confidence") if isinstance(value.get("confidence"), dict) else {}
        for field in ALLOWED_FIELDS:
            raw_value = value.get(field)
            if isinstance(raw_value, dict) and "value" in raw_value:
                raw_value = raw_value.get("value")
            cleaned = self._clean_field(field, raw_value)
            if cleaned is not None:
                draft[field] = cleaned
        draft["confidence"] = self._clean_confidence(confidence)
        if value.get("warnings") and not warnings:
            warnings.append("模型返回了无法解析的提示信息")
        return draft, warnings

    @staticmethod
    def _clean_field(field: str, value: Any) -> Any:
        if value is None:
            return None
        if field in {"proteinSources", "ingredients", "additiveFlags"}:
            return [str(item).strip() for item in value if str(item).strip()] if isinstance(value, list) else None
        if field in {"packageWeight", "proteinPercent", "fatPercent", "fiberPercent", "moisturePercent",
                     "ashPercent", "calciumPercent", "phosphorusPercent", "energyKcal"}:
            try:
                return float(value)
            except (TypeError, ValueError):
                return None
        return str(value).strip() or None

    @staticmethod
    def _clean_confidence(confidence: dict[str, Any]) -> dict[str, float]:
        result: dict[str, float] = {}
        for key, value in confidence.items():
            if key not in ALLOWED_FIELDS:
                continue
            try:
                number = float(value)
            except (TypeError, ValueError):
                continue
            if 0 <= number <= 1:
                result[key] = number
        return result

    @staticmethod
    def _string_list(value: Any) -> list[str]:
        return [str(item).strip() for item in value if str(item).strip()] if isinstance(value, list) else []

    @staticmethod
    def _parse_json(raw_text: str) -> dict[str, Any]:
        text = raw_text.strip()
        if text.startswith("```"):
            text = text.strip("`").removeprefix("json").strip()
        try:
            value = json.loads(text)
        except json.JSONDecodeError as exc:
            start, end = text.find("{"), text.rfind("}")
            if start < 0 or end <= start:
                raise FoodRecognitionError("模型未返回有效 JSON") from exc
            try:
                value = json.loads(text[start:end + 1])
            except json.JSONDecodeError as nested_exc:
                raise FoodRecognitionError("模型未返回有效 JSON") from nested_exc
        if not isinstance(value, dict):
            raise FoodRecognitionError("模型返回不是 JSON 对象")
        return value

    @staticmethod
    def _response_text(response: Any) -> str:
        content = getattr(response, "content", response)
        if isinstance(content, str):
            return content
        if isinstance(content, list):
            return "".join(str(item.get("text", item)) if isinstance(item, dict) else str(item)
                           for item in content)
        return str(content)


food_recognition_service = FoodRecognitionService()
