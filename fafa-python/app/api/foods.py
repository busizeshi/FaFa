"""食品领域 AI 内部接口。"""

from fastapi import APIRouter
from loguru import logger

from app.schemas.food import FoodRecognitionRequest, FoodRecognitionResponse
from app.service.food_recognition_service import food_recognition_service


router = APIRouter(prefix="/api/foods", tags=["foods"])


@router.post("/recognize", response_model=FoodRecognitionResponse)
async def recognize_food(request: FoodRecognitionRequest) -> FoodRecognitionResponse:
    """识别包装图片并返回待 Java 确认的候选字段。"""
    logger.info("收到食品包装识别请求: task_id={}, media_id={}", request.task_id, request.media_id)
    return FoodRecognitionResponse.model_validate(await food_recognition_service.recognize(request))
