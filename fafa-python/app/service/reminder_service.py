"""
提醒服务
"""
from typing import List, Optional
from sqlalchemy.ext.asyncio import AsyncSession
from app.repository.reminder_repository import ReminderRepository


class ReminderService:
    """提醒服务"""
    
    def __init__(self, db: AsyncSession):
        self.db = db
        self.reminder_repository = ReminderRepository()
    
    async def get_reminder_by_id(self, reminder_id: int) -> Optional[dict]:
        """根据ID获取提醒"""
        return await self.reminder_repository.get_reminder_by_id(reminder_id)
    
    async def list_reminders_by_pet(self, pet_id: int, status: Optional[str] = None, limit: int = 20) -> List[dict]:
        """根据宠物ID获取提醒列表"""
        return await self.reminder_repository.list_reminders_by_pet(pet_id, status, limit)
    
    async def list_reminders_by_user(self, user_id: int, status: Optional[str] = None, limit: int = 50) -> List[dict]:
        """根据用户ID获取提醒列表"""
        return await self.reminder_repository.list_reminders_by_user(user_id, status, limit)
