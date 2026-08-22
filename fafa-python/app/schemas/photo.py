"""
照片/媒体相关请求模型（契约见技术文档 7.3，字段与 Java 侧逐字节一致）
"""

from typing import List, Optional

from pydantic import BaseModel, Field


class PhotoAnalyzeRequest(BaseModel):
    """媒体理解与向量化请求（Java → Python）"""

    user_id: int = Field(..., description="用户 ID")
    photo_id: int = Field(..., description="照片 ID")
    url: str = Field(..., description="对象存储可访问 URL")
    media_type: str = Field(..., description="photo / video / pet_profile")
    tags: Optional[List[str]] = Field(default=None, description="用户自定义标签")
    pet_id: Optional[int] = Field(default=None, description="已判定的宠物 ID，可空")
    message_id: Optional[str] = Field(default=None, description="幂等键")


class PhotoSearchRequest(BaseModel):
    """语义检索请求（Java → Python）"""

    user_id: int = Field(..., description="用户 ID")
    query: str = Field(..., min_length=1, description="自然语言查询")
    limit: int = Field(default=20, ge=1, le=100, description="返回数量")
    pet_id: Optional[int] = Field(default=None, description="限定宠物，可空")
    start_date: Optional[str] = Field(default=None, description="起始日期 yyyy-MM-dd")
    end_date: Optional[str] = Field(default=None, description="结束日期 yyyy-MM-dd")
