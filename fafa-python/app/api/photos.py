"""照片/媒体路由。"""

import asyncio
from datetime import date, datetime, time

from fastapi import APIRouter, BackgroundTasks, Request, status
from loguru import logger
from qdrant_client.models import FieldCondition, Filter, MatchValue, Range

from app.core.config import get_settings
from app.core.llm import Purpose, get_embedding_model
from app.core.qdrant import COLLECTION_MEDIA, get_client
from app.schemas.photo import PhotoAnalyzeRequest, PhotoSearchRequest
from app.service.photo_service import photo_service

router = APIRouter(prefix="/api/photos", tags=["photos"])


@router.post("/analyze", status_code=status.HTTP_202_ACCEPTED)
async def analyze_photo(
    request: PhotoAnalyzeRequest,
    background_tasks: BackgroundTasks,
    http_request: Request,
) -> dict:
    """受理素材分析任务，视觉理解与向量化在后台执行。"""
    logger.info(
        "收到照片分析任务: media_id={}, media_type={}, message_id={}",
        request.photo_id, request.media_type, request.message_id,
    )
    background_tasks.add_task(
        photo_service.analyze,
        request.user_id,
        request.photo_id,
        request.url,
        request.media_type,
        request.tags,
        request.pet_id,
        request.message_id,
        request.taken_at,
        http_request.headers.get("x-trace-id"),
    )
    return {"accepted": True, "media_id": request.photo_id, "message_id": request.message_id}


@router.post("/search")
async def search_photos(request: PhotoSearchRequest) -> dict:
    """文本向量检索素材，并叠加用户、宠物和拍摄时间过滤。"""
    logger.info("收到语义检索请求: user_id={}, query={}", request.user_id, request.query)
    embedder = get_embedding_model(Purpose.EMBED_TEXT)
    embedding = await asyncio.to_thread(embedder.embed_query, request.query)

    conditions = [FieldCondition(key="user_id", match=MatchValue(value=request.user_id))]
    if request.pet_id is not None:
        conditions.append(FieldCondition(key="pet_id", match=MatchValue(value=request.pet_id)))
    time_range = _build_time_range(request.start_date, request.end_date)
    if time_range is not None:
        conditions.append(FieldCondition(key="taken_ts", range=time_range))

    points = await asyncio.to_thread(
        get_client().search,
        collection_name=COLLECTION_MEDIA,
        query_vector=embedding,
        query_filter=Filter(must=conditions),
        limit=request.limit,
        score_threshold=get_settings().search_score_threshold,
        with_payload=True,
    )
    results = []
    for point in points:
        media_id = (point.payload or {}).get("media_id")
        if media_id is not None:
            results.append({"media_id": int(media_id), "score": float(point.score)})
    return {"results": results}


def _build_time_range(start_date: date | None, end_date: date | None) -> Range | None:
    """将 yyyy-MM-dd 日期范围转换成素材 payload 的毫秒时间戳范围。"""
    if not start_date and not end_date:
        return None
    return Range(
        gte=_timestamp(start_date, time.min) if start_date else None,
        lte=_timestamp(end_date, time.max) if end_date else None,
    )


def _timestamp(day: date, at: time) -> int:
    return int(datetime.combine(day, at).timestamp() * 1000)
