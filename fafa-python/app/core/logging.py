"""loguru 日志配置：按天滚动、保留 30 天、绑定 traceId"""

import sys
from contextvars import ContextVar

from loguru import logger

from app.core.config import settings

# 每个请求/消息的 traceId（中间件写入，日志 patcher 读取）
trace_id_ctx: ContextVar[str] = ContextVar("trace_id", default="-")


def _patcher(record: dict) -> None:
    record["extra"].setdefault("trace_id", trace_id_ctx.get())


def setup_logging() -> None:
    """初始化日志：控制台 + 外部文件夹文件双输出"""
    logger.remove()
    logger.configure(patcher=_patcher)

    fmt = (
        "<green>{time:YYYY-MM-DD HH:mm:ss.SSS}</green> | "
        "<level>{level: <8}</level> | "
        "[{extra[trace_id]}] | "
        "<cyan>{name}</cyan>:<cyan>{line}</cyan> - <level>{message}</level>"
    )

    logger.add(sys.stderr, format=fmt, level="INFO")

    logger.add(
        f"{settings.log_path}/fafa-python.log",
        format=fmt,
        level="INFO",
        rotation="00:00",       # 每天零点切割
        retention="30 days",
        compression="gz",
        enqueue=True,           # 多进程/异步安全
        encoding="utf-8",
    )
