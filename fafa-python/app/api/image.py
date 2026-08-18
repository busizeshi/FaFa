"""
图片识别 API
"""
from fastapi import APIRouter, HTTPException, UploadFile, File
from pydantic import BaseModel
from loguru import logger

from app.service.image_service import ImageService

router = APIRouter()
image_service = ImageService()


class ImageAnalysisResponse(BaseModel):
    """图片分析响应"""
    description: str
    tags: list[str]
    confidence: float | None = None


@router.post("/analyze", response_model=ImageAnalysisResponse)
async def analyze_image(image_url: str):
    """
    分析图片内容
    
    Args:
        image_url: 图片 URL
    
    Returns:
        分析结果
    """
    try:
        logger.info(f"开始分析图片: {image_url}")
        
        result = await image_service.analyze_image(image_url)
        
        logger.info(f"图片分析完成: tags={result['tags']}")
        
        return ImageAnalysisResponse(**result)
        
    except Exception as e:
        logger.error(f"图片分析失败: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"图片分析失败: {str(e)}")


@router.post("/upload")
async def upload_image(file: UploadFile = File(...)):
    """
    上传图片
    
    Args:
        file: 图片文件
    
    Returns:
        上传结果
    """
    try:
        logger.info(f"上传图片: {file.filename}")
        
        # TODO: 实现图片上传到 MinIO
        
        return {"message": "图片上传成功", "url": ""}
        
    except Exception as e:
        logger.error(f"图片上传失败: {str(e)}")
        raise HTTPException(status_code=500, detail=f"图片上传失败: {str(e)}")
