"""
宠物向量化服务：三视图嵌入 + 向量清理

从 API 层抽取的核心逻辑，供两条链路共用：
- dev：Java HTTP 直推 /api/pets/profile-photos
- prod：RocketMQ 消费者拉取 fafa_pet_photo_vectorization 消息后调用

幂等保证：point_id 由 {pet_id}_{view} 经 uuid5 确定性生成，upsert 天然幂等，
重复消费/重复调用不会产生脏数据。
"""

from typing import List

from loguru import logger
from qdrant_client.models import FieldCondition, Filter, MatchValue

from app.core.dashscope_client import get_multimodal_embedding
from app.core.qdrant import COLLECTION_PET_PROFILES, get_client, point_id


async def embed_profile_photos(pet_id: int, user_id: int,
                              avatar_url: str = "",
                              front_url: str = "",
                              side_url: str = "") -> List[str]:
    """
    宠物照片向量化：对提供的照片分别生成 1024 维向量并 upsert 到 Qdrant

    Args:
        pet_id: 宠物 ID
        user_id: 用户 ID
        avatar_url: 头像 URL（空字符串表示本次不更新头像向量）
        front_url: 正面照 URL（空字符串跳过）
        side_url: 侧面照 URL（空字符串跳过）

    Returns:
        本次写入的 Qdrant point ID 列表

    Raises:
        RuntimeError: 嵌入生成或向量库写入失败
    """
    logger.info("开始宠物照片向量化: pet_id={}, user_id={}", pet_id, user_id)

    # 构建待向量化的照片列表（跳过空 URL）
    photos = []
    if avatar_url:
        photos.append({"url": avatar_url, "view": "avatar"})
    if front_url:
        photos.append({"url": front_url, "view": "front"})
    if side_url:
        photos.append({"url": side_url, "view": "side"})

    if not photos:
        logger.warning("宠物照片向量化无有效 URL，跳过: pet_id={}", pet_id)
        return []

    qdrant = get_client()
    written_ids: List[str] = []

    for photo in photos:
        logger.info("正在向量化: pet_id={}, view={}", pet_id, photo["view"])

        embedding = await get_multimodal_embedding(photo["url"])

        # 确定性 point_id：同一 pet_id + view 永远生成同一 ID，upsert 天然幂等
        pid = point_id(f"{pet_id}_{photo['view']}")

        qdrant.upsert(
            collection_name=COLLECTION_PET_PROFILES,
            points=[{
                "id": pid,
                "vector": embedding,
                "payload": {
                    "user_id": user_id,
                    "pet_id": pet_id,
                    "view": photo["view"],
                    "photo_url": photo["url"],
                }
            }]
        )
        written_ids.append(pid)
        logger.info("向量化完成: pet_id={}, view={}, point_id={}", pet_id, photo["view"], pid)

    logger.info("宠物照片向量化全部完成: pet_id={}, vector_count={}", pet_id, len(written_ids))
    return written_ids


def delete_pet_vectors(pet_id: int) -> None:
    """
    清理宠物全部照片向量（删除宠物时由 Java 触发）

    Args:
        pet_id: 宠物 ID

    Raises:
        RuntimeError: 向量库删除失败
    """
    logger.info("开始清理宠物向量: pet_id={}", pet_id)

    qdrant = get_client()
    qdrant.delete(
        collection_name=COLLECTION_PET_PROFILES,
        points_selector=Filter(
            must=[
                FieldCondition(
                    key="pet_id",
                    match=MatchValue(value=pet_id)
                )
            ]
        )
    )

    logger.info("宠物向量清理完成: pet_id={}", pet_id)
