"""
提醒数据查询路由
"""
from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.ext.asyncio import AsyncSession
from app.core.database import get_db
from app.service.reminder_service import ReminderService
from loguru import logger
from typing import Optional

router = APIRouter(prefix="/reminders", tags=["提醒数据"])


@router.get("/{reminder_id}")
async def get_reminder(
    reminder_id: int,
    db: AsyncSession = Depends(get_db)
):
    """
    获取提醒详情（供 AI 使用）
    """
    try:
        reminder_service = ReminderService(db)
        reminder = await reminder_service.get_reminder_by_id(reminder_id)
        
        if not reminder:
            raise HTTPException(status_code=404, detail="提醒不存在")
        
        return {
            "id": reminder["id"],
            "pet_id": reminder["pet_id"],
            "user_id": reminder["user_id"],
            "title": reminder["title"],
            "reminder_type": reminder["reminder_type"],
            "remind_time": str(reminder["remind_time"]) if reminder["remind_time"] else None,
            "repeat_type": reminder["repeat_type"],
            "status": reminder["status"],
            "description": reminder["description"],
        }
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"获取提醒详情失败: {e}")
        raise HTTPException(status_code=500, detail="服务器内部错误")


@router.get("/pet/{pet_id}")
async def get_pet_reminders(
    pet_id: int,
    status: Optional[str] = Query(None, description="提醒状态：pending, completed, cancelled, expired"),
    limit: int = Query(20, ge=1, le=100),
    db: AsyncSession = Depends(get_db)
):
    """
    获取宠物的提醒列表（供 AI 使用）
    """
    try:
        reminder_service = ReminderService(db)
        reminders = await reminder_service.list_reminders_by_pet(pet_id, status, limit)
        
        return {
            "count": len(reminders),
            "reminders": [
                {
                    "id": reminder["id"],
                    "title": reminder["title"],
                    "reminder_type": reminder["reminder_type"],
                    "remind_time": str(reminder["remind_time"]) if reminder["remind_time"] else None,
                    "repeat_type": reminder["repeat_type"],
                    "status": reminder["status"],
                }
                for reminder in reminders
            ]
        }
    except Exception as e:
        logger.error(f"获取宠物提醒列表失败: {e}")
        raise HTTPException(status_code=500, detail="服务器内部错误")


@router.get("/user/{user_id}")
async def get_user_reminders(
    user_id: int,
    status: Optional[str] = Query(None, description="提醒状态：pending, completed, cancelled, expired"),
    limit: int = Query(50, ge=1, le=200),
    db: AsyncSession = Depends(get_db)
):
    """
    获取用户的提醒列表（供 AI 使用）
    """
    try:
        reminder_service = ReminderService(db)
        reminders = await reminder_service.list_reminders_by_user(user_id, status, limit)
        
        return {
            "count": len(reminders),
            "reminders": [
                {
                    "id": reminder["id"],
                    "pet_id": reminder["pet_id"],
                    "title": reminder["title"],
                    "reminder_type": reminder["reminder_type"],
                    "remind_time": str(reminder["remind_time"]) if reminder["remind_time"] else None,
                    "repeat_type": reminder["repeat_type"],
                    "status": reminder["status"],
                }
                for reminder in reminders
            ]
        }
    except Exception as e:
        logger.error(f"获取用户提醒列表失败: {e}")
        raise HTTPException(status_code=500, detail="服务器内部错误")
