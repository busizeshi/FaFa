"""
宠物路由：三视图向量化与向量清理（契约见技术文档 7.3）

Java 上传照片到对象存储后，通过 AiTaskDispatcher 分发到本服务：
- dev：HTTP POST /api/pets/profile-photos
- prod：RocketMQ 消费者拉取后调用同一逻辑
"""

from typing import List, Optional

from fastapi import APIRouter, HTTPException, status
from loguru import logger
from pydantic import BaseModel, Field

router = APIRouter(prefix="/api/pets", tags=["pets"])


class PetProfilePhotosRequest(BaseModel):
    """三视图向量化请求（Java → Python）

    avatar_url / front_url / side_url 可部分为空字符串，
    表示本次只更新部分照片（如只换头像），Python 端按 view 分别 upsert，幂等。
    """

    pet_id: int = Field(..., description="宠物 ID")
    user_id: int = Field(..., description="用户 ID")
    avatar_url: str = Field("", description="头像 URL（空表示本次不更新头像）")
    front_url: str = Field("", description="正面照 URL")
    side_url: str = Field("", description="侧面照 URL")


class PetProfilePhotosResponse(BaseModel):
    """三视图向量化结果"""

    pet_id: int
    vector_count: int
    message_ids: List[str] = []


@router.post("/profile-photos", response_model=PetProfilePhotosResponse)
async def embed_profile_photos(request: PetProfilePhotosRequest) -> PetProfilePhotosResponse:
    """
    宠物照片向量化

    使用 qwen3-vl-embedding 对提供的照片分别生成向量，
    存入 Qdrant fafa_pet_profiles 集合（payload 含 user_id/pet_id/view/photo_url）。
    point_id 由 {pet_id}_{view} 经 uuid5 确定性生成，保证 upsert 幂等。
    """
    from app.service import pet_vector_service

    logger.info("收到宠物照片向量化请求: pet_id={}, user_id={}", request.pet_id, request.user_id)

    try:
        message_ids = await pet_vector_service.embed_profile_photos(
            pet_id=request.pet_id,
            user_id=request.user_id,
            avatar_url=request.avatar_url,
            front_url=request.front_url,
            side_url=request.side_url,
        )
        return PetProfilePhotosResponse(
            pet_id=request.pet_id,
            vector_count=len(message_ids),
            message_ids=message_ids
        )

    except Exception as e:
        logger.error("宠物照片向量化失败: pet_id={}, error={}", request.pet_id, str(e))
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"向量化失败: {str(e)}"
        )


@router.delete("/{pet_id}/profile-vectors")
def delete_profile_vectors(pet_id: int) -> dict:
    """
    清理宠物全部照片向量（宠物照片更新/删除宠物时由 Java 触发）
    """
    from app.service import pet_vector_service

    logger.info("收到宠物向量清理请求: pet_id={}", pet_id)

    try:
        pet_vector_service.delete_pet_vectors(pet_id)
        return {"pet_id": pet_id, "status": "deleted"}

    except Exception as e:
        logger.error("宠物向量清理失败: pet_id={}, error={}", pet_id, str(e))
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"向量清理失败: {str(e)}"
        )
