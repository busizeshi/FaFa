"""RocketMQ 消费者（仅 Linux 部署环境启用）

Windows 开发环境 rocketmq 客户端不可用，main.py 不导入本模块；
Linux 部署时单独进程启动：
    python -m app.consumer.photo_consumer
"""

from loguru import logger

from app.schemas.photo import AnalyzeMediaRequest

CONSUMER_GROUP = "fafa_python_photo_group"
TOPIC = "fafa_photo_analysis_topic"


def _to_request(msg_body: dict) -> AnalyzeMediaRequest:
    """MQ 消息体（camelCase）-> 请求数据（snake_case）"""
    return AnalyzeMediaRequest(
        photo_id=msg_body["photoId"],
        user_id=msg_body["userId"],
        pet_id=msg_body.get("petId"),
        url=msg_body["url"],
        media_type=msg_body.get("mediaType", "photo"),
        tags=msg_body.get("tags", []),
        trace_id=msg_body.get("traceId"),
    )


def start_consumer() -> None:
    """启动照片分析消费者（幂等：以 photo_id 判重）

    TODO(M5): 接入 rocketmq-client-python，消费逻辑复用 photo_service（与 HTTP 直推同一条代码路径）
    """
    logger.info("MQ 消费者骨架：Linux 环境部署时实现")


if __name__ == "__main__":
    start_consumer()
