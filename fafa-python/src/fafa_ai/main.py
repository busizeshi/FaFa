"""FaFa AI 服务 FastAPI 入口。

启动方式（开发机）::

    uv run uvicorn fafa_ai.main:app --host 0.0.0.0 --port 8090 --reload

服务边界（架构规范 §6.6）：
    - 本服务仅对 Java 内网开放（biz 同步调用入口 / SSE 流式回答）；
    - 所有请求必须由 biz 携带内部身份与授权范围发起，本服务不面对终端用户。
"""

from __future__ import annotations

import logging
import time
import uuid
from contextlib import asynccontextmanager

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

from fafa_ai import __version__
from fafa_ai.api.routes import health
from fafa_ai.config.settings import get_settings
from fafa_ai.logging_config import request_id_var, setup_logging

logger = logging.getLogger(__name__)

#: 统一业务错误码（与 Java 侧 ErrorCode 对齐；成功 "0"）
CODE_INTERNAL_ERROR = "500-01-00"


@asynccontextmanager
async def lifespan(app: FastAPI):
    """应用生命周期：启动时初始化日志与配置，关闭时释放资源。

    后续扩展点：RabbitMQ 消费者启停、Qdrant 连接池、模型客户端预热。
    """
    settings = get_settings()
    setup_logging(level=settings.log_level, environment=settings.environment)
    logger.info("FaFa AI 服务启动完成 version=%s environment=%s", __version__, settings.environment)
    yield
    logger.info("FaFa AI 服务关闭")


def create_app() -> FastAPI:
    """构建 FastAPI 应用（工厂模式，便于测试注入配置）。

    :return: 配置完成的应用实例
    """
    settings = get_settings()
    app = FastAPI(
        title="FaFa AI Service",
        description=(
            "FaFa AI 能力服务：LangGraph 工作流、Qwen 模型适配、OCR/语义处理。"
            "仅接受 Java biz 内网调用；AI 输出为候选草稿，最终落库由 biz 领域用例完成。"
        ),
        version=__version__,
        lifespan=lifespan,
        # API 文档仅内网排障使用（架构规范 §3）
        docs_url="/docs" if settings.environment != "prod" else None,
    )

    @app.middleware("http")
    async def request_context_middleware(request: Request, call_next):
        """请求上下文中间件：生成/沿用 requestId，输出访问日志与耗时。"""
        request_id = request.headers.get("X-Request-Id") or uuid.uuid4().hex
        request_id_var.set(request_id)
        start = time.perf_counter()

        response = await call_next(request)

        cost_ms = (time.perf_counter() - start) * 1000
        # requestId 回写响应头，供调用方（Java biz）日志串联
        response.headers["X-Request-Id"] = request_id
        logger.info(
            "%s %s -> %s %.1fms",
            request.method,
            request.url.path,
            response.status_code,
            cost_ms,
        )
        return response

    @app.exception_handler(Exception)
    async def unhandled_exception_handler(request: Request, exc: Exception):
        """兜底异常处理：对外返回稳定错误结构，堆栈只进日志（架构规范 §8.2）。"""
        logger.exception("未处理异常 path=%s", request.url.path)
        return JSONResponse(
            status_code=500,
            content={
                "code": CODE_INTERNAL_ERROR,
                "message": "服务内部错误，请稍后再试",
                "requestId": request_id_var.get(),
                "retryable": True,
            },
        )

    # 路由注册：健康检查为脚手架内置，AI 业务路由随模块开发补充
    app.include_router(health.router)

    return app


#: uvicorn 入口
app = create_app()
