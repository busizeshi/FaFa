"""
回调接口 - 接收 Java 服务异步下发的媒体分析消息

说明:
1. 消息体与 RocketMQ photo-analysis topic 的消息保持一致 (与 Java 侧 PhotoAnalysisMessage 对齐，camelCase)
2. 开发环境 (Windows) 下 RocketMQ Python 客户端不可用，可将消息直接 POST 到本接口模拟 MQ 消费
"""
from typing import List, Optional
from fastapi import APIRouter, BackgroundTasks
from pydantic import BaseModel
from loguru import logger

from app.service.photo_service import photo_analysis_service

router = APIRouter()


class MediaAnalysisMessage(BaseModel):
    """媒体分析消息（与 Java 侧 PhotoAnalysisMessage 保持一致）"""
    photoId: int
    petId: Optional[int] = None
    userId: int
    url: str
    thumbnailUrl: Optional[str] = None
    mediaType: str = 'image'
    takenAt: Optional[str] = None
    tags: Optional[List[str]] = None


@router.post("/photo-analysis")
async def photo_analysis_callback(
    message: MediaAnalysisMessage,
    background_tasks: BackgroundTasks
):
    """
    媒体分析回调接口
    接收 Java 服务发送的照片/视频分析消息（等价于 photo-analysis topic 的 MQ 消息）
    """
    logger.info(f"收到媒体分析消息: photoId={message.photoId}, mediaType={message.mediaType}, petId={message.petId}")

    # 将媒体分析任务放到后台执行
    background_tasks.add_task(process_media_analysis, message)

    return {
        "code": 200,
        "message": "媒体分析任务已接收",
        "data": None
    }


async def process_media_analysis(message: MediaAnalysisMessage):
    """后台处理媒体分析"""
    try:
        logger.info(f"开始处理媒体分析: photoId={message.photoId}")

        await photo_analysis_service.analyze_media(
            photo_id=message.photoId,
            user_id=message.userId,
            pet_id=message.petId,
            url=message.url,
            media_type=message.mediaType,
            tags=message.tags,
            taken_at=message.takenAt
        )

        logger.info(f"媒体分析完成: photoId={message.photoId}")

    except Exception as e:
        logger.error(f"媒体分析失败: photoId={message.photoId}, error={e}", exc_info=True)
