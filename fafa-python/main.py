"""
fafa-python AI 服务入口

启动：uvicorn main:app --host 0.0.0.0 --port 8000
安全：本服务不对外暴露，仅接受 fafa-java 内网调用（X-Internal-Token 校验），
8000 端口在任何环境都不得暴露公网。
"""

import time
import uuid
from contextlib import asynccontextmanager

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse
from loguru import logger
from prometheus_fastapi_instrumentator import Instrumentator

from app import __version__
from app.api import chat, health, photos, pets, record, vector
from app.core.config import get_settings
from app.core.logging import set_trace_id, setup_logging
from app.core.qdrant import ensure_collections


@asynccontextmanager
async def lifespan(app: FastAPI):
    """启动初始化：日志 + Qdrant 集合"""
    setup_logging()
    settings = get_settings()
    logger.info("fafa-python 启动: version={}", settings.app_version)
    ensure_collections()
    yield
    logger.info("fafa-python 关闭")


app = FastAPI(
    title="FaFa AI Service",
    version=__version__,
    description="FaFa 宠物生命周期管理 - AI 服务（内网）",
    lifespan=lifespan,
)

# 路由挂载
app.include_router(health.router)
app.include_router(photos.router)
app.include_router(pets.router)
app.include_router(vector.router)
app.include_router(chat.router)
app.include_router(record.router)

# Prometheus 指标：/metrics
Instrumentator().instrument(app).expose(app)


@app.middleware("http")
async def trace_and_auth_middleware(request: Request, call_next):
    """
    中间件：
    1. 提取/生成 traceId 并注入日志上下文（与 Java 侧 X-Trace-Id 串联）
    2. /api/** 接口校验服务间内部令牌 X-Internal-Token（/health 放行）
    3. 记录请求级日志：方法、路径、耗时、状态码
    """
    settings = get_settings()
    trace_id = request.headers.get("x-trace-id") or uuid.uuid4().hex
    set_trace_id(trace_id)

    # 内部令牌校验
    if request.url.path.startswith("/api/") and settings.internal_token:
        token = request.headers.get("x-internal-token")
        if token != settings.internal_token:
            logger.warning("内部令牌校验失败: path={}", request.url.path)
            return JSONResponse(status_code=401, content={"detail": "invalid internal token"})

    start = time.perf_counter()
    response = await call_next(request)
    elapsed_ms = (time.perf_counter() - start) * 1000

    logger.info(
        "{} {} -> {} {:.1f}ms",
        request.method, request.url.path, response.status_code, elapsed_ms,
    )
    response.headers["X-Trace-Id"] = trace_id
    return response
