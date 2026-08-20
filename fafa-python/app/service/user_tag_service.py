"""
用户标签服务层

负责用户自定义标签的 CRUD 操作
"""
from typing import List, Dict, Any, Optional
from sqlalchemy import text
from sqlalchemy.ext.asyncio import AsyncSession
from datetime import datetime
from loguru import logger

from app.core.database import get_async_session


class UserTagService:
    """用户标签服务"""
    
    def __init__(self, db: AsyncSession = None):
        self.db = db
    
    async def create_tag(
        self,
        user_id: int,
        name: str,
        color: Optional[str] = None,
        icon: Optional[str] = None
    ) -> int:
        """
        创建用户标签
        
        Args:
            user_id: 用户ID
            name: 标签名称
            color: 标签颜色
            icon: 标签图标
            
        Returns:
            标签ID
        """
        try:
            logger.info(f"创建用户标签: userId={user_id}, name={name}")
            
            async with get_async_session() as session:
                sql = text("""
                    INSERT INTO user_tag (user_id, name, color, icon, use_count, created_at, updated_at)
                    VALUES (:user_id, :name, :color, :icon, 0, NOW(), NOW())
                """)
                
                result = await session.execute(sql, {
                    'user_id': user_id,
                    'name': name,
                    'color': color,
                    'icon': icon
                })
                await session.commit()
                
                tag_id = result.lastrowid
                logger.info(f"用户标签创建成功: tagId={tag_id}")
                return tag_id
                
        except Exception as e:
            logger.error(f"创建用户标签失败: userId={user_id}, name={name}, error={e}")
            raise
    
    async def get_user_tags(
        self,
        user_id: int,
        keyword: Optional[str] = None,
        limit: int = 100
    ) -> List[Dict[str, Any]]:
        """
        获取用户标签列表
        
        Args:
            user_id: 用户ID
            keyword: 搜索关键词
            limit: 返回数量
            
        Returns:
            标签列表
        """
        try:
            async with get_async_session() as session:
                if keyword:
                    sql = text("""
                        SELECT id, user_id, name, color, icon, use_count, created_at, updated_at
                        FROM user_tag
                        WHERE user_id = :user_id AND name LIKE :keyword
                        ORDER BY use_count DESC, created_at DESC
                        LIMIT :limit
                    """)
                    result = await session.execute(sql, {
                        'user_id': user_id,
                        'keyword': f'%{keyword}%',
                        'limit': limit
                    })
                else:
                    sql = text("""
                        SELECT id, user_id, name, color, icon, use_count, created_at, updated_at
                        FROM user_tag
                        WHERE user_id = :user_id
                        ORDER BY use_count DESC, created_at DESC
                        LIMIT :limit
                    """)
                    result = await session.execute(sql, {
                        'user_id': user_id,
                        'limit': limit
                    })
                
                rows = result.fetchall()
                
                tags = []
                for row in rows:
                    tags.append({
                        'id': row[0],
                        'user_id': row[1],
                        'name': row[2],
                        'color': row[3],
                        'icon': row[4],
                        'use_count': row[5],
                        'created_at': row[6],
                        'updated_at': row[7]
                    })
                
                logger.info(f"获取用户标签列表: userId={user_id}, count={len(tags)}")
                return tags
                
        except Exception as e:
            logger.error(f"获取用户标签列表失败: userId={user_id}, error={e}")
            raise
    
    async def get_popular_tags(
        self,
        user_id: int,
        limit: int = 20
    ) -> List[Dict[str, Any]]:
        """
        获取热门标签 (按使用次数排序)
        
        Args:
            user_id: 用户ID
            limit: 返回数量
            
        Returns:
            热门标签列表
        """
        try:
            async with get_async_session() as session:
                sql = text("""
                    SELECT id, name, color, icon, use_count
                    FROM user_tag
                    WHERE user_id = :user_id AND use_count > 0
                    ORDER BY use_count DESC, updated_at DESC
                    LIMIT :limit
                """)
                
                result = await session.execute(sql, {
                    'user_id': user_id,
                    'limit': limit
                })
                
                rows = result.fetchall()
                
                tags = []
                for row in rows:
                    tags.append({
                        'id': row[0],
                        'name': row[1],
                        'color': row[2],
                        'icon': row[3],
                        'use_count': row[4]
                    })
                
                logger.info(f"获取热门标签: userId={user_id}, count={len(tags)}")
                return tags
                
        except Exception as e:
            logger.error(f"获取热门标签失败: userId={user_id}, error={e}")
            raise
    
    async def update_tag(
        self,
        tag_id: int,
        user_id: int,
        name: Optional[str] = None,
        color: Optional[str] = None,
        icon: Optional[str] = None
    ):
        """
        更新标签
        
        Args:
            tag_id: 标签ID
            user_id: 用户ID
            name: 标签名称
            color: 标签颜色
            icon: 标签图标
        """
        try:
            logger.info(f"更新用户标签: tagId={tag_id}, userId={user_id}")
            
            # 构建动态 SQL
            update_fields = []
            params = {'tag_id': tag_id, 'user_id': user_id}
            
            if name is not None:
                update_fields.append("name = :name")
                params['name'] = name
            
            if color is not None:
                update_fields.append("color = :color")
                params['color'] = color
            
            if icon is not None:
                update_fields.append("icon = :icon")
                params['icon'] = icon
            
            if not update_fields:
                logger.warning(f"没有需要更新的字段: tagId={tag_id}")
                return
            
            update_fields.append("updated_at = NOW()")
            
            async with get_async_session() as session:
                sql = text(f"""
                    UPDATE user_tag
                    SET {', '.join(update_fields)}
                    WHERE id = :tag_id AND user_id = :user_id
                """)
                
                await session.execute(sql, params)
                await session.commit()
                
            logger.info(f"用户标签更新成功: tagId={tag_id}")
            
        except Exception as e:
            logger.error(f"更新用户标签失败: tagId={tag_id}, error={e}")
            raise
    
    async def delete_tag(self, tag_id: int, user_id: int):
        """
        删除标签
        
        Args:
            tag_id: 标签ID
            user_id: 用户ID
        """
        try:
            logger.info(f"删除用户标签: tagId={tag_id}, userId={user_id}")
            
            async with get_async_session() as session:
                sql = text("""
                    DELETE FROM user_tag
                    WHERE id = :tag_id AND user_id = :user_id
                """)
                
                await session.execute(sql, {
                    'tag_id': tag_id,
                    'user_id': user_id
                })
                await session.commit()
                
            logger.info(f"用户标签删除成功: tagId={tag_id}")
            
        except Exception as e:
            logger.error(f"删除用户标签失败: tagId={tag_id}, error={e}")
            raise
    
    async def batch_delete_tags(self, tag_ids: List[int], user_id: int):
        """
        批量删除标签
        
        Args:
            tag_ids: 标签ID列表
            user_id: 用户ID
        """
        try:
            logger.info(f"批量删除用户标签: userId={user_id}, count={len(tag_ids)}")
            
            if not tag_ids:
                return
            
            async with get_async_session() as session:
                # 使用 IN 子句批量删除
                placeholders = ','.join([f':tag_id_{i}' for i in range(len(tag_ids))])
                sql = text(f"""
                    DELETE FROM user_tag
                    WHERE id IN ({placeholders}) AND user_id = :user_id
                """)
                
                params = {'user_id': user_id}
                for i, tag_id in enumerate(tag_ids):
                    params[f'tag_id_{i}'] = tag_id
                
                await session.execute(sql, params)
                await session.commit()
                
            logger.info(f"批量删除用户标签成功: count={len(tag_ids)}")
            
        except Exception as e:
            logger.error(f"批量删除用户标签失败: error={e}")
            raise
    
    async def increment_use_count(self, tag_id: int, user_id: int):
        """
        增加标签使用次数
        
        Args:
            tag_id: 标签ID
            user_id: 用户ID
        """
        try:
            async with get_async_session() as session:
                sql = text("""
                    UPDATE user_tag
                    SET use_count = use_count + 1, updated_at = NOW()
                    WHERE id = :tag_id AND user_id = :user_id
                """)
                
                await session.execute(sql, {
                    'tag_id': tag_id,
                    'user_id': user_id
                })
                await session.commit()
                
        except Exception as e:
            logger.error(f"增加标签使用次数失败: tagId={tag_id}, error={e}")
            raise
    
    async def batch_increment_use_count(self, tag_names: List[str], user_id: int):
        """
        批量增加标签使用次数 (根据名称)
        
        Args:
            tag_names: 标签名称列表
            user_id: 用户ID
        """
        try:
            if not tag_names:
                return
            
            async with get_async_session() as session:
                placeholders = ','.join([f':tag_name_{i}' for i in range(len(tag_names))])
                sql = text(f"""
                    UPDATE user_tag
                    SET use_count = use_count + 1, updated_at = NOW()
                    WHERE name IN ({placeholders}) AND user_id = :user_id
                """)
                
                params = {'user_id': user_id}
                for i, tag_name in enumerate(tag_names):
                    params[f'tag_name_{i}'] = tag_name
                
                await session.execute(sql, params)
                await session.commit()
                
            logger.info(f"批量增加标签使用次数: userId={user_id}, count={len(tag_names)}")
            
        except Exception as e:
            logger.error(f"批量增加标签使用次数失败: error={e}")
            raise
    
    async def get_tag_count(self, user_id: int) -> int:
        """
        获取用户标签总数
        
        Args:
            user_id: 用户ID
            
        Returns:
            标签总数
        """
        try:
            async with get_async_session() as session:
                sql = text("""
                    SELECT COUNT(*) FROM user_tag
                    WHERE user_id = :user_id
                """)
                
                result = await session.execute(sql, {'user_id': user_id})
                count = result.scalar()
                
                return count or 0
                
        except Exception as e:
            logger.error(f"获取用户标签总数失败: userId={user_id}, error={e}")
            raise


# 全局实例
user_tag_service = UserTagService()
