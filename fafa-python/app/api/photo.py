"""
照片/视频搜索 API (基于 qwen3-vl-embedding)
"""
from typing import List, Optional
from fastapi import APIRouter, Query, HTTPException
from pydantic import BaseModel
from loguru import logger

from app.service.photo_service import photo_analysis_service
from app.core.config import settings

router = APIRouter(prefix="/photos", tags=["照片"])


class MediaAnalysisRequest(BaseModel):
    """媒体分析请求"""
    photo_id: int
    user_id: int
    pet_id: Optional[int] = None
    url: str
    media_type: str  # 'image' 或 'video'
    tags: Optional[List[str]] = None


class SemanticSearchRequest(BaseModel):
    """语义搜索请求"""
    user_id: int
    query: str
    pet_id: Optional[int] = None
    media_type: Optional[str] = None  # 'image' 或 'video'
    limit: int = 20


class SearchResult(BaseModel):
    """搜索结果"""
    photo_id: int
    pet_id: Optional[int]
    url: str
    media_type: str
    tags: List[str]
    score: float


class SearchResponse(BaseModel):
    """搜索响应"""
    total: int
    results: List[SearchResult]


@router.post("/analyze")
async def analyze_media(request: MediaAnalysisRequest):
    """
    分析照片或视频
    使用 qwen3-vl-embedding 生成向量并自动识别宠物
    """
    try:
        logger.info(f"接收媒体分析请求: photoId={request.photo_id}, mediaType={request.media_type}")
        
        await photo_analysis_service.analyze_media(
            photo_id=request.photo_id,
            user_id=request.user_id,
            pet_id=request.pet_id,
            url=request.url,
            media_type=request.media_type,
            tags=request.tags
        )
        
        return {
            "message": "媒体分析成功",
            "photo_id": request.photo_id
        }
        
    except Exception as e:
        logger.error(f"媒体分析失败: photoId={request.photo_id}, error={e}")
        raise HTTPException(status_code=500, detail=f"分析失败: {str(e)}")


@router.post("/search", response_model=SearchResponse)
async def semantic_search(request: SemanticSearchRequest):
    """
    语义搜索照片/视频（跨模态）
    
    支持自然语言查询，例如：
    - "搜索去年12月阿酷在阳台趴着的照片和视频"
    - "正在睡觉的照片"
    - "有玩具的照片"
    """
    try:
        logger.info(f"语义搜索: userId={request.user_id}, query={request.query}")
        
        results = await photo_analysis_service.semantic_search(
            user_id=request.user_id,
            query=request.query,
            pet_id=request.pet_id,
            media_type=request.media_type,
            limit=request.limit
        )
        
        search_results = [
            SearchResult(
                photo_id=r['photo_id'],
                pet_id=r['pet_id'],
                url=r['url'],
                media_type=r['media_type'],
                tags=r['tags'],
                score=r['score']
            )
            for r in results
        ]
        
        logger.info(f"语义搜索完成: 找到 {len(search_results)} 条结果")
        
        return SearchResponse(
            total=len(search_results),
            results=search_results
        )
        
    except Exception as e:
        logger.error(f"语义搜索失败: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"搜索失败: {str(e)}")
