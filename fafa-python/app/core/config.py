"""
核心配置模块
"""
from typing import Optional
from pydantic_settings import BaseSettings
from functools import lru_cache


class Settings(BaseSettings):
    """应用配置"""
    
    # ==================== 基础配置 ====================
    APP_NAME: str = "FaFa AI Service"
    ENVIRONMENT: str = "development"
    DEBUG: bool = True
    SERVER_HOST: str = "0.0.0.0"
    SERVER_PORT: int = 8000
    
    # ==================== 通义千问 API 配置 ====================
    DASHSCOPE_API_KEY: str = ""
    AI_MODEL: str = "qwen-plus"  # 对话模型
    AI_VISION_MODEL: str = "qwen-vl-plus"  # 视觉理解模型
    AI_EMBEDDING_MODEL: str = "text-embedding-v2"  # 向量化模型
    
    # ==================== MySQL 配置 ====================
    MYSQL_HOST: str = "192.168.1.14"
    MYSQL_PORT: int = 3306
    MYSQL_DATABASE: str = "fafa"
    MYSQL_USERNAME: str = "fafa"
    MYSQL_PASSWORD: str = "fafa_123456"
    
    @property
    def mysql_url(self) -> str:
        """MySQL 连接 URL"""
        return f"mysql+aiomysql://{self.MYSQL_USERNAME}:{self.MYSQL_PASSWORD}@{self.MYSQL_HOST}:{self.MYSQL_PORT}/{self.MYSQL_DATABASE}?charset=utf8mb4"
    
    @property
    def mysql_sync_url(self) -> str:
        """MySQL 同步连接 URL"""
        return f"mysql+pymysql://{self.MYSQL_USERNAME}:{self.MYSQL_PASSWORD}@{self.MYSQL_HOST}:{self.MYSQL_PORT}/{self.MYSQL_DATABASE}?charset=utf8mb4"
    
    # ==================== Redis 配置 ====================
    REDIS_HOST: str = "localhost"
    REDIS_PORT: int = 6379
    REDIS_PASSWORD: Optional[str] = None
    REDIS_DB: int = 2
    REDIS_MAX_CONNECTIONS: int = 10
    
    @property
    def redis_url(self) -> str:
        """Redis 连接 URL"""
        if self.REDIS_PASSWORD:
            return f"redis://:{self.REDIS_PASSWORD}@{self.REDIS_HOST}:{self.REDIS_PORT}/{self.REDIS_DB}"
        return f"redis://{self.REDIS_HOST}:{self.REDIS_PORT}/{self.REDIS_DB}"
    
    # ==================== Qdrant 向量库配置 ====================
    QDRANT_HOST: str = "192.168.1.14"
    QDRANT_PORT: int = 6333
    QDRANT_GRPC_PORT: int = 6334
    QDRANT_COLLECTION_NAME: str = "fafa_photos"
    QDRANT_VECTOR_SIZE: int = 1536  # text-embedding-v2 的维度
    
    @property
    def qdrant_url(self) -> str:
        """Qdrant HTTP URL"""
        return f"http://{self.QDRANT_HOST}:{self.QDRANT_PORT}"
    
    # ==================== RocketMQ 配置 ====================
    ROCKETMQ_HOST: str = "192.168.1.14"
    ROCKETMQ_PORT: int = 9876
    ROCKETMQ_NAME_SERVER: str = "192.168.1.14:9876"
    
    # RocketMQ Topics
    ROCKETMQ_TOPIC_PHOTO_ANALYSIS: str = "photo-analysis"
    ROCKETMQ_TOPIC_REPORT_GENERATION: str = "report-generation"
    
    # RocketMQ Consumer Groups
    ROCKETMQ_CONSUMER_GROUP_PHOTO: str = "fafa-photo-consumer"
    ROCKETMQ_CONSUMER_GROUP_REPORT: str = "fafa-report-consumer"
    
    # ==================== Java 服务配置 ====================
    JAVA_SERVICE_URL: str = "http://localhost:8080/api"
    JAVA_SERVICE_TIMEOUT: int = 30  # 超时时间（秒）

    # ==================== 微信小程序配置 ====================
    WECHAT_APPID: str = ""
    WECHAT_SECRET: str = ""

    # ==================== 日志配置 ====================
    LOG_LEVEL: str = "DEBUG"
    LOG_FILE: str = "logs/fafa-python.log"
    LOG_ROTATION: str = "100 MB"  # 日志轮转大小
    LOG_RETENTION: str = "30 days"  # 日志保留时间
    
    # ==================== 业务配置 ====================
    # 照片分析
    PHOTO_ANALYSIS_MAX_RETRIES: int = 3  # 照片分析失败重试次数
    PHOTO_ANALYSIS_TIMEOUT: int = 60  # 照片分析超时时间（秒）
    
    # 向量搜索
    VECTOR_SEARCH_LIMIT: int = 20  # 向量搜索默认返回数量
    VECTOR_SEARCH_SCORE_THRESHOLD: float = 0.7  # 向量搜索相似度阈值
    
    # AI 对话
    AI_CONVERSATION_MAX_HISTORY: int = 20  # 对话历史最大条数
    AI_MAX_TOKENS: int = 2000  # AI 生成最大 token 数
    
    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"
        case_sensitive = True


@lru_cache()
def get_settings() -> Settings:
    """获取配置单例"""
    return Settings()


# 全局配置实例
settings = get_settings()
