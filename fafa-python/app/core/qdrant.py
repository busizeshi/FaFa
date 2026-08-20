"""
Qdrant 向量数据库客户端 (基于 qwen3-vl-embedding)
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
    """
    初始化 Qdrant 集合
    
    创建两个集合:
    1. fafa_media: 存储照片/视频向量 (用于语义搜索)
    2. fafa_pet_profiles: 存储宠物三视图向量 (用于宠物识别)
    """
    try:
        # 获取已存在的集合
        collections = qdrant_client.get_collections().collections
        collection_names = [c.name for c in collections]
        
        # 1. 创建媒体集合 (fafa_media)
        if settings.QDRANT_COLLECTION_MEDIA not in collection_names:
            qdrant_client.create_collection(
                collection_name=settings.QDRANT_COLLECTION_MEDIA,
                vectors_config=VectorParams(
                    size=settings.QWEN3_VL_EMBEDDING_DIM,  # 1024 维度
                    distance=Distance.COSINE
                )
            )
            logger.info(f"创建 Qdrant 集合: {settings.QDRANT_COLLECTION_MEDIA} (size={settings.QWEN3_VL_EMBEDDING_DIM})")
        else:
            logger.info(f"Qdrant 集合已存在: {settings.QDRANT_COLLECTION_MEDIA}")
        
        # 2. 创建宠物三视图集合 (fafa_pet_profiles)
        if settings.QDRANT_COLLECTION_PET_PROFILES not in collection_names:
            qdrant_client.create_collection(
                collection_name=settings.QDRANT_COLLECTION_PET_PROFILES,
                vectors_config=VectorParams(
                    size=settings.QWEN3_VL_EMBEDDING_DIM,  # 1024 维度
                    distance=Distance.COSINE
                )
            )
            logger.info(f"创建 Qdrant 集合: {settings.QDRANT_COLLECTION_PET_PROFILES} (size={settings.QWEN3_VL_EMBEDDING_DIM})")
        else:
            logger.info(f"Qdrant 集合已存在: {settings.QDRANT_COLLECTION_PET_PROFILES}")
        
        logger.info("Qdrant 集合初始化完成")
            
    except Exception as e:
        logger.error(f"初始化 Qdrant 失败: {e}")
        raise


def get_qdrant_client() -> QdrantClient:
    """获取 Qdrant 客户端"""
    return qdrant_client
