"""
照片/视频分析服务 (基于 qwen3-vl-embedding)
"""
import asyncio
from typing import List, Dict, Any, Optional
from loguru import logger

from app.core.config import settings
from app.core.database import get_async_session
from app.core.qdrant import qdrant_client, point_id_from_embedding_id
from app.core.dashscope_client import (
    generate_image_embedding,
    generate_video_embedding,
    generate_text_embedding
)
from app.repository.photo_repository import PhotoRepository
from sqlalchemy import text


class PhotoAnalysisService:
    """照片/视频分析服务"""
    
    def __init__(self):
        self.photo_repo = PhotoRepository()
    
    async def analyze_media(
        self,
        photo_id: int,
        user_id: int,
        pet_id: Optional[int],
        url: str,
        media_type: str,
        tags: Optional[List[str]] = None,
        taken_at: Optional[str] = None
    ):
        """
        分析照片或视频 (使用 qwen3-vl-embedding 直接生成向量)

        Args:
            photo_id: 照片/视频ID
            user_id: 用户ID
            pet_id: 宠物ID (可选)
            url: 照片/视频URL
            media_type: 媒体类型 (image 或 video)
            tags: 用户标签
            taken_at: 拍摄时间 (可选，用于时间范围过滤搜索)
        """
        try:
            logger.info(f"开始分析媒体: photoId={photo_id}, mediaType={media_type}, petId={pet_id}")
            
            # 1. 使用 qwen3-vl-embedding 直接生成向量
            if media_type == 'video':
                embedding = await generate_video_embedding(url)
            else:
                embedding = await generate_image_embedding(url)
            
            if not embedding:
                logger.error(f"生成向量失败: photoId={photo_id}")
                return
            
            # 2. 如果启用宠物识别且 petId 为空,则自动识别宠物
            recognized_pet_ids = []
            recognition_confidence = None
            auto_recognized = False
            
            if settings.PET_RECOGNITION_ENABLED and not pet_id and media_type == 'image':
                recognition_result = await self.recognize_pet(user_id, embedding)
                if recognition_result:
                    recognized_pet_ids = recognition_result['pet_ids']
                    recognition_confidence = recognition_result['confidence']
                    auto_recognized = True
                    
                    if recognized_pet_ids and recognition_confidence >= settings.PET_RECOGNITION_MIN_CONFIDENCE:
                        pet_id = recognized_pet_ids[0]
                        logger.info(f"自动识别到宠物: petId={pet_id}, confidence={recognition_confidence}")
            
            # 3. 存入 Qdrant 向量库
            embedding_id = f"media_{photo_id}"
            await self._save_to_qdrant(
                embedding_id=embedding_id,
                vector=embedding,
                payload={
                    'photo_id': photo_id,
                    'user_id': user_id,
                    'pet_id': pet_id,
                    'url': url,
                    'media_type': media_type,
                    'taken_at': taken_at,
                    'tags': tags or [],
                    'auto_recognized': auto_recognized,
                    'recognized_pet_ids': recognized_pet_ids
                }
            )
            
            # 4. 更新 MySQL photo 表
            await self._update_photo_ai_result(
                photo_id=photo_id,
                pet_id=pet_id,
                embedding_id=embedding_id,
                auto_recognized=auto_recognized,
                recognition_confidence=recognition_confidence,
                recognized_pet_ids=recognized_pet_ids
            )
            
            logger.info(f"媒体分析完成: photoId={photo_id}, embeddingId={embedding_id}, autoRecognized={auto_recognized}")
            
        except Exception as e:
            logger.error(f"媒体分析失败: photoId={photo_id}, error={e}", exc_info=True)
            raise
    
    async def recognize_pet(self, user_id: int, query_embedding: List[float]) -> Optional[Dict[str, Any]]:
        """
        通过图片向量识别宠物 (图片->图片的相似度搜索)
        
        Args:
            user_id: 用户ID
            query_embedding: 查询向量
            
        Returns:
            识别结果: {'pet_ids': [1, 2], 'confidence': 0.85}
        """
        try:
            # 从宠物三视图向量库中搜索最相似的宠物
            search_result = qdrant_client.search(
                collection_name=settings.QDRANT_COLLECTION_PET_PROFILES,
                query_vector=query_embedding,
                limit=settings.PET_RECOGNITION_TOP_K,
                query_filter={
                    "must": [
                        {"key": "user_id", "match": {"value": user_id}}
                    ]
                }
            )
            
            if not search_result:
                logger.info(f"未识别到宠物: userId={user_id}")
                return None
            
            # 取最高分的宠物
            top_result = search_result[0]
            confidence = top_result.score
            
            if confidence < settings.PET_RECOGNITION_MIN_CONFIDENCE:
                logger.info(f"识别置信度过低: userId={user_id}, confidence={confidence}")
                return None
            
            # 收集 Top-K 个宠物ID
            pet_ids = []
            for result in search_result:
                if result.score >= settings.PET_RECOGNITION_MIN_CONFIDENCE:
                    pet_id = result.payload.get('pet_id')
                    if pet_id and pet_id not in pet_ids:
                        pet_ids.append(pet_id)
            
            return {
                'pet_ids': pet_ids,
                'confidence': confidence
            }
            
        except Exception as e:
            logger.error(f"宠物识别失败: userId={user_id}, error={e}")
            return None
    
    async def semantic_search(
        self, 
        user_id: int, 
        query: str,
        pet_id: Optional[int] = None,
        media_type: Optional[str] = None,
        limit: int = 20
    ) -> List[Dict[str, Any]]:
        """
        语义搜索照片/视频 (文本->图片/视频的跨模态搜索)
        
        Args:
            user_id: 用户ID
            query: 搜索查询 (自然语言)
            pet_id: 宠物ID (可选)
            media_type: 媒体类型过滤 (可选)
            limit: 返回数量
            
        Returns:
            搜索结果列表
        """
        try:
            # 使用 qwen3-vl-embedding 将查询文本向量化
            query_embedding = await generate_text_embedding(query)
            
            # 构建过滤条件
            filter_conditions = [
                {"key": "user_id", "match": {"value": user_id}}
            ]
            
            if pet_id:
                filter_conditions.append({"key": "pet_id", "match": {"value": pet_id}})
            
            if media_type:
                filter_conditions.append({"key": "media_type", "match": {"value": media_type}})
            
            # 在 Qdrant 中搜索
            search_results = qdrant_client.search(
                collection_name=settings.QDRANT_COLLECTION_MEDIA,
                query_vector=query_embedding,
                limit=limit,
                query_filter={"must": filter_conditions},
                score_threshold=settings.QDRANT_SEMANTIC_SEARCH_THRESHOLD
            )
            
            # 格式化结果
            results = []
            for hit in search_results:
                results.append({
                    'photo_id': hit.payload.get('photo_id'),
                    'pet_id': hit.payload.get('pet_id'),
                    'url': hit.payload.get('url'),
                    'media_type': hit.payload.get('media_type'),
                    'tags': hit.payload.get('tags', []),
                    'score': hit.score
                })
            
            logger.info(f"语义搜索完成: userId={user_id}, query={query}, results={len(results)}")
            return results
            
        except Exception as e:
            logger.error(f"语义搜索失败: userId={user_id}, query={query}, error={e}")
            return []
    
    async def _save_to_qdrant(
        self, 
        embedding_id: str, 
        vector: List[float], 
        payload: Dict[str, Any]
    ):
        """保存向量到 Qdrant"""
        try:
            qdrant_client.upsert(
                collection_name=settings.QDRANT_COLLECTION_MEDIA,
                points=[{
                    'id': point_id_from_embedding_id(embedding_id),
                    'vector': vector,
                    'payload': payload
                }]
            )
            logger.info(f"向量已保存: id={embedding_id}")
        except Exception as e:
            logger.error(f"保存向量失败: id={embedding_id}, error={e}")
            raise
    
    async def _update_photo_ai_result(
        self,
        photo_id: int,
        pet_id: Optional[int],
        embedding_id: str,
        auto_recognized: bool,
        recognition_confidence: Optional[float],
        recognized_pet_ids: List[int]
    ):
        """更新照片的 AI 分析结果"""
        try:
            async with get_async_session() as session:
                sql = text("""
                    UPDATE photo 
                    SET 
                        pet_id = :pet_id,
                        embedding_id = :embedding_id,
                        auto_recognized = :auto_recognized,
                        recognition_confidence = :recognition_confidence,
                        recognized_pet_ids = :recognized_pet_ids
                    WHERE id = :photo_id
                """)
                
                import json
                await session.execute(sql, {
                    'photo_id': photo_id,
                    'pet_id': pet_id,
                    'embedding_id': embedding_id,
                    'auto_recognized': auto_recognized,
                    'recognition_confidence': recognition_confidence,
                    'recognized_pet_ids': json.dumps(recognized_pet_ids) if recognized_pet_ids else None
                })
                await session.commit()
                
            logger.info(f"照片AI结果已更新: photoId={photo_id}")
        except Exception as e:
            logger.error(f"更新照片AI结果失败: photoId={photo_id}, error={e}")
            raise


# 全局实例
photo_analysis_service = PhotoAnalysisService()
