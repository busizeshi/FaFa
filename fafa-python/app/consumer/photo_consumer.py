"""
RocketMQ 消费者（仅 Linux 生产环境启用）

Windows 开发环境不支持 rocketmq Python 客户端，
开发链路由 Java 直接 HTTP 调用 /api/pets/profile-photos 替代（见技术文档 7.1）。

消费链路（与 Java RocketMqAiTaskDispatcher 对应）：
- Consumer Group: fafa_python_photo_group
- Topic: fafa_pet_photo_vectorization
- 消息体 camelCase（petId/userId/avatarUrl/frontUrl/sideUrl）→ 服务层 snake_case
- 幂等：point_id 由 {pet_id}_{view} 确定性生成，重复消费 upsert 无副作用
- 失败：返回 RECONSUME_LATER，由 broker 按延迟级别重试，超限进死信队列人工处理
"""

import asyncio
import json
import threading
from datetime import datetime

from loguru import logger

from app.core.config import get_settings

CONSUMER_GROUP = "fafa_python_photo_group"
TOPIC_PET_PHOTO_VECTORIZATION = "fafa_pet_photo_vectorization"
TOPIC_PHOTO_ANALYSIS = "fafa_photo_analysis_topic"

# 消费者生命周期（进程内单例）
_consumer = None
_consumer_lock = threading.Lock()


def _handle_message(msg):
    """
    消费回调：解析消息体并调用向量化服务

    在 C++ 客户端回调线程中执行，通过 asyncio.run 桥接异步嵌入逻辑。
    成功返回 CONSUME_SUCCESS，失败返回 RECONSUME_LATER 触发 broker 重试。
    """
    # 延迟导入：rocketmq 仅 Linux 可安装，Windows 开发环境导入本模块不报错
    from rocketmq.client import ConsumeStatus

    from app.service import pet_vector_service
    from app.service.photo_service import photo_service as photo_analysis_service

    msg_id = getattr(msg, "id", "unknown")
    try:
        body = msg.body.decode("utf-8") if isinstance(msg.body, bytes) else str(msg.body)
        data = json.loads(body)

        topic = getattr(msg, "topic", TOPIC_PET_PHOTO_VECTORIZATION)
        if topic == TOPIC_PHOTO_ANALYSIS:
            media_id = data["photoId"]
            user_id = data["userId"]
            taken_at_value = data.get("takenAt")
            taken_at = datetime.fromisoformat(taken_at_value) if taken_at_value else None
            uploaded_at_value = data.get("uploadedAt")
            uploaded_at = datetime.fromisoformat(uploaded_at_value) if uploaded_at_value else None
            logger.info("收到素材分析消息: msg_id={}, media_id={}, user_id={}", msg_id, media_id, user_id)
            asyncio.run(photo_analysis_service.analyze(
                user_id=user_id,
                photo_id=media_id,
                url=data["url"],
                file_url=data.get("fileUrl") or data.get("file_url"),
                media_type=data.get("mediaType", "photo"),
                tags=data.get("tags"),
                pet_id=data.get("petId"),
                candidate_pet_ids=data.get("candidatePetIds") or data.get("candidate_pet_ids"),
                message_id=data.get("messageId") or msg_id,
                taken_at=taken_at,
                uploaded_at=uploaded_at,
                trace_id=data.get("traceId"),
            ))
            logger.info("素材分析消息消费成功: msg_id={}, media_id={}", msg_id, media_id)
            return ConsumeStatus.CONSUME_SUCCESS

        pet_id = data["petId"]
        user_id = data["userId"]

        logger.info("收到宠物照片向量化消息: msg_id={}, pet_id={}, user_id={}",
                    msg_id, pet_id, user_id)

        asyncio.run(pet_vector_service.embed_profile_photos(
            pet_id=pet_id,
            user_id=user_id,
            avatar_url=data.get("avatarUrl") or "",
            front_url=data.get("frontUrl") or "",
            side_url=data.get("sideUrl") or "",
        ))

        logger.info("宠物照片向量化消息消费成功: msg_id={}, pet_id={}", msg_id, pet_id)
        return ConsumeStatus.CONSUME_SUCCESS

    except (KeyError, json.JSONDecodeError, TypeError, ValueError) as e:
        # 消息体格式错误不可恢复，重试无意义，记录日志后 ack 丢弃，人工排查
        logger.error("向量化消息体解析失败，丢弃消息: msg_id={}, error={}", msg_id, str(e))
        return ConsumeStatus.CONSUME_SUCCESS

    except Exception as e:
        # 嵌入/写库失败可重试（如 DashScope 限流、Qdrant 抖动）
        logger.error("向量化消息消费失败，稍后重试: msg_id={}, error={}", msg_id, str(e))
        return ConsumeStatus.RECONSUME_LATER


def start_photo_consumer() -> bool:
    """
    启动宠物照片向量化消费者（幂等，重复调用只启动一次）

    Returns:
        True 启动成功；False 未启用或启动失败（不阻断主服务）
    """
    global _consumer

    settings = get_settings()

    if not settings.consumer_enabled:
        logger.info("RocketMQ 消费者未启用（consumer_enabled=false），跳过启动")
        return False

    if not settings.rocketmq_namesrv:
        logger.error("consumer_enabled=true 但 ROCKETMQ_NAMESRV 未配置，消费者启动失败")
        return False

    with _consumer_lock:
        if _consumer is not None:
            logger.info("RocketMQ 消费者已在运行，跳过重复启动")
            return True

        # 延迟导入：Windows 开发环境未安装 rocketmq 时给出明确提示
        try:
            from rocketmq.client import PushConsumer
        except ImportError:
            logger.error("rocketmq-client-python 未安装，无法启动消费者（仅支持 Linux 部署）")
            return False

        try:
            consumer = PushConsumer(CONSUMER_GROUP)
            consumer.set_name_server_address(settings.rocketmq_namesrv)
            consumer.subscribe(TOPIC_PET_PHOTO_VECTORIZATION, _handle_message)
            consumer.subscribe(TOPIC_PHOTO_ANALYSIS, _handle_message)
            consumer.start()
            _consumer = consumer
            logger.info("RocketMQ 消费者启动成功: group={}, topics=[{}, {}], namesrv={}",
                        CONSUMER_GROUP, TOPIC_PET_PHOTO_VECTORIZATION, TOPIC_PHOTO_ANALYSIS,
                        settings.rocketmq_namesrv)
            return True

        except Exception as e:
            logger.error("RocketMQ 消费者启动失败: {}", str(e))
            return False


def stop_photo_consumer() -> None:
    """停止消费者（应用关闭时调用）"""
    global _consumer

    with _consumer_lock:
        if _consumer is None:
            return
        try:
            _consumer.shutdown()
            logger.info("RocketMQ 消费者已停止")
        except Exception as e:
            logger.error("RocketMQ 消费者停止失败: {}", str(e))
        finally:
            _consumer = None
