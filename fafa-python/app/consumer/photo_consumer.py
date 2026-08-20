"""
RocketMQ 消费者服务

注意：
1. rocketmq-client-python 不支持 Windows 系统
2. 仅在 Linux/Mac 环境下可用
3. 开发环境（Windows）可以使用 HTTP 回调接口代替
"""
import json
import asyncio
from typing import Optional
from loguru import logger
from rocketmq.client import PushConsumer, ConsumeStatus

from app.service.photo_service import PhotoAnalysisService
from app.core.config import settings


class PhotoAnalysisConsumer:
    """照片分析消费者"""
    
    def __init__(self):
        self.consumer: Optional[PushConsumer] = None
        self.photo_service = PhotoAnalysisService()
        
    def start(self):
        """启动消费者"""
        try:
            self.consumer = PushConsumer('photo-analysis-consumer-group')
            self.consumer.set_name_server_address(settings.ROCKETMQ_NAME_SERVER)
            self.consumer.subscribe('photo-analysis', self._callback)
            self.consumer.start()
            logger.info("RocketMQ 照片分析消费者启动成功")
        except Exception as e:
            logger.error(f"启动 RocketMQ 消费者失败: {e}")
            raise
    
    def _callback(self, msg):
        """消息回调处理"""
        try:
            # 解析消息
            body = msg.body.decode('utf-8')
            message_data = json.loads(body)
            
            logger.info(f"收到照片分析消息: photoId={message_data.get('photoId')}")
            
            # 异步处理照片分析
            asyncio.create_task(self._process_photo_analysis(message_data))
            
            return ConsumeStatus.CONSUME_SUCCESS
            
        except Exception as e:
            logger.error(f"处理照片分析消息失败: {e}", exc_info=True)
            return ConsumeStatus.RECONSUME_LATER
    
    async def _process_photo_analysis(self, message_data: dict):
        """处理照片分析"""
        try:
            photo_id = message_data.get('photoId')
            pet_id = message_data.get('petId')
            url = message_data.get('url')
            
            if not all([photo_id, pet_id, url]):
                logger.error(f"照片分析消息缺少必要字段: {message_data}")
                return
            
            # 调用照片分析服务
            await self.photo_service.analyze_photo(
                photo_id=photo_id,
                pet_id=pet_id,
                url=url
            )
            
            logger.info(f"照片分析完成: photoId={photo_id}")
            
        except Exception as e:
            logger.error(f"照片分析处理失败: photoId={message_data.get('photoId')}, error={e}", exc_info=True)
    
    def shutdown(self):
        """关闭消费者"""
        if self.consumer:
            self.consumer.shutdown()
            logger.info("RocketMQ 照片分析消费者已关闭")


# 全局消费者实例
photo_consumer: Optional[PhotoAnalysisConsumer] = None


def start_photo_consumer():
    """启动照片分析消费者"""
    global photo_consumer
    if photo_consumer is None:
        photo_consumer = PhotoAnalysisConsumer()
        photo_consumer.start()


def stop_photo_consumer():
    """停止照片分析消费者"""
    global photo_consumer
    if photo_consumer:
        photo_consumer.shutdown()
        photo_consumer = None
