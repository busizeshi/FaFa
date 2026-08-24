"""
配置模块

基于 pydantic-settings，优先读取环境变量，其次读取 .env 文件。
敏感信息（DASHSCOPE_API_KEY 等）不入代码库，通过 .env（已 gitignore）注入。
"""

from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """全局配置"""

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    # 服务信息
    app_name: str = "fafa-python"
    app_version: str = "1.0.0"

    # 阿里云百炼（DashScope）OpenAI 兼容入口
    dashscope_api_key: str = ""
    dashscope_base_url: str = "https://dashscope.aliyuncs.com/compatible-mode/v1"

    # 模型映射：用途 -> 模型名（换模型只改配置，不改代码）
    model_chat_light: str = "qwen-flash"
    model_chat_deep: str = "qwen-plus"
    model_vision: str = "qwen-vl-plus"
    model_embed_multimodal: str = "qwen3-vl-embedding"
    # 纯文本向量模型保留给文本专用场景；素材跨模态检索使用 model_embed_multimodal。
    model_embed_text: str = "text-embedding-v4"
    # 视频向量化与视觉描述参数；视频 Embedding 由百炼原生接口按 URL 拉取。
    video_embedding_fps: float = 1.0
    video_vision_fps: float = 2.0
    video_vision_inline_max_bytes: int = 7_500_000

    # Qdrant 向量数据库
    qdrant_url: str = "http://192.168.1.14:6333"
    qdrant_api_key: str = ""

    # fafa-java 回调地址与服务间内部令牌
    java_base_url: str = "http://localhost:8080"
    internal_token: str = ""
    tool_timeout_seconds: float = 8.0
    agent_timeout_seconds: float = 45.0
    max_tool_calls: int = 5
    media_search_limit: int = 20

    # 日志输出目录
    log_path: str = "./logs/fafa-python"

    # RocketMQ 消费者（仅 Linux 环境启用，Windows 走 HTTP 直推链路）
    consumer_enabled: bool = False
    rocketmq_namesrv: str = ""

    # Phase 4 素材分析与语义检索
    recognition_threshold: float = 0.85
    search_score_threshold: float = 0.30
    callback_retry: int = 3

    # Phase 7 食品包装识别
    food_recognition_prompt_version: str = "food-recognition-v1"
    food_recognition_max_image_bytes: int = 10_485_760
    food_recognition_timeout_seconds: float = 90.0


@lru_cache
def get_settings() -> Settings:
    """获取全局配置单例"""
    return Settings()
