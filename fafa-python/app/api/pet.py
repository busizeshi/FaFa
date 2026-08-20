"""
宠物数据查询路由 (基于 qwen3-vl-embedding)
"""
from typing import Optional
from fastapi import APIRouter, Depends, HTTPException, File, UploadFile, Form
from sqlalchemy.ext.asyncio import AsyncSession
from pydantic import BaseModel
from app.core.database import get_db
from app.service.pet_service import PetService, pet_service
from loguru import logger

router = APIRouter(prefix="/pets", tags=["宠物数据"])


class UploadProfilePhotosRequest(BaseModel):
    """上传宠物三视图请求"""
    pet_id: int
    user_id: int
    front_view_url: Optional[str] = None
    side_view_url: Optional[str] = None
    top_view_url: Optional[str] = None


@router.get("/{pet_id}")
async def get_pet(
    pet_id: int,
    db: AsyncSession = Depends(get_db)
):
    """
    获取宠物信息（供 AI 使用）
    """
    try:
        pet_service_instance = PetService(db)
        pet = await pet_service_instance.get_pet_by_id(pet_id)
        
        if not pet:
            raise HTTPException(status_code=404, detail="宠物不存在")
        
        return {
            "id": pet["id"],
            "name": pet["name"],
            "species": pet["species"],
            "breed": pet["breed"],
            "gender": pet["gender"],
            "birth_date": str(pet["birth_date"]) if pet["birth_date"] else None,
            "weight": float(pet["weight"]) if pet["weight"] else None,
            "age_in_months": pet.get("age_in_months"),
            "front_view_url": pet.get("front_view_url"),
            "side_view_url": pet.get("side_view_url"),
            "top_view_url": pet.get("top_view_url"),
            "profile_embedding_id": pet.get("profile_embedding_id"),
        }
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"获取宠物信息失败: {e}")
        raise HTTPException(status_code=500, detail="服务器内部错误")


@router.get("/user/{user_id}")
async def get_user_pets(
    user_id: int,
    db: AsyncSession = Depends(get_db)
):
    """
    获取用户的宠物列表（供 AI 使用）
    """
    try:
        pet_service_instance = PetService(db)
        pets = await pet_service_instance.get_pets_by_user(user_id)
        
        return {
            "count": len(pets),
            "pets": [
                {
                    "id": pet["id"],
                    "name": pet["name"],
                    "species": pet["species"],
                    "age_in_months": pet.get("age_in_months"),
                    "front_view_url": pet.get("front_view_url"),
                    "side_view_url": pet.get("side_view_url"),
                    "top_view_url": pet.get("top_view_url"),
                }
                for pet in pets
            ]
        }
    except Exception as e:
        logger.error(f"获取用户宠物列表失败: {e}")
        raise HTTPException(status_code=500, detail="服务器内部错误")


@router.post("/profile-photos")
async def upload_profile_photos(request: UploadProfilePhotosRequest):
    """
    上传宠物三视图照片（正面、侧面、俯视）
    用于后续自动识别宠物
    """
    try:
        logger.info(f"接收宠物三视图上传: petId={request.pet_id}, userId={request.user_id}")
        
        await pet_service.upload_pet_profile_photos(
            pet_id=request.pet_id,
            user_id=request.user_id,
            front_view_url=request.front_view_url,
            side_view_url=request.side_view_url,
            top_view_url=request.top_view_url
        )
        
        return {
            "message": "宠物三视图上传成功",
            "pet_id": request.pet_id
        }
        
    except Exception as e:
        logger.error(f"上传宠物三视图失败: petId={request.pet_id}, error={e}")
        raise HTTPException(status_code=500, detail=f"上传失败: {str(e)}")


@router.delete("/{pet_id}/profile-vectors")
async def delete_pet_profile_vectors(pet_id: int):
    """
    删除宠物的三视图向量
    """
    try:
        logger.info(f"删除宠物三视图向量: petId={pet_id}")
        
        await pet_service.delete_pet_profile_vectors(pet_id)
        
        return {
            "message": "宠物三视图向量删除成功",
            "pet_id": pet_id
        }
        
    except Exception as e:
        logger.error(f"删除宠物三视图向量失败: petId={pet_id}, error={e}")
        raise HTTPException(status_code=500, detail=f"删除失败: {str(e)}")
