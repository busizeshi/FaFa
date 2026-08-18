"""
向量搜索 API
"""
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from loguru import logger

from app.service.vector_service import VectorService

router = APIRouter()
vector_service = VectorService()


class SearchRequest(BaseModel):
    """搜索请求"""
    user_id: int
    pet_id: int
    query: str
    limit: int = 10


class SearchResult(BaseModel):
    """搜索结果"""
    photo_id: int
    url: str
    description: str
    score: float


class SearchResponse(BaseModel):
    """搜索响应"""
    query: str
    results: list[SearchResult]
    total: int


@router.post("/search", response_model=SearchResponse)
async def search_photos(request: SearchRequest):
    """
    使用自然语言搜索照片
    
    Args:
        request: 搜索请求
    
    Returns:
        搜索结果
    """
    try:
        logger.info(f"照片搜索: pet_id={request.pet_id}, query={request.query}")
        
        results = await vector_service.search(
            pet_id=request.pet_id,
            query=request.query,
            limit=request.limit,
        )
        
        logger.info(f"搜索完成: 找到 {len(results)} 张照片")
        
        return SearchResponse(
            query=request.query,
            results=[SearchResult(**r) for r in results],
            total=len(results),
        )
        
    except Exception as e:
        logger.error(f"搜索失败: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"搜索失败: {str(e)}")


@router.post("/index")
async def index_photo(photo_id: int, image_url: str, description: str):
    """
    将照片索引到向量库
    
    Args:
        photo_id: 照片 ID
        image_url: 图片 URL
        description: 描述
    
    Returns:
        索引结果
    """
    try:
        logger.info(f"索引照片: photo_id={photo_id}")
        
        embedding_id = await vector_service.index_photo(
            photo_id=photo_id,
            image_url=image_url,
            description=description,
        )
        
        logger.info(f"照片索引成功: embedding_id={embedding_id}")
        
        return {"message": "索引成功", "embedding_id": embedding_id}
        
    except Exception as e:
        logger.error(f"索引失败: {str(e)}")
        raise HTTPException(status_code=500, detail=f"索引失败: {str(e)}")
