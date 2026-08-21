"""
向量管理服务 (基于 qwen3-vl-embedding)

本服务已被 photo_service.py 和 pet_service.py 替代
保留此文件用于向后兼容和通用向量操作
"""
from typing import List, Dict, Any, Optional
from loguru import logger

from app.core.config import settings
from app.core.qdrant import qdrant_client, point_id_from_embedding_id
from app.core.dashscope_client import (
    generate_text_embedding,
    generate_image_embedding,
    generate_video_embedding,
    batch_generate_embeddings
)


class VectorService:
    """向量管理服务"""
    
    def __init__(self):
        """初始化"""
        pass
    
    async def search_by_text(
        self, 
        user_id: int,
        query: str,
        collection_name: str = None,
        pet_id: Optional[int] = None,
        media_type: Optional[str] = None,
        limit: int = 20
    ) -> List[Dict[str, Any]]:
        """
        文本语义搜索 (跨模态: 文本->图片/视频)
        
        Args:
            user_id: 用户ID
            query: 搜索查询文本
            collection_name: 集合名称 (默认: fafa_media)
            pet_id: 宠物ID过滤 (可选)
            media_type: 媒体类型过滤 (可选)
            limit: 返回数量
        
        Returns:
            搜索结果列表
        """
        try:
            logger.info(f"文本语义搜索: userId={user_id}, query={query}")
            
            # 使用 qwen3-vl-embedding 将文本向量化
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
            collection = collection_name or settings.QDRANT_COLLECTION_MEDIA
            search_results = qdrant_client.search(
                collection_name=collection,
                query_vector=query_embedding,
                limit=limit,
                query_filter={"must": filter_conditions},
                score_threshold=settings.QDRANT_SEMANTIC_SEARCH_THRESHOLD
            )
            
            # 格式化结果
            results = []
            for hit in search_results:
                results.append({
                    'id': hit.id,
                    'score': hit.score,
                    'payload': hit.payload
                })
            
            logger.info(f"搜索完成: results={len(results)}")
            return results
            
        except Exception as e:
            logger.error(f"文本搜索失败: error={e}", exc_info=True)
            return []
    
    async def search_by_image(
        self,
        user_id: int,
        image_url: str,
        collection_name: str = None,
        limit: int = 20
    ) -> List[Dict[str, Any]]:
        """
        图片相似度搜索 (图片->图片)
        
        Args:
            user_id: 用户ID
            image_url: 查询图片URL
            collection_name: 集合名称
            limit: 返回数量
        
        Returns:
            搜索结果列表
        """
        try:
            logger.info(f"图片相似度搜索: userId={user_id}, imageUrl={image_url}")
            
            # 使用 qwen3-vl-embedding 生成图片向量
            query_embedding = await generate_image_embedding(image_url)
            
            # 在 Qdrant 中搜索
            collection = collection_name or settings.QDRANT_COLLECTION_MEDIA
            search_results = qdrant_client.search(
                collection_name=collection,
                query_vector=query_embedding,
                limit=limit,
                query_filter={
                    "must": [
                        {"key": "user_id", "match": {"value": user_id}}
                    ]
                }
            )
            
            # 格式化结果
            results = []
            for hit in search_results:
                results.append({
                    'id': hit.id,
                    'score': hit.score,
                    'payload': hit.payload
                })
            
            logger.info(f"搜索完成: results={len(results)}")
            return results
            
        except Exception as e:
            logger.error(f"图片搜索失败: error={e}", exc_info=True)
            return []
    
    async def save_vector(
        self,
        embedding_id: str,
        vector: List[float],
        payload: Dict[str, Any],
        collection_name: str = None
    ):
        """
        保存向量到 Qdrant
        
        Args:
            embedding_id: 向量ID
            vector: 向量数据
            payload: 元数据
            collection_name: 集合名称 (默认: fafa_media)
        """
        try:
            collection = collection_name or settings.QDRANT_COLLECTION_MEDIA
            
            qdrant_client.upsert(
                collection_name=collection,
                points=[{
                    'id': point_id_from_embedding_id(embedding_id),
                    'vector': vector,
                    'payload': payload
                }]
            )
            
            logger.info(f"向量已保存: id={embedding_id}, collection={collection}")
            
        except Exception as e:
            logger.error(f"保存向量失败: id={embedding_id}, error={e}")
            raise
    
    async def delete_vector(
        self,
        embedding_id: str,
        collection_name: str = None
    ):
        """
        删除向量
        
        Args:
            embedding_id: 向量ID
            collection_name: 集合名称 (默认: fafa_media)
        """
        try:
            collection = collection_name or settings.QDRANT_COLLECTION_MEDIA
            
            qdrant_client.delete(
                collection_name=collection,
                points_selector=[point_id_from_embedding_id(embedding_id)]
            )
            
            logger.info(f"向量已删除: id={embedding_id}, collection={collection}")
            
        except Exception as e:
            logger.error(f"删除向量失败: id={embedding_id}, error={e}")
            raise
    
    async def batch_save_vectors(
        self,
        points: List[Dict[str, Any]],
        collection_name: str = None
    ):
        """
        批量保存向量
        
        Args:
            points: 向量点列表 [{'id': ..., 'vector': ..., 'payload': ...}, ...]
            collection_name: 集合名称 (默认: fafa_media)
        """
        try:
            collection = collection_name or settings.QDRANT_COLLECTION_MEDIA
            
            qdrant_client.upsert(
                collection_name=collection,
                points=[
                    {**p, 'id': point_id_from_embedding_id(p['id'])}
                    for p in points
                ]
            )
            
            logger.info(f"批量保存向量完成: count={len(points)}, collection={collection}")
            
        except Exception as e:
            logger.error(f"批量保存向量失败: error={e}")
            raise


# 全局实例
vector_service = VectorService()
