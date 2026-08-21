"""健康检查：版本 + Qdrant 连通状态"""

from fastapi import APIRouter
from loguru import logger

from app.core.config import settings
from app.core.qdrant import get_client

router = APIRouter(tags=["health"])


@router.get("/health")
def health() -> dict:
    """存活探针。qdrant 不可达时整体仍返回 200，status 标记 degraded"""
    result: dict = {
        "service": "fafa-python",
        "version": "0.1.0",
        "status": "ok",
        "qdrant": "ok",
    }
    try:
        get_client().get_collections()
    except Exception as e:  # noqa: BLE001 健康检查需要吞掉一切依赖异常
        result["status"] = "degraded"
        result["qdrant"] = f"unreachable: {e}"
        logger.warning(f"健康检查: Qdrant 不可达: {e}")
    return result


@router.get("/health/config")
def health_config() -> dict:
    """配置指纹（脱敏）：确认模型路由映射是否生效，不暴露 API Key"""
    return {
        "chat_light": settings.model_chat_light,
        "chat_deep": settings.model_chat_deep,
        "vision": settings.model_vision,
        "embed_text": settings.model_embed_text,
        "embed_multimodal": settings.model_embed_multimodal,
        "embedding_dim": settings.embedding_dim,
        "api_key_set": bool(settings.dashscope_api_key),
    }
