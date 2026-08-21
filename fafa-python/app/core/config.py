"""配置：环境变量前缀 FAFA_，支持 .env 文件覆盖"""

from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """全局配置（模型路由：用途 -> 模型名，换模型只改配置不改代码）"""

    model_config = SettingsConfigDict(env_file=".env", env_prefix="FAFA_", extra="ignore")

    port: int = 8000

    # 阿里云百炼
    dashscope_api_key: str = ""
    dashscope_base_url: str = "https://dashscope.aliyuncs.com/compatible-mode/v1"

    # 模型路由映射
    model_chat_light: str = "qwen-turbo"          # 轻量对话/意图识别/标签生成
    model_chat_deep: str = "qwen-plus"            # 复杂推理/Agent 编排/报告生成
    model_vision: str = "qwen-vl-plus"            # 图像/视频理解
    model_embed_text: str = "text-embedding-v4"   # 文本嵌入
    model_embed_multimodal: str = "qwen3-vl-embedding"  # 多模态嵌入（1024 维）
    embedding_dim: int = 1024

    # Qdrant
    qdrant_url: str = "http://127.0.0.1:6333"

    # Java 服务（回调/Agent 工具取数）
    java_base_url: str = "http://127.0.0.1:8080"
    internal_token: str = "dev-internal-token"

    # 日志
    log_path: str = "./logs/fafa-python"


@lru_cache
def get_settings() -> Settings:
    return Settings()


settings = get_settings()
