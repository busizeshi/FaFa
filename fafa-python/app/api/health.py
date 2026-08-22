"""
健康检查：返回版本与 Qdrant / 百炼连通状态
"""

import httpx
from fastapi import APIRouter
from loguru import logger

from app.core.config import get_settings
from app.core.qdrant import get_client

router = APIRouter(tags=["health"])


@router.get("/health")
def health() -> dict:
    """
    健康检查

    Returns:
        服务版本 + Qdrant / DashScope 连通状态
    """
    settings = get_settings()

    # Qdrant 连通性
    qdrant_ok = False
    try:
        get_client().get_collections()
        qdrant_ok = True
    except Exception as e:
        logger.error("健康检查: Qdrant 不可达: {}", e)

    # DashScope 连通性（查询模型列表，不消耗推理 token）
    dashscope_ok = False
    if settings.dashscope_api_key:
        try:
            response = httpx.get(
                f"{settings.dashscope_base_url}/models",
                headers={"Authorization": f"Bearer {settings.dashscope_api_key}"},
                timeout=5,
            )
            dashscope_ok = response.status_code == 200
        except Exception as e:
            logger.error("健康检查: DashScope 不可达: {}", e)

    status = "ok" if (qdrant_ok and dashscope_ok) else "degraded"
    return {
        "status": status,
        "version": settings.app_version,
        "dependencies": {
            "qdrant": "ok" if qdrant_ok else "unreachable",
            "dashscope": "ok" if dashscope_ok else ("no-api-key" if not settings.dashscope_api_key else "unreachable"),
        },
    }
