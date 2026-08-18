"""
照片搜索 API
"""
from typing import List, Optional
from fastapi import APIRouter, Query, HTTPException
from pydantic import BaseModel
from loguru import logger

from app.service.photo_service import PhotoAnalysisService
from app.core.qdrant import qdrant_client
from app.repository.photo_repository import PhotoRepository
from dashscope import TextEmbedding
import dashscope
from app.core.config import settings

router = APIRouter(prefix="/photos", tags=["照片"])

dashscope.api_key = settings.DASHSCOPE_API_KEY


class PhotoSearchRequest(BaseModel):
    """照片搜索请求"""
    pet_id: int
    query: str
    limit: int = 20


class PhotoSearchResult(BaseModel):
    """照片搜索结果"""
    photo_id: int
    pet_id: int
    url: str
    thumbnail_url: Optional[str]
    description: Optional[str]
    ai_description: Optional[str]
    score: float


class PhotoSearchResponse(BaseModel):
    """照片搜索响应"""
    total: int
    results: List[PhotoSearchResult]
    insight: Optional[str] = None


@router.post("/search", response_model=PhotoSearchResponse)
async def search_photos(request: PhotoSearchRequest):
    """
    语义搜索照片
    
    通过自然语言描述搜索照片，例如：
    - "去年夏天在阳台的照片"
    - "正在睡觉的照片"
    - "有玩具的照片"
    """
    try:
        # 1. 生成查询文本的 Embedding
        query_embedding = await generate_query_embedding(request.query)
        
        if not query_embedding:
            raise HTTPException(status_code=500, detail="生成查询向量失败")
        
        # 2. 在 Qdrant 中搜索
        from qdrant_client.models import Filter, FieldCondition, MatchValue
        
        search_results = await qdrant_client.search(
            collection_name='pet_photos',
            query_vector=query_embedding,
            query_filter=Filter(
                must=[
                    FieldCondition(
                        key='pet_id',
                        match=MatchValue(value=request.pet_id)
                    )
                ]
            ),
            limit=request.limit,
            score_threshold=0.6  # 相似度阈值
        )
        
        # 3. 获取照片详细信息
        photo_repo = PhotoRepository()
        results = []
        
        for hit in search_results:
            payload = hit.payload
            results.append(PhotoSearchResult(
                photo_id=payload['photo_id'],
                pet_id=payload['pet_id'],
                url=payload['url'],
                thumbnail_url=payload.get('url'),  # 使用原图作为缩略图
                description=payload.get('description'),
                ai_description=payload.get('description'),
                score=hit.score
            ))
        
        # 4. 生成搜索洞察（可选）
        insight = None
        if results:
            insight = f"找到 {len(results)} 张相关照片"
        
        logger.info(f"照片搜索完成: petId={request.pet_id}, query='{request.query}', results={len(results)}")
        
        return PhotoSearchResponse(
            total=len(results),
            results=results,
            insight=insight
        )
        
    except Exception as e:
        logger.error(f"照片搜索失败: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"搜索失败: {str(e)}")


async def generate_query_embedding(query: str) -> Optional[List[float]]:
    """
    生成查询文本的 Embedding
    
    Args:
        query: 查询文本
        
    Returns:
        向量列表
    """
    try:
        response = TextEmbedding.call(
            model=TextEmbedding.Models.text_embedding_v2,
            input=query
        )
        
        if response.status_code == 200:
            embedding = response.output['embeddings'][0]['embedding']
            logger.info(f"生成查询 Embedding 成功: query='{query}'")
            return embedding
        else:
            logger.error(f"生成查询 Embedding 失败: {response.code}, {response.message}")
            return None
            
    except Exception as e:
        logger.error(f"生成查询 Embedding 失败: {e}", exc_info=True)
        return None


@router.get("/list")
async def list_photos(pet_id: int = Query(..., description="宠物ID"), 
                     limit: int = Query(20, description="返回数量")):
    """
    查询照片列表
    
    Args:
        pet_id: 宠物ID
        limit: 返回数量，默认20
    """
    try:
        photo_repo = PhotoRepository()
        photos = await photo_repo.list_photos_by_pet(pet_id, limit)
        
        return {
            'total': len(photos),
            'photos': photos
        }
        
    except Exception as e:
        logger.error(f"查询照片列表失败: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"查询失败: {str(e)}")
