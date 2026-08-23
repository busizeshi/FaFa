"""向量路由：素材向量删除。"""

from fastapi import APIRouter
from loguru import logger
from qdrant_client.models import PointIdsList

from app.core.qdrant import COLLECTION_MEDIA, get_client, point_id

router = APIRouter(prefix="/api/vector", tags=["vector"])


@router.delete("/{embedding_id}")
def delete_vector(embedding_id: str) -> dict:
    """按确定性 point id 删除素材向量；point 不存在时同样视为成功。"""
    logger.info("收到向量删除请求: embedding_id={}", embedding_id)
    get_client().delete(
        collection_name=COLLECTION_MEDIA,
        points_selector=PointIdsList(points=[point_id(embedding_id)]),
    )
    return {"deleted": True, "embedding_id": embedding_id}
