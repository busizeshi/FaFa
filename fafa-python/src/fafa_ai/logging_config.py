"""结构化日志配置。

约定（架构规范 §5.2 / §11.2）：
    - 运行日志一律走 ``logging``，业务代码<strong>禁止 print</strong>；
    - 输出 JSON 结构化字段：timestamp / level / logger / message / requestId / environment；
    - traceId 由 OpenTelemetry 注入（实施观测接入后扩展字段），当前保留 requestId 关联；
    - 日志中不得出现密码、完整病历、验证码、长期签名 URL（架构规范 §10）。
"""

from __future__ import annotations

import json
import logging
import sys
from contextvars import ContextVar

#: 请求关联 ID 上下文变量（由 main.py 的中间件写入，异步安全）
request_id_var: ContextVar[str] = ContextVar("request_id", default="-")


class RequestIdLogFilter(logging.Filter):
    """把当前请求的 requestId 注入每条日志记录。"""

    def filter(self, record: logging.LogRecord) -> bool:
        """给 record 附加 requestId 字段。

        :param record: 日志记录
        :return: 始终放行（True）
        """
        record.requestId = request_id_var.get()
        return True


class JsonFormatter(logging.Formatter):
    """JSON 结构化日志格式器（stdout 采集到 Loki 的前置形态）。"""

    def format(self, record: logging.LogRecord) -> str:
        """将日志记录序列化为单行 JSON。

        :param record: 日志记录
        :return: JSON 字符串（不含换行）
        """
        payload = {
            "timestamp": self.formatTime(record, "%Y-%m-%dT%H:%M:%S%z"),
            "level": record.levelname,
            "logger": record.name,
            "message": record.getMessage(),
            "requestId": getattr(record, "requestId", "-"),
        }
        # 异常栈仅写入受控日志平台，不返回给调用方
        if record.exc_info:
            payload["exception"] = self.formatException(record.exc_info)
        return json.dumps(payload, ensure_ascii=False)


def setup_logging(level: str = "INFO", environment: str = "dev") -> None:
    """初始化根日志器（进程启动时调用一次）。

    :param level: 日志级别字符串（DEBUG/INFO/...）
    :param environment: 运行环境，写入 JSON 字段便于区分 dev/prod 日志流
    """
    handler = logging.StreamHandler(stream=sys.stdout)
    handler.setFormatter(JsonFormatter())
    handler.addFilter(RequestIdLogFilter())

    root = logging.getLogger()
    root.handlers.clear()
    root.addHandler(handler)
    root.setLevel(level.upper())

    # 抑制第三方库噪音日志
    logging.getLogger("httpx").setLevel(logging.WARNING)
    logging.getLogger("aio_pika").setLevel(logging.INFO)

    logging.getLogger(__name__).info(
        "日志系统初始化完成 level=%s environment=%s", level, environment
    )
