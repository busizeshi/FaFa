"""
提醒仓储
"""
from typing import List, Optional
from sqlalchemy.ext.asyncio import AsyncSession
from app.core.database import get_async_session


class ReminderRepository:
    """提醒仓储"""
    
    async def get_reminder_by_id(self, reminder_id: int) -> Optional[dict]:
        """根据ID查询提醒"""
        async with get_async_session() as session:
            from sqlalchemy import text
            
            sql = text("""
                SELECT id, pet_id, user_id, title, reminder_type, remind_time,
                       repeat_type, repeat_config, advance_minutes, description,
                       status, completed_at, completion_note, completion_images,
                       is_notified, created_at, updated_at
                FROM reminder
                WHERE id = :reminder_id
            """)
            
            result = await session.execute(sql, {'reminder_id': reminder_id})
            row = result.fetchone()
            
            if row:
                return {
                    'id': row[0],
                    'pet_id': row[1],
                    'user_id': row[2],
                    'title': row[3],
                    'reminder_type': row[4],
                    'remind_time': row[5],
                    'repeat_type': row[6],
                    'repeat_config': row[7],
                    'advance_minutes': row[8],
                    'description': row[9],
                    'status': row[10],
                    'completed_at': row[11],
                    'completion_note': row[12],
                    'completion_images': row[13],
                    'is_notified': row[14],
                    'created_at': row[15],
                    'updated_at': row[16]
                }
            
            return None
    
    async def list_reminders_by_pet(self, pet_id: int, status: Optional[str] = None, limit: int = 20) -> List[dict]:
        """根据宠物ID查询提醒列表"""
        async with get_async_session() as session:
            from sqlalchemy import text
            
            if status:
                sql = text("""
                    SELECT id, pet_id, user_id, title, reminder_type, remind_time,
                           repeat_type, status, description
                    FROM reminder
                    WHERE pet_id = :pet_id AND status = :status
                    ORDER BY remind_time ASC
                    LIMIT :limit
                """)
                result = await session.execute(sql, {'pet_id': pet_id, 'status': status, 'limit': limit})
            else:
                sql = text("""
                    SELECT id, pet_id, user_id, title, reminder_type, remind_time,
                           repeat_type, status, description
                    FROM reminder
                    WHERE pet_id = :pet_id
                    ORDER BY remind_time ASC
                    LIMIT :limit
                """)
                result = await session.execute(sql, {'pet_id': pet_id, 'limit': limit})
            
            rows = result.fetchall()
            
            reminders = []
            for row in rows:
                reminders.append({
                    'id': row[0],
                    'pet_id': row[1],
                    'user_id': row[2],
                    'title': row[3],
                    'reminder_type': row[4],
                    'remind_time': row[5],
                    'repeat_type': row[6],
                    'status': row[7],
                    'description': row[8]
                })
            
            return reminders
    
    async def list_reminders_by_user(self, user_id: int, status: Optional[str] = None, limit: int = 50) -> List[dict]:
        """根据用户ID查询提醒列表"""
        async with get_async_session() as session:
            from sqlalchemy import text
            
            if status:
                sql = text("""
                    SELECT id, pet_id, user_id, title, reminder_type, remind_time,
                           repeat_type, status, description
                    FROM reminder
                    WHERE user_id = :user_id AND status = :status
                    ORDER BY remind_time ASC
                    LIMIT :limit
                """)
                result = await session.execute(sql, {'user_id': user_id, 'status': status, 'limit': limit})
            else:
                sql = text("""
                    SELECT id, pet_id, user_id, title, reminder_type, remind_time,
                           repeat_type, status, description
                    FROM reminder
                    WHERE user_id = :user_id
                    ORDER BY remind_time ASC
                    LIMIT :limit
                """)
                result = await session.execute(sql, {'user_id': user_id, 'limit': limit})
            
            rows = result.fetchall()
            
            reminders = []
            for row in rows:
                reminders.append({
                    'id': row[0],
                    'pet_id': row[1],
                    'user_id': row[2],
                    'title': row[3],
                    'reminder_type': row[4],
                    'remind_time': row[5],
                    'repeat_type': row[6],
                    'status': row[7],
                    'description': row[8]
                })
            
            return reminders
