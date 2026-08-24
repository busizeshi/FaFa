"""照片/素材分析服务：视觉理解、向量化、个体识别与 Java 回调。"""

import base64
import json
from datetime import datetime
from typing import Any
from urllib.parse import urlparse

import httpx
from langchain_core.messages import HumanMessage
from loguru import logger
from qdrant_client.models import FieldCondition, Filter, MatchValue, PointStruct

from app.core.config import get_settings
from app.core.dashscope_client import (
    VideoEmbeddingInputError,
    get_multimodal_embedding,
    get_video_multimodal_embedding,
    is_private_url,
)
from app.core.llm import Purpose, get_chat_model
from app.core.qdrant import COLLECTION_MEDIA, COLLECTION_PET_PROFILES, get_client, point_id
from app.service.callback_client import send_photo_analysis_callback


class PermanentMediaError(RuntimeError):
    """素材下载或格式确定性错误，需回调 Java 将素材置为 failed。"""


class PhotoService:
    """照片分析编排服务。"""

    async def analyze(
        self,
        user_id: int,
        photo_id: int,
        url: str,
        media_type: str,
        tags: list[str] | None,
        pet_id: int | None,
        message_id: str | None,
        taken_at: datetime | None = None,
        trace_id: str | None = None,
    ) -> None:
        """执行媒体理解、向量化、个体识别与回调；重复执行通过 point_id 幂等。"""
        if media_type not in {"photo", "video"}:
            raise PermanentMediaError(f"不支持的媒体类型: {media_type}")

        logger.info("媒体分析开始: media_id={}, media_type={}, user_id={}, message_id={}",
                    photo_id, media_type, user_id, message_id)
        try:
            if media_type == "photo":
                image_data_uri = await self._download_image_as_data_uri(url)
                description, ai_tags = await self._understand_image(image_data_uri)
                embedding = await get_multimodal_embedding(image_data_uri)
            else:
                vision_source = await self._prepare_video_for_vision(url)
                description, ai_tags = await self._understand_video(vision_source)
                embedding = await get_video_multimodal_embedding(url)

            recognized_pet_id = None
            recognition_score = None
            if pet_id is None:
                recognized_pet_id, recognition_score = self._recognize_pet(user_id, embedding)

            self._upsert_media_vector(
                media_id=photo_id,
                user_id=user_id,
                media_type=media_type,
                tags=tags,
                pet_id=pet_id or recognized_pet_id,
                taken_at=taken_at,
                embedding=embedding,
            )
            await send_photo_analysis_callback(
                media_id=photo_id,
                success=True,
                description=description,
                ai_tags=ai_tags,
                recognized_pet_id=recognized_pet_id,
                recognition_score=recognition_score,
                trace_id=trace_id,
            )
            logger.info("照片分析完成: media_id={}, recognized_pet_id={}, score={}",
                        photo_id, recognized_pet_id, recognition_score)
        except (PermanentMediaError, VideoEmbeddingInputError) as exc:
            logger.warning("媒体素材不可处理: media_id={}, error={}", photo_id, str(exc))
            await send_photo_analysis_callback(
                media_id=photo_id,
                success=False,
                error_message=str(exc)[:500],
                trace_id=trace_id,
            )
        except Exception:
            # 模型、Qdrant 等暂时性失败交给 MQ 重试；HTTP 直推场景保留 pending 供人工处理。
            logger.exception("照片分析失败: media_id={}", photo_id)
            raise

    async def _prepare_video_for_vision(self, url: str) -> str:
        """为视觉模型准备视频；内网视频转受控 Base64，公网视频保留 URL。"""
        if url.startswith("data:video/") or not is_private_url(url):
            return url
        try:
            async with httpx.AsyncClient(timeout=60) as client:
                response = await client.get(url)
                response.raise_for_status()
        except httpx.HTTPStatusError as exc:
            raise PermanentMediaError(f"视频下载失败: HTTP {exc.response.status_code}") from exc
        except httpx.HTTPError as exc:
            raise PermanentMediaError(f"视频下载失败: {exc}") from exc

        settings = get_settings()
        if len(response.content) > settings.video_vision_inline_max_bytes:
            raise PermanentMediaError(
                "内网视频超过视觉模型 Base64 大小限制，请配置百炼可访问的公网视频 URL"
            )
        content_type = response.headers.get("content-type", "video/mp4").split(";", 1)[0].lower()
        if not content_type.startswith("video/"):
            suffix = urlparse(url).path.lower()
            if not suffix.endswith((".mp4", ".avi", ".mov")):
                raise PermanentMediaError(f"素材不是有效视频: {content_type}")
            content_type = "video/mp4"
        encoded = base64.b64encode(response.content).decode("ascii")
        return f"data:{content_type};base64,{encoded}"

    async def _download_image_as_data_uri(self, url: str) -> str:
        """下载图片并转为 Data URI，避免百炼访问不到开发环境内网对象存储。"""
        if url.startswith("data:image/"):
            return url
        try:
            async with httpx.AsyncClient(timeout=60) as client:
                response = await client.get(url)
                response.raise_for_status()
        except httpx.HTTPStatusError as exc:
            raise PermanentMediaError(f"素材下载失败: HTTP {exc.response.status_code}") from exc
        except httpx.HTTPError as exc:
            raise PermanentMediaError(f"素材下载失败: {exc}") from exc

        content_type = response.headers.get("content-type", "image/jpeg").split(";", 1)[0].lower()
        if not content_type.startswith("image/"):
            raise PermanentMediaError(f"素材不是有效图片: {content_type}")
        if not response.content:
            raise PermanentMediaError("素材内容为空")
        if len(response.content) > 20 * 1024 * 1024:
            raise PermanentMediaError("素材超过 AI 服务可处理大小")
        encoded = base64.b64encode(response.content).decode("ascii")
        return f"data:{content_type};base64,{encoded}"

    async def _understand_image(self, image_data_uri: str) -> tuple[str, list[str]]:
        """调用视觉模型生成稳定 JSON，并对模型输出做宽容解析。"""
        model = get_chat_model(Purpose.VISION)
        message = HumanMessage(content=[
            {
                "type": "text",
                "text": (
                    "请分析这张宠物照片，只返回 JSON，不要 Markdown。格式为 "
                    '{"description":"不超过200字的客观描述","tags":["不超过10个中文短标签"]}。'
                    "标签应描述可观察到的宠物、动作、地点或物品，不要猜测健康诊断。"
                ),
            },
            {"type": "image_url", "image_url": {"url": image_data_uri}},
        ])
        response = await model.ainvoke([message])
        raw = self._response_text(response)
        parsed = self._parse_json(raw)
        description = str(parsed.get("description") or raw).strip()[:1000]
        raw_tags = parsed.get("tags")
        ai_tags = [str(tag).strip() for tag in raw_tags if str(tag).strip()] if isinstance(raw_tags, list) else []
        return description, list(dict.fromkeys(ai_tags))[:10]

    async def _understand_video(self, video_source: str) -> tuple[str, list[str]]:
        """调用视觉模型按抽帧理解视频，输出与照片一致的描述和标签契约。"""
        settings = get_settings()
        model = get_chat_model(Purpose.VISION)
        message = HumanMessage(content=[
            {
                "type": "text",
                "text": (
                    "请分析这个宠物视频，只返回 JSON，不要 Markdown。格式为 "
                    '{"description":"不超过200字的客观描述","tags":["不超过10个中文短标签"]}。'
                    "标签应描述视频中可观察到的宠物、动作、地点或物品，不要猜测健康诊断。"
                ),
            },
            {
                "type": "video_url",
                "video_url": {"url": video_source, "fps": settings.video_vision_fps},
            },
        ])
        response = await model.ainvoke([message])
        parsed = self._parse_json(self._response_text(response))
        description = str(parsed.get("description") or "").strip()[:1000]
        raw_tags = parsed.get("tags")
        ai_tags = [str(tag).strip() for tag in raw_tags if str(tag).strip()] if isinstance(raw_tags, list) else []
        return description, list(dict.fromkeys(ai_tags))[:10]

    @staticmethod
    def _response_text(response: Any) -> str:
        content = getattr(response, "content", response)
        if isinstance(content, str):
            return content
        if isinstance(content, list):
            parts = []
            for item in content:
                if isinstance(item, dict) and item.get("text"):
                    parts.append(str(item["text"]))
                elif isinstance(item, str):
                    parts.append(item)
            return "".join(parts)
        return str(content)

    @staticmethod
    def _parse_json(raw: str) -> dict[str, Any]:
        text = raw.strip()
        if text.startswith("```"):
            text = text.strip("`").removeprefix("json").strip()
        try:
            value = json.loads(text)
            return value if isinstance(value, dict) else {"description": text}
        except json.JSONDecodeError:
            start, end = text.find("{"), text.rfind("}")
            if start >= 0 and end > start:
                try:
                    value = json.loads(text[start:end + 1])
                    return value if isinstance(value, dict) else {"description": text}
                except json.JSONDecodeError:
                    pass
            return {"description": text}

    def _recognize_pet(self, user_id: int, embedding: list[float]) -> tuple[int | None, float | None]:
        """在当前用户的三视图向量中取最高相似度，达到配置阈值才返回归属。"""
        settings = get_settings()
        query_filter = Filter(must=[
            FieldCondition(key="user_id", match=MatchValue(value=user_id)),
        ])
        query_response = get_client().query_points(
            collection_name=COLLECTION_PET_PROFILES,
            query=embedding,
            query_filter=query_filter,
            limit=20,
            with_payload=True,
        )
        points = query_response.points
        best_by_pet: dict[int, float] = {}
        for point in points:
            payload = point.payload or {}
            candidate_pet_id = payload.get("pet_id")
            if candidate_pet_id is None:
                continue
            score = float(point.score)
            candidate_pet_id = int(candidate_pet_id)
            best_by_pet[candidate_pet_id] = max(best_by_pet.get(candidate_pet_id, 0.0), score)
        if not best_by_pet:
            return None, None
        best_pet_id, best_score = max(best_by_pet.items(), key=lambda item: item[1])
        if best_score < settings.recognition_threshold:
            return None, best_score
        return best_pet_id, best_score

    @staticmethod
    def _upsert_media_vector(
        *,
        media_id: int,
        user_id: int,
        media_type: str,
        tags: list[str] | None,
        pet_id: int | None,
        taken_at: datetime | None,
        embedding: list[float],
    ) -> None:
        taken_ts = int(taken_at.timestamp() * 1000) if taken_at else None
        payload = {
            "media_id": media_id,
            "user_id": user_id,
            "pet_id": pet_id,
            "media_type": media_type,
            "tags": tags or [],
            "taken_ts": taken_ts,
        }
        get_client().upsert(
            collection_name=COLLECTION_MEDIA,
            points=[PointStruct(id=point_id(f"media_{media_id}"), vector=embedding, payload=payload)],
        )


photo_service = PhotoService()
