"""
核心配置模块
"""
from typing import Optional
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    """应用配置"""
    
    # 基础配置
    APP_NAME: str = "FaFa AI Service"
    ENVIRONMENT: str = "development"
    DEBUG: bool = True
    
    # 通义千问配置
    DASHSCOPE_API_KEY: str = ""
    AI_MODEL: str = "qwen-plus"  # qwen3.7-flash 的别名
    AI_VISION_MODEL: str = "qwen-vl-plus"
    AI_EMBEDDING_MODEL: str = "text-embedding-v2"
    
    # MySQL 配置
    MYSQL_HOST: str = "localhost"
    MYSQL_PORT: int = 3306
    MYSQL_DATABASE: str = "fafa"
    MYSQL_USERNAME: str = "root"
    MYSQL_PASSWORD: str = "password"
    
    # Redis 配置
    REDIS_HOST: str = "localhost"
    REDIS_PORT: int = 6379
    REDIS_PASSWORD: Optional[str] = None
    REDIS_DB: int = 2
    
    # Qdrant 向量库配置
    QDRANT_HOST: str = "localhost"
    QDRANT_PORT: int = 6333
    QDRANT_COLLECTION_NAME: str = "fafa_photos"
    
    # RocketMQ 配置
    ROCKETMQ_HOST: str = "localhost"
    ROCKETMQ_PORT: int = 9876
    
    # Java 服务地址
    JAVA_SERVICE_URL: str = "http://localhost:8080"
    
    class Config:
        env_file = ".env"
        case_sensitive = True


# 全局配置实例
settings = Settings()
