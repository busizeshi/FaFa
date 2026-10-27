"""健康检查路由。

说明：
    - ``/healthz`` 存活探针：进程可用即返回 200（k8s/Compose liveness 用）；
    - ``/readyz`` 就绪探针：依赖（RabbitMQ/Qdrant/模型）连接正常才返回 200，
      脚手架阶段暂只校验配置加载，接入依赖后逐项补充检查（架构规范 §11.1）。
"""

from __future__ import annotations

import logging

from fastapi import APIRouter

from fafa_ai import __version__
from fafa_ai.config.settings import get_settings

logger = logging.getLogger(__name__)

router = APIRouter(tags=["ops"])


@router.get("/healthz", summary="存活探针")
async def healthz() -> dict:
    """存活检查：进程存活即返回服务标识与版本。

    :return: 服务名与版本号
    """
    logger.debug("存活探针被调用")
    return {"service": "fafa-ai", "status": "UP", "version": __version__}


@router.get("/readyz", summary="就绪探针")
async def readyz() -> dict:
    """就绪检查：确认配置可加载；后续接入依赖健康检查（RabbitMQ/Qdrant/模型连通性）。

    :return: 各依赖的就绪状态
    """
    settings = get_settings()
    # 脚手架阶段：仅验证配置加载与必需密钥存在性，不发起网络探测
    dependencies = {
        "config": "UP",
        # 待 RabbitMQ 消费者接入后启用
        "rabbitmq": "SKIPPED",
        # 待 Qdrant 部署后启用（见中间件文档 §9 待办）
        "qdrant": "SKIPPED",
        # 模型 Key 未配置时标记未就绪（AI 功能不可用但服务可存活）
        "qwen": "UP" if settings.dashscope_api_key else "NOT_CONFIGURED",
    }
    logger.info("就绪探针被调用 dependencies=%s", dependencies)
    return {"service": "fafa-ai", "status": "UP", "dependencies": dependencies}
