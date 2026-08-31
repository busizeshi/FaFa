"""照片/素材 embedding 服务：向量化、个体匹配与 Java 回调。"""

from datetime import datetime

from loguru import logger
from qdrant_client.models import FieldCondition, Filter, MatchAny, MatchValue, PointStruct

from app.core.config import get_settings
from app.core.dashscope_client import (
    VideoEmbeddingInputError,
    get_multimodal_embedding,
    get_video_multimodal_embedding,
)
from app.core.qdrant import COLLECTION_MEDIA, COLLECTION_PET_PROFILES, get_client, point_id
from app.service.callback_client import send_photo_analysis_callback


class PermanentMediaError(RuntimeError):
    """素材下载或格式确定性错误，需回调 Java 将素材置为 failed。"""


class PhotoService:
    """照片 embedding 编排服务。"""

    async def analyze(
        self,
        user_id: int,
        photo_id: int,
        url: str,
        file_url: str | None,
        media_type: str,
        tags: list[str] | None,
        pet_id: int | None,
        candidate_pet_ids: list[int] | None,
        message_id: str | None,
        taken_at: datetime | None = None,
        uploaded_at: datetime | None = None,
        trace_id: str | None = None,
    ) -> None:
        """执行 embedding、宠物匹配与回调；重复执行通过 point_id 幂等。"""
        if media_type not in {"photo", "video"}:
            raise PermanentMediaError(f"不支持的媒体类型: {media_type}")

        logger.info("媒体分析开始: media_id={}, media_type={}, user_id={}, message_id={}",
                    photo_id, media_type, user_id, message_id)
        try:
            if media_type == "photo":
                embedding = await get_multimodal_embedding(url)
            else:
                embedding = await get_video_multimodal_embedding(url)

            recognized_pets: list[tuple[int, float]] = []
            recognition_score = None
            if pet_id is None:
                recognized_pets = self._recognize_pets(user_id, embedding, candidate_pet_ids)
                if recognized_pets:
                    recognition_score = max(score for _, score in recognized_pets)

            self._upsert_media_vector(
                media_id=photo_id,
                user_id=user_id,
                media_type=media_type,
                tags=tags,
                pet_ids=[pet_id] if pet_id is not None else [matched_id for matched_id, _ in recognized_pets],
                file_url=file_url or url,
                taken_at=taken_at,
                uploaded_at=uploaded_at,
                embedding=embedding,
            )
            await send_photo_analysis_callback(
                media_id=photo_id,
                success=True,
                recognized_pet_ids=[matched_id for matched_id, _ in recognized_pets],
                recognition_score=recognition_score,
                trace_id=trace_id,
            )
            logger.info("媒体 embedding 完成: media_id={}, recognized_pet_ids={}, score={}",
                        photo_id, [matched_id for matched_id, _ in recognized_pets], recognition_score)
        except (PermanentMediaError, VideoEmbeddingInputError) as exc:
            logger.warning("媒体素材不可处理: media_id={}, error={}", photo_id, str(exc))
            await send_photo_analysis_callback(
                media_id=photo_id,
                success=False,
                error_message=str(exc)[:500],
                trace_id=trace_id,
            )
        except Exception:
            # embedding、Qdrant 等暂时性失败交给 MQ 重试；HTTP 直推场景保留 pending 供人工处理。
            logger.exception("照片分析失败: media_id={}", photo_id)
            raise

    def _recognize_pets(self, user_id: int, embedding: list[float],
                        candidate_pet_ids: list[int] | None) -> list[tuple[int, float]]:
        """按每只宠物三视图中的最高相似度筛选，返回所有达到阈值的宠物。"""
        if candidate_pet_ids is not None and not candidate_pet_ids:
            return []

        settings = get_settings()
        conditions = [FieldCondition(key="user_id", match=MatchValue(value=user_id))]
        if candidate_pet_ids is not None:
            conditions.append(FieldCondition(
                key="pet_id", match=MatchAny(any=list(dict.fromkeys(candidate_pet_ids)))
            ))
        # 每只宠物最多三张 profile view；乘以三可以保证取到全部视图，再按宠物聚合。
        limit = max(len(candidate_pet_ids or []) * 3, 1) if candidate_pet_ids is not None else 20
        query_response = get_client().query_points(
            collection_name=COLLECTION_PET_PROFILES,
            query=embedding,
            query_filter=Filter(must=conditions),
            limit=limit,
            with_payload=True,
        )
        best_by_pet: dict[int, float] = {}
        for point in query_response.points:
            candidate_pet_id = (point.payload or {}).get("pet_id")
            if candidate_pet_id is None:
                continue
            candidate_pet_id = int(candidate_pet_id)
            best_by_pet[candidate_pet_id] = max(
                best_by_pet.get(candidate_pet_id, 0.0), float(point.score)
            )
        return sorted(
            ((pet_id, score) for pet_id, score in best_by_pet.items()
             if score >= settings.recognition_threshold),
            key=lambda item: item[1],
            reverse=True,
        )

    @staticmethod
    def _upsert_media_vector(
        *,
        media_id: int,
        user_id: int,
        media_type: str,
        tags: list[str] | None,
        pet_ids: list[int],
        file_url: str,
        taken_at: datetime | None,
        uploaded_at: datetime | None,
        embedding: list[float],
    ) -> None:
        taken_ts = int(taken_at.timestamp() * 1000) if taken_at else None
        uploaded_ts = int(uploaded_at.timestamp() * 1000) if uploaded_at else None
        normalized_pet_ids = list(dict.fromkeys(pet_ids))
        payload = {
            "media_id": media_id,
            "user_id": user_id,
            # pet_ids 支持一份媒体同时命中多个宠物；pet_id 仅保留单宠物兼容字段。
            "pet_id": normalized_pet_ids[0] if len(normalized_pet_ids) == 1 else None,
            "pet_ids": normalized_pet_ids,
            "media_type": media_type,
            "file_url": file_url,
            "tags": tags or [],
            "taken_ts": taken_ts,
            "uploaded_ts": uploaded_ts,
        }
        get_client().upsert(
            collection_name=COLLECTION_MEDIA,
            points=[PointStruct(id=point_id(f"media_{media_id}"), vector=embedding, payload=payload)],
        )


photo_service = PhotoService()
