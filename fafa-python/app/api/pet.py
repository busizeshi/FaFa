"""
宠物数据查询路由
"""
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession
from app.core.database import get_db
from app.service.pet_service import PetService
from loguru import logger

router = APIRouter(prefix="/pets", tags=["宠物数据"])


@router.get("/{pet_id}")
async def get_pet(
    pet_id: int,
    db: AsyncSession = Depends(get_db)
):
    """
    获取宠物信息（供 AI 使用）
    """
    try:
        pet_service = PetService(db)
        pet = await pet_service.get_pet_by_id(pet_id)
        
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
        pet_service = PetService(db)
        pets = await pet_service.get_pets_by_user(user_id)
        
        return {
            "count": len(pets),
            "pets": [
                {
                    "id": pet["id"],
                    "name": pet["name"],
                    "species": pet["species"],
                    "age_in_months": pet.get("age_in_months"),
                }
                for pet in pets
            ]
        }
    except Exception as e:
        logger.error(f"获取用户宠物列表失败: {e}")
        raise HTTPException(status_code=500, detail="服务器内部错误")
