"""基于 Java 事实快照生成可编辑内容草稿。"""

import asyncio
import json
from typing import Any

from langchain_core.messages import HumanMessage, SystemMessage
from loguru import logger

from app.core.config import get_settings
from app.core.llm import Purpose, get_chat_model
from app.schemas.content import ContentDraftContent, ContentGenerationRequest


class ContentGenerationService:
    async def generate(self, request: ContentGenerationRequest) -> dict[str, Any]:
        facts = self._parse(request.fact_snapshot_json)
        settings = get_settings()
        if not settings.dashscope_api_key:
            return self._response(self._fallback(facts, request), "deterministic", settings.model_chat_deep, request)
        try:
            value = await self._ask_model(facts, request)
            return self._response(value, "ai", settings.model_chat_deep, request)
        except Exception:
            logger.warning("内容 AI 生成失败，返回确定性草稿", exc_info=True)
            return self._response(self._fallback(facts, request), "deterministic", settings.model_chat_deep, request)

    async def _ask_model(self, facts: dict[str, Any], request: ContentGenerationRequest) -> ContentDraftContent:
        settings = get_settings()
        model = get_chat_model(Purpose.CHAT_DEEP)
        prompt = (
            "你是宠物内容编辑。只能使用事实快照中的信息，不能编造日期、事件、健康结论。"
            "只返回 JSON，字段为 title, body, hashtags, mediaOrder, sourceKeys。"
            f"内容类型：{request.content_type}，主题：{request.topic_type} {request.custom_topic or ''}。"
            f"可用素材顺序：{request.selected_media_json}\n事实快照：{request.fact_snapshot_json}"
        )
        response = await asyncio.wait_for(
            model.ainvoke([SystemMessage(content="内容必须可编辑，不得包含诊断或治疗建议。"), HumanMessage(content=prompt)]),
            timeout=settings.agent_timeout_seconds,
        )
        return ContentDraftContent.model_validate(_parse_json(_content_text(response.content)))

    def _fallback(self, facts: dict[str, Any], request: ContentGenerationRequest) -> ContentDraftContent:
        pet = facts.get("pet", {})
        name = pet.get("name", "宠物") if isinstance(pet, dict) else "宠物"
        media = _parse_media(request.selected_media_json)
        return ContentDraftContent(title=f"{name}的成长记录",
                                   body=f"记录一下{name}最近的生活点滴，留住每一个值得回看的瞬间。",
                                   hashtags=["#宠物日常", "#成长记录"], mediaOrder=media, sourceKeys=[])

    @staticmethod
    def _parse(value: str) -> dict[str, Any]:
        parsed = json.loads(value)
        if not isinstance(parsed, dict):
            raise ValueError("事实快照必须是 JSON 对象")
        return parsed

    @staticmethod
    def _response(content: ContentDraftContent, mode: str, model: str,
                  request: ContentGenerationRequest) -> dict[str, Any]:
        return {"content": content.model_dump(by_alias=True), "mode": mode,
                "modelName": model, "promptVersion": request.prompt_version}


def _parse_media(value: str) -> list[int]:
    try:
        parsed = json.loads(value or "[]")
        return [int(item) for item in parsed if isinstance(item, int)] if isinstance(parsed, list) else []
    except (TypeError, ValueError, json.JSONDecodeError):
        return []


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


content_generation_service = ContentGenerationService()
