"""FaFa AI 服务统一配置（pydantic-settings）。

设计约定（架构规范 §5.2 / §6.6 / §10）：
    1. 所有配置项集中于本文件，环境变量统一使用 ``FAFA_`` 前缀；
    2. 本地开发支持 ``.env`` 文件（模板见 ``.env.example``），生产由部署 Secret 注入；
    3. Python 服务<strong>不配置任何业务 MySQL 连接</strong>——这是硬边界：
       业务数据一律通过 biz 内网 API 按授权范围获取；
    4. 模型名称、超时、限额均配置化，不硬编码。

使用方式::

    from fafa_ai.config.settings import get_settings
    settings = get_settings()   # 进程内单例
"""

from __future__ import annotations

from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """FaFa AI 服务配置集。

    字段命名即环境变量名（去 ``FAFA_`` 前缀、不区分大小写），
    例如 ``FAFA_LOG_LEVEL`` 对应 :attr:`log_level`。
    """

    model_config = SettingsConfigDict(
        env_prefix="FAFA_",
        env_file=".env",
        env_file_encoding="utf-8",
        # 生产环境禁止从 .env 读取？pydantic-settings 优先级为
        # 环境变量 > .env；部署容器中仅注入环境变量即可覆盖。
        extra="ignore",
    )

    # ==================== 应用基础 ====================
    #: 运行环境标识（dev / test / prod），写入日志便于区分环境
    environment: str = "dev"
    #: 日志级别：生产 INFO，排查期按服务/包临时调整（架构规范 §11.2）
    log_level: str = "INFO"
    #: 服务监听地址
    server_host: str = "0.0.0.0"
    #: 服务监听端口（私网，仅 Java 内网调用，不对公网开放）
    server_port: int = 8090

    # ==================== Java biz 内网接口 ====================
    #: biz 服务内网地址（授权数据获取、AI 结果回传）
    biz_api_base_url: str = "http://192.168.1.254:8081"
    #: 内部服务身份令牌（biz 签发的短时凭据；认证模块实施前留空）
    biz_api_token: str = ""

    # ==================== RabbitMQ ====================
    #: AI Worker 任务队列连接串（vhost=/fafa，见中间件文档 §4.4）
    rabbitmq_url: str = "amqp://fafa:FafaRabbit2026!@192.168.1.254:5672/%2Ffafa"

    # ==================== Qdrant ====================
    #: 语义索引服务地址（索引可重建，不作为授权依据）
    qdrant_url: str = "http://192.168.1.254:6333"

    # ==================== 模型（Qwen / DashScope） ====================
    #: DashScope API Key：仅经环境变量注入，禁止写入代码或仓库
    dashscope_api_key: str = ""
    #: 默认模型名（模型白名单在业务侧另行校验）
    model_name: str = "qwen-plus"
    #: 单次模型调用超时（秒）
    model_timeout_seconds: float = 60.0

    # ==================== 对象存储（开发期 MinIO） ====================
    #: MinIO/S3 端点（host:port）
    minio_endpoint: str = "192.168.1.254:9000"
    #: 访问 Key（生产换为阿里云 OSS AK，经 Secret 注入）
    minio_access_key: str = "minioadmin"
    #: 访问 Secret
    minio_secret_key: str = "minioadmin123"
    #: 默认桶（私有，禁止公共读）
    minio_bucket: str = "fafa-dev-media"


@lru_cache
def get_settings() -> Settings:
    """获取进程级单例配置。

    :return: 全局唯一的 :class:`Settings` 实例（``lru_cache`` 保证重复调用不重复解析环境变量）
    """
    return Settings()
