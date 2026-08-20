"""
回调接口 - 接收 Java 服务的消息通知
"""
from fastapi import APIRouter, BackgroundTasks
from pydantic import BaseModel
from loguru import logger

from app.service.photo_service import PhotoAnalysisService

router = APIRouter()
photo_service = PhotoAnalysisService()


class PhotoAnalysisMessage(BaseModel):
    """照片分析消息"""
    photoId: int
    petId: int
    userId: int
    url: str
    thumbnailUrl: str = None
    takenAt: str = None


@router.post("/photo-analysis")
async def photo_analysis_callback(
    message: PhotoAnalysisMessage,
    background_tasks: BackgroundTasks
):
    """
    照片分析回调接口
    接收 Java 服务发送的照片分析请求
    """
    logger.info(f"收到照片分析请求: photoId={message.photoId}, petId={message.petId}")
    
    # 将照片分析任务放到后台执行
    background_tasks.add_task(
        process_photo_analysis,
        message.photoId,
        message.petId,
        message.url
    )
    
    return {
        "code": 200,
        "message": "照片分析任务已接收",
        "data": None
    }


async def process_photo_analysis(photo_id: int, pet_id: int, url: str):
    """处理照片分析"""
    try:
        logger.info(f"开始处理照片分析: photoId={photo_id}")
        
        await photo_service.analyze_photo(
            photo_id=photo_id,
            pet_id=pet_id,
            url=url
        )
        
        logger.info(f"照片分析完成: photoId={photo_id}")
        
    except Exception as e:
        logger.error(f"照片分析失败: photoId={photo_id}, error={e}", exc_info=True)
