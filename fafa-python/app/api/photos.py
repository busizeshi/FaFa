"""照片/媒体路由。"""

from fastapi import APIRouter, BackgroundTasks, Request, status
from loguru import logger
from app.schemas.photo import PhotoAnalyzeRequest, PhotoSearchRequest
from app.service.media_search_service import media_search_service
from app.service.photo_service import photo_service

router = APIRouter(prefix="/api/photos", tags=["photos"])


@router.post("/analyze", status_code=status.HTTP_202_ACCEPTED)
async def analyze_photo(
    request: PhotoAnalyzeRequest,
    background_tasks: BackgroundTasks,
    http_request: Request,
) -> dict:
    """受理素材 embedding 向量化任务，向量化与宠物匹配在后台执行。"""
    logger.info(
        "收到媒体分析任务: media_id={}, media_type={}, message_id={}",
        request.photo_id, request.media_type, request.message_id,
    )
    background_tasks.add_task(
        photo_service.analyze,
        user_id=request.user_id,
        photo_id=request.photo_id,
        url=request.url,
        file_url=request.file_url,
        media_type=request.media_type,
        tags=request.tags,
        pet_id=request.pet_id,
        candidate_pet_ids=request.candidate_pet_ids,
        message_id=request.message_id,
        taken_at=request.taken_at,
        uploaded_at=request.uploaded_at,
        trace_id=http_request.headers.get("x-trace-id"),
    )
    return {"accepted": True, "media_id": request.photo_id, "message_id": request.message_id}


@router.post("/search")
async def search_photos(request: PhotoSearchRequest) -> dict:
    """用时间/宠物精确召回与语义向量召回并行检索素材。"""
    logger.info("收到语义检索请求: user_id={}, query={}", request.user_id, request.query)
    return {"results": await media_search_service.search(request)}
