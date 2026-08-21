"""Qdrant 封装：集合管理 + 确定性 point id

两个集合统一使用 qwen3-vl-embedding（1024 维），point id 由 embedding_id
经 uuid5 确定性生成，保证同一素材重复处理时幂等 upsert。
"""

import uuid

from loguru import logger
from qdrant_client import QdrantClient
from qdrant_client.models import Distance, VectorParams

from app.core.config import settings

MEDIA_COLLECTION = "fafa_media"          # 照片/视频素材向量
PET_PROFILES_COLLECTION = "fafa_pet_profiles"  # 宠物三视图向量

# uuid5 命名空间（固定值，勿改动：改动会导致新旧 point id 不一致）
_NAMESPACE = uuid.UUID("6f617069-0000-4000-8000-666166610001")

_client: QdrantClient | None = None


def get_client() -> QdrantClient:
    """获取 Qdrant 客户端（懒加载单例）"""
    global _client
    if _client is None:
        _client = QdrantClient(url=settings.qdrant_url)
    return _client


def point_id_from_embedding_id(embedding_id: str) -> str:
    """embedding_id -> 确定性 uuid5 point id（幂等 upsert 的关键）"""
    return str(uuid.uuid5(_NAMESPACE, embedding_id))


def ensure_collections() -> None:
    """确保两个向量集合存在（维度以 settings.embedding_dim 为准）"""
    client = get_client()
    dim = settings.embedding_dim
    for name in (MEDIA_COLLECTION, PET_PROFILES_COLLECTION):
        if not client.collection_exists(name):
            client.create_collection(
                collection_name=name,
                vectors_config=VectorParams(size=dim, distance=Distance.COSINE),
            )
            logger.info(f"Qdrant 集合已创建: {name} (dim={dim})")
