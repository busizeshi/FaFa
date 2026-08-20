"""
宠物服务层 (基于 qwen3-vl-embedding)

负责宠物三视图照片的向量化和管理
"""
from typing import List, Dict, Any, Optional
from sqlalchemy import text
from sqlalchemy.ext.asyncio import AsyncSession
from datetime import date
from loguru import logger

from app.core.config import settings
from app.core.database import get_async_session
from app.core.qdrant import qdrant_client
from app.core.dashscope_client import generate_image_embedding, batch_generate_embeddings


class PetService:
    """宠物服务"""
    
    def __init__(self, db: AsyncSession = None):
        self.db = db
    
    async def upload_pet_profile_photos(
        self,
        pet_id: int,
        user_id: int,
        front_view_url: Optional[str] = None,
        side_view_url: Optional[str] = None,
        top_view_url: Optional[str] = None
    ):
        """
        上传宠物三视图照片并生成向量
        
        Args:
            pet_id: 宠物ID
            user_id: 用户ID
            front_view_url: 正面照URL
            side_view_url: 侧面照URL
            top_view_url: 俯视照URL
        """
        try:
            logger.info(f"上传宠物三视图: petId={pet_id}, userId={user_id}")
            
            # 收集需要生成向量的图片
            image_urls = []
            view_types = []
            
            if front_view_url:
                image_urls.append(front_view_url)
                view_types.append('front')
            
            if side_view_url:
                image_urls.append(side_view_url)
                view_types.append('side')
            
            if top_view_url:
                image_urls.append(top_view_url)
                view_types.append('top')
            
            if not image_urls:
                logger.warning(f"没有提供任何三视图照片: petId={pet_id}")
                return
            
            # 批量生成图片向量
            inputs = [{"image": url} for url in image_urls]
            embeddings = await batch_generate_embeddings(inputs)
            
            # 保存到 Qdrant (pet_profiles 集合)
            points = []
            for i, (view_type, embedding) in enumerate(zip(view_types, embeddings)):
                embedding_id = f"pet_{pet_id}_{view_type}"
                points.append({
                    'id': embedding_id,
                    'vector': embedding,
                    'payload': {
                        'pet_id': pet_id,
                        'user_id': user_id,
                        'view_type': view_type,
                        'image_url': image_urls[i]
                    }
                })
            
            qdrant_client.upsert(
                collection_name=settings.QDRANT_COLLECTION_PET_PROFILES,
                points=points
            )
            
            # 更新 MySQL pet 表 (保存 URL 和第一个向量ID)
            profile_embedding_id = points[0]['id'] if points else None
            await self._update_pet_profile_photos(
                pet_id=pet_id,
                front_view_url=front_view_url,
                side_view_url=side_view_url,
                top_view_url=top_view_url,
                profile_embedding_id=profile_embedding_id
            )
            
            logger.info(f"宠物三视图上传完成: petId={pet_id}, embeddingCount={len(points)}")
            
        except Exception as e:
            logger.error(f"上传宠物三视图失败: petId={pet_id}, error={e}", exc_info=True)
            raise
    
    async def _update_pet_profile_photos(
        self,
        pet_id: int,
        front_view_url: Optional[str],
        side_view_url: Optional[str],
        top_view_url: Optional[str],
        profile_embedding_id: Optional[str]
    ):
        """更新宠物三视图照片URL"""
        try:
            async with get_async_session() as session:
                sql = text("""
                    UPDATE pet 
                    SET 
                        front_view_url = :front_view_url,
                        side_view_url = :side_view_url,
                        top_view_url = :top_view_url,
                        profile_embedding_id = :profile_embedding_id
                    WHERE id = :pet_id
                """)
                
                await session.execute(sql, {
                    'pet_id': pet_id,
                    'front_view_url': front_view_url,
                    'side_view_url': side_view_url,
                    'top_view_url': top_view_url,
                    'profile_embedding_id': profile_embedding_id
                })
                await session.commit()
                
            logger.info(f"宠物三视图URL已更新: petId={pet_id}")
            
        except Exception as e:
            logger.error(f"更新宠物三视图URL失败: petId={pet_id}, error={e}")
            raise
    
    async def delete_pet_profile_vectors(self, pet_id: int):
        """
        删除宠物的三视图向量
        
        Args:
            pet_id: 宠物ID
        """
        try:
            # 删除 Qdrant 中的向量
            embedding_ids = [
                f"pet_{pet_id}_front",
                f"pet_{pet_id}_side",
                f"pet_{pet_id}_top"
            ]
            
            qdrant_client.delete(
                collection_name=settings.QDRANT_COLLECTION_PET_PROFILES,
                points_selector=embedding_ids
            )
            
            logger.info(f"宠物三视图向量已删除: petId={pet_id}")
            
        except Exception as e:
            logger.error(f"删除宠物三视图向量失败: petId={pet_id}, error={e}")
            raise
    
    async def get_pet_by_id(self, pet_id: int) -> dict:
        """
        根据 ID 查询宠物
        """
        query = text("""
            SELECT 
                id, user_id, name, avatar, species, breed, gender, 
                birth_date, adopt_date, weight, is_neutered, 
                coat_color, remarks, status, sort_order,
                front_view_url, side_view_url, top_view_url, profile_embedding_id,
                TIMESTAMPDIFF(MONTH, birth_date, CURDATE()) as age_in_months
            FROM pet 
            WHERE id = :pet_id AND status = 1
        """)
        
        result = await self.db.execute(query, {"pet_id": pet_id})
        row = result.fetchone()
        
        if not row:
            return None
        
        return {
            "id": row[0],
            "user_id": row[1],
            "name": row[2],
            "avatar": row[3],
            "species": row[4],
            "breed": row[5],
            "gender": row[6],
            "birth_date": row[7],
            "adopt_date": row[8],
            "weight": row[9],
            "is_neutered": row[10],
            "coat_color": row[11],
            "remarks": row[12],
            "status": row[13],
            "sort_order": row[14],
            "front_view_url": row[15],
            "side_view_url": row[16],
            "top_view_url": row[17],
            "profile_embedding_id": row[18],
            "age_in_months": row[19],
        }
    
    async def get_pets_by_user(self, user_id: int) -> list:
        """
        根据用户 ID 查询宠物列表
        """
        query = text("""
            SELECT 
                id, name, species, breed, gender, birth_date, weight,
                front_view_url, side_view_url, top_view_url,
                TIMESTAMPDIFF(MONTH, birth_date, CURDATE()) as age_in_months
            FROM pet 
            WHERE user_id = :user_id AND status = 1
            ORDER BY sort_order, id
        """)
        
        result = await self.db.execute(query, {"user_id": user_id})
        rows = result.fetchall()
        
        pets = []
        for row in rows:
            pets.append({
                "id": row[0],
                "name": row[1],
                "species": row[2],
                "breed": row[3],
                "gender": row[4],
                "birth_date": row[5],
                "weight": row[6],
                "front_view_url": row[7],
                "side_view_url": row[8],
                "top_view_url": row[9],
                "age_in_months": row[10],
            })
        
        return pets


# 全局实例
pet_service = PetService()
