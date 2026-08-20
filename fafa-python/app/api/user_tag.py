"""
用户标签 API
"""
from typing import List, Optional
from fastapi import APIRouter, HTTPException, Query
from pydantic import BaseModel
from loguru import logger

from app.service.user_tag_service import user_tag_service

router = APIRouter(prefix="/user-tags", tags=["用户标签"])


class CreateTagRequest(BaseModel):
    """创建标签请求"""
    user_id: int
    name: str
    color: Optional[str] = None
    icon: Optional[str] = None


class UpdateTagRequest(BaseModel):
    """更新标签请求"""
    tag_id: int
    user_id: int
    name: Optional[str] = None
    color: Optional[str] = None
    icon: Optional[str] = None


class BatchDeleteTagsRequest(BaseModel):
    """批量删除标签请求"""
    tag_ids: List[int]
    user_id: int


class TagResponse(BaseModel):
    """标签响应"""
    id: int
    user_id: int
    name: str
    color: Optional[str]
    icon: Optional[str]
    use_count: int
    created_at: str
    updated_at: str


class TagListResponse(BaseModel):
    """标签列表响应"""
    total: int
    tags: List[TagResponse]


@router.post("/create")
async def create_tag(request: CreateTagRequest):
    """
    创建用户标签
    """
    try:
        logger.info(f"创建用户标签: userId={request.user_id}, name={request.name}")
        
        tag_id = await user_tag_service.create_tag(
            user_id=request.user_id,
            name=request.name,
            color=request.color,
            icon=request.icon
        )
        
        return {
            "message": "标签创建成功",
            "tag_id": tag_id
        }
        
    except Exception as e:
        logger.error(f"创建用户标签失败: {e}")
        raise HTTPException(status_code=500, detail=f"创建失败: {str(e)}")


@router.get("/list")
async def get_user_tags(
    user_id: int = Query(..., description="用户ID"),
    keyword: Optional[str] = Query(None, description="搜索关键词"),
    limit: int = Query(100, description="返回数量")
):
    """
    获取用户标签列表
    """
    try:
        logger.info(f"获取用户标签列表: userId={user_id}, keyword={keyword}")
        
        tags = await user_tag_service.get_user_tags(
            user_id=user_id,
            keyword=keyword,
            limit=limit
        )
        
        tag_responses = [
            TagResponse(
                id=tag['id'],
                user_id=tag['user_id'],
                name=tag['name'],
                color=tag['color'],
                icon=tag['icon'],
                use_count=tag['use_count'],
                created_at=str(tag['created_at']),
                updated_at=str(tag['updated_at'])
            )
            for tag in tags
        ]
        
        return TagListResponse(
            total=len(tag_responses),
            tags=tag_responses
        )
        
    except Exception as e:
        logger.error(f"获取用户标签列表失败: {e}")
        raise HTTPException(status_code=500, detail=f"获取失败: {str(e)}")


@router.get("/popular")
async def get_popular_tags(
    user_id: int = Query(..., description="用户ID"),
    limit: int = Query(20, description="返回数量")
):
    """
    获取热门标签（按使用次数排序）
    """
    try:
        logger.info(f"获取热门标签: userId={user_id}")
        
        tags = await user_tag_service.get_popular_tags(
            user_id=user_id,
            limit=limit
        )
        
        return {
            "total": len(tags),
            "tags": tags
        }
        
    except Exception as e:
        logger.error(f"获取热门标签失败: {e}")
        raise HTTPException(status_code=500, detail=f"获取失败: {str(e)}")


@router.put("/update")
async def update_tag(request: UpdateTagRequest):
    """
    更新标签
    """
    try:
        logger.info(f"更新用户标签: tagId={request.tag_id}, userId={request.user_id}")
        
        await user_tag_service.update_tag(
            tag_id=request.tag_id,
            user_id=request.user_id,
            name=request.name,
            color=request.color,
            icon=request.icon
        )
        
        return {
            "message": "标签更新成功",
            "tag_id": request.tag_id
        }
        
    except Exception as e:
        logger.error(f"更新用户标签失败: {e}")
        raise HTTPException(status_code=500, detail=f"更新失败: {str(e)}")


@router.delete("/{tag_id}")
async def delete_tag(
    tag_id: int,
    user_id: int = Query(..., description="用户ID")
):
    """
    删除标签
    """
    try:
        logger.info(f"删除用户标签: tagId={tag_id}, userId={user_id}")
        
        await user_tag_service.delete_tag(
            tag_id=tag_id,
            user_id=user_id
        )
        
        return {
            "message": "标签删除成功",
            "tag_id": tag_id
        }
        
    except Exception as e:
        logger.error(f"删除用户标签失败: {e}")
        raise HTTPException(status_code=500, detail=f"删除失败: {str(e)}")


@router.post("/batch-delete")
async def batch_delete_tags(request: BatchDeleteTagsRequest):
    """
    批量删除标签
    """
    try:
        logger.info(f"批量删除用户标签: userId={request.user_id}, count={len(request.tag_ids)}")
        
        await user_tag_service.batch_delete_tags(
            tag_ids=request.tag_ids,
            user_id=request.user_id
        )
        
        return {
            "message": "批量删除成功",
            "count": len(request.tag_ids)
        }
        
    except Exception as e:
        logger.error(f"批量删除用户标签失败: {e}")
        raise HTTPException(status_code=500, detail=f"删除失败: {str(e)}")


@router.get("/count")
async def get_tag_count(
    user_id: int = Query(..., description="用户ID")
):
    """
    获取用户标签总数
    """
    try:
        count = await user_tag_service.get_tag_count(user_id=user_id)
        
        return {
            "user_id": user_id,
            "count": count
        }
        
    except Exception as e:
        logger.error(f"获取用户标签总数失败: {e}")
        raise HTTPException(status_code=500, detail=f"获取失败: {str(e)}")
