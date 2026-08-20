"""
向量搜索 API (基于 qwen3-vl-embedding)
"""
from typing import List, Optional
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from loguru import logger

from app.service.vector_service import vector_service

router = APIRouter(prefix="/vector", tags=["向量搜索"])


class TextSearchRequest(BaseModel):
    """文本搜索请求"""
    user_id: int
    query: str
    collection_name: Optional[str] = None
    pet_id: Optional[int] = None
    media_type: Optional[str] = None
    limit: int = 20


class ImageSearchRequest(BaseModel):
    """图片搜索请求"""
    user_id: int
    image_url: str
    collection_name: Optional[str] = None
    limit: int = 20


class VectorSaveRequest(BaseModel):
    """保存向量请求"""
    embedding_id: str
    vector: List[float]
    payload: dict
    collection_name: Optional[str] = None


class SearchResult(BaseModel):
    """搜索结果"""
    id: str
    score: float
    payload: dict


class SearchResponse(BaseModel):
    """搜索响应"""
    total: int
    results: List[SearchResult]


@router.post("/search/text", response_model=SearchResponse)
async def search_by_text(request: TextSearchRequest):
    """
    文本语义搜索（跨模态：文本->图片/视频）
    
    使用 qwen3-vl-embedding 将文本查询映射到图片/视频向量空间
    """
    try:
        logger.info(f"文本搜索: userId={request.user_id}, query={request.query}")
        
        results = await vector_service.search_by_text(
            user_id=request.user_id,
            query=request.query,
            collection_name=request.collection_name,
            pet_id=request.pet_id,
            media_type=request.media_type,
            limit=request.limit
        )
        
        search_results = [
            SearchResult(
                id=r['id'],
                score=r['score'],
                payload=r['payload']
            )
            for r in results
        ]
        
        logger.info(f"文本搜索完成: 找到 {len(search_results)} 条结果")
        
        return SearchResponse(
            total=len(search_results),
            results=search_results
        )
        
    except Exception as e:
        logger.error(f"文本搜索失败: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"搜索失败: {str(e)}")


@router.post("/search/image", response_model=SearchResponse)
async def search_by_image(request: ImageSearchRequest):
    """
    图片相似度搜索（图片->图片）
    
    使用 qwen3-vl-embedding 生成图片向量并搜索相似图片
    """
    try:
        logger.info(f"图片搜索: userId={request.user_id}, imageUrl={request.image_url}")
        
        results = await vector_service.search_by_image(
            user_id=request.user_id,
            image_url=request.image_url,
            collection_name=request.collection_name,
            limit=request.limit
        )
        
        search_results = [
            SearchResult(
                id=r['id'],
                score=r['score'],
                payload=r['payload']
            )
            for r in results
        ]
        
        logger.info(f"图片搜索完成: 找到 {len(search_results)} 条结果")
        
        return SearchResponse(
            total=len(search_results),
            results=search_results
        )
        
    except Exception as e:
        logger.error(f"图片搜索失败: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"搜索失败: {str(e)}")


@router.post("/save")
async def save_vector(request: VectorSaveRequest):
    """
    保存向量到 Qdrant
    """
    try:
        logger.info(f"保存向量: embeddingId={request.embedding_id}")
        
        await vector_service.save_vector(
            embedding_id=request.embedding_id,
            vector=request.vector,
            payload=request.payload,
            collection_name=request.collection_name
        )
        
        return {
            "message": "向量保存成功",
            "embedding_id": request.embedding_id
        }
        
    except Exception as e:
        logger.error(f"保存向量失败: {e}")
        raise HTTPException(status_code=500, detail=f"保存失败: {str(e)}")


@router.delete("/{embedding_id}")
async def delete_vector(
    embedding_id: str,
    collection_name: Optional[str] = None
):
    """
    删除向量
    """
    try:
        logger.info(f"删除向量: embeddingId={embedding_id}")
        
        await vector_service.delete_vector(
            embedding_id=embedding_id,
            collection_name=collection_name
        )
        
        return {
            "message": "向量删除成功",
            "embedding_id": embedding_id
        }
        
    except Exception as e:
        logger.error(f"删除向量失败: {e}")
        raise HTTPException(status_code=500, detail=f"删除失败: {str(e)}")
