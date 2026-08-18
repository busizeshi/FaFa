"""
照片仓储
"""
from typing import List, Optional
from sqlalchemy import select, and_
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_async_session


class PhotoRepository:
    """照片仓储"""
    
    async def get_photo_by_id(self, photo_id: int) -> Optional[dict]:
        """根据ID查询照片"""
        async with get_async_session() as session:
            from sqlalchemy import text
            
            sql = text("""
                SELECT id, pet_id, user_id, url, thumbnail_url, taken_at,
                       description, ai_tags, ai_description, embedding_id
                FROM photo
                WHERE id = :photo_id
            """)
            
            result = await session.execute(sql, {'photo_id': photo_id})
            row = result.fetchone()
            
            if row:
                return {
                    'id': row[0],
                    'pet_id': row[1],
                    'user_id': row[2],
                    'url': row[3],
                    'thumbnail_url': row[4],
                    'taken_at': row[5],
                    'description': row[6],
                    'ai_tags': row[7],
                    'ai_description': row[8],
                    'embedding_id': row[9]
                }
            
            return None
    
    async def list_photos_by_pet(self, pet_id: int, limit: int = 20) -> List[dict]:
        """根据宠物ID查询照片列表"""
        async with get_async_session() as session:
            from sqlalchemy import text
            
            sql = text("""
                SELECT id, pet_id, url, thumbnail_url, taken_at,
                       description, ai_tags, ai_description
                FROM photo
                WHERE pet_id = :pet_id
                ORDER BY taken_at DESC, created_at DESC
                LIMIT :limit
            """)
            
            result = await session.execute(sql, {'pet_id': pet_id, 'limit': limit})
            rows = result.fetchall()
            
            photos = []
            for row in rows:
                photos.append({
                    'id': row[0],
                    'pet_id': row[1],
                    'url': row[2],
                    'thumbnail_url': row[3],
                    'taken_at': row[4],
                    'description': row[5],
                    'ai_tags': row[6],
                    'ai_description': row[7]
                })
            
            return photos
