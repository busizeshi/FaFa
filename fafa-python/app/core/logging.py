"""
日志模块

loguru 结构化日志，按天滚动、保留 30 天，输出到外部文件夹（LOG_PATH 可覆盖）。
trace_id 通过 contextvar 在请求链路中传递，请求结束时自动清理。
"""

import sys
from contextvars import ContextVar

from loguru import logger

from app.core.config import get_settings

# 当前请求的 traceId（Java 侧经 X-Trace-Id 头传入）
trace_id_var: ContextVar[str] = ContextVar("trace_id", default="-")

_FORMAT = (
    "<green>{time:YYYY-MM-DD HH:mm:ss.SSS}</green> | "
    "<level>{level: <8}</level> | "
    "trace_id={extra[trace_id]} | "
    "<cyan>{name}</cyan>:<cyan>{line}</cyan> - <level>{message}</level>"
)


def setup_logging() -> None:
    """初始化日志：控制台 + 文件双输出"""
    settings = get_settings()

    logger.remove()
    logger.configure(extra={"trace_id": "-"})
    logger.add(sys.stdout, format=_FORMAT, level="INFO")

    logger.add(
        f"{settings.log_path}/fafa-python.log",
        format=_FORMAT,
        level="INFO",
        rotation="00:00",      # 按天滚动
        retention="30 days",
        encoding="utf-8",
    )


def set_trace_id(trace_id: str) -> None:
    """设置当前请求的 traceId，并注入 loguru 上下文"""
    trace_id_var.set(trace_id or "-")
    logger.configure(extra={"trace_id": trace_id or "-"})


def get_trace_id() -> str:
    """读取当前链路 traceId"""
    return trace_id_var.get()
