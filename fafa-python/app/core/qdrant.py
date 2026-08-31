"""
Qdrant 向量库管理

集合规划（见技术文档 5.5）：
- fafa_media        照片/视频素材向量（qwen3-vl-embedding，1024 维）
- fafa_pet_profiles 宠物三视图向量（同上）

Point ID 由 embedding_id 经 uuid5 确定性生成，保证幂等 upsert，
禁止随机 UUID。
"""

import uuid
from functools import lru_cache

from loguru import logger
from qdrant_client import QdrantClient
from qdrant_client.models import Distance, PayloadSchemaType, VectorParams

from app.core.config import get_settings

# 集合常量
COLLECTION_MEDIA = "fafa_media"
COLLECTION_PET_PROFILES = "fafa_pet_profiles"

# 统一向量维度（qwen3-vl-embedding）
VECTOR_DIM = 1024

# uuid5 命名空间（固定值，保证同一 embedding_id 生成同一 point id）
_NAMESPACE = uuid.UUID("6f1f2a3c-0000-4000-8000-fafa00000001")


@lru_cache
def get_client() -> QdrantClient:
    """获取 Qdrant 客户端单例"""
    settings = get_settings()
    logger.info("连接 Qdrant: {}", settings.qdrant_url)
    return QdrantClient(url=settings.qdrant_url, api_key=settings.qdrant_api_key or None)


def point_id(embedding_id: str) -> str:
    """
    由业务 embedding_id 确定性生成 Qdrant point ID

    同一 embedding_id 永远得到同一 point id，删除与 upsert 天然幂等。
    """
    return str(uuid.uuid5(_NAMESPACE, embedding_id))


def ensure_collections() -> None:
    """启动时确保两个业务集合存在，不存在则创建"""
    client = get_client()
    for collection in (COLLECTION_MEDIA, COLLECTION_PET_PROFILES):
        try:
            if not client.collection_exists(collection):
                client.create_collection(
                    collection_name=collection,
                    vectors_config=VectorParams(size=VECTOR_DIM, distance=Distance.COSINE),
                )
                logger.info("创建 Qdrant 集合: {}, dim={}", collection, VECTOR_DIM)
            else:
                logger.info("Qdrant 集合已存在: {}", collection)
            if collection == COLLECTION_MEDIA:
                _ensure_media_payload_indexes(client)
        except Exception as e:
            # 集合初始化失败不阻断启动，记日志人工处理
            logger.error("Qdrant 集合初始化失败: collection={}, error={}", collection, e)


def _ensure_media_payload_indexes(client: QdrantClient) -> None:
    """为媒体筛选和按拍摄时间精确召回创建必要的 payload 索引。"""
    for field_name in ("user_id", "pet_id", "pet_ids", "taken_ts", "uploaded_ts"):
        try:
            client.create_payload_index(
                collection_name=COLLECTION_MEDIA,
                field_name=field_name,
                field_schema=PayloadSchemaType.INTEGER,
                wait=True,
            )
        except Exception as exc:
            # 已存在或短暂不可用均不影响服务启动；Qdrant 会在下次启动继续补偿。
            logger.warning("Qdrant payload 索引确保失败: collection={}, field={}, error={}",
                           COLLECTION_MEDIA, field_name, str(exc))


def drop_collection(collection: str) -> None:
    """删除集合（运维操作慎用）"""
    get_client().delete_collection(collection_name=collection)
    logger.warning("已删除 Qdrant 集合: {}", collection)
