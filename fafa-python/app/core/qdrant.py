"""
Qdrant 向量数据库客户端
"""
from qdrant_client import QdrantClient
from qdrant_client.models import Distance, VectorParams, PointStruct
from loguru import logger
from app.core.config import settings

# 创建 Qdrant 客户端
qdrant_client = QdrantClient(
    host=settings.QDRANT_HOST,
    port=settings.QDRANT_PORT,
)


async def init_qdrant():
    """初始化 Qdrant 集合"""
    try:
        # 检查集合是否存在
        collections = qdrant_client.get_collections().collections
        collection_names = [c.name for c in collections]
        
        if settings.QDRANT_COLLECTION_NAME not in collection_names:
            # 创建集合
            qdrant_client.create_collection(
                collection_name=settings.QDRANT_COLLECTION_NAME,
                vectors_config=VectorParams(
                    size=1536,  # text-embedding-v2 维度
                    distance=Distance.COSINE
                )
            )
            logger.info(f"创建 Qdrant 集合: {settings.QDRANT_COLLECTION_NAME}")
        else:
            logger.info(f"Qdrant 集合已存在: {settings.QDRANT_COLLECTION_NAME}")
            
    except Exception as e:
        logger.error(f"初始化 Qdrant 失败: {e}")
        raise


def get_qdrant_client() -> QdrantClient:
    """获取 Qdrant 客户端"""
    return qdrant_client
