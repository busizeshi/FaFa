"""
照片/媒体相关请求模型（契约见技术文档 7.3，字段与 Java 侧逐字节一致）
"""

from datetime import date, datetime
from typing import List, Literal, Optional

from pydantic import BaseModel, Field


class PhotoAnalyzeRequest(BaseModel):
    """媒体 embedding 向量化请求（Java → Python）"""

    user_id: int = Field(..., description="用户 ID")
    photo_id: int = Field(..., description="照片 ID")
    url: str = Field(..., description="本次 embedding 使用的对象存储可访问 URL")
    file_url: Optional[str] = Field(default=None, description="对象存储永久引用，写入 Qdrant")
    media_type: Literal["photo", "video"] = Field(..., description="photo / video")
    tags: Optional[List[str]] = Field(default=None, description="用户自定义标签")
    pet_id: Optional[int] = Field(default=None, description="已判定的宠物 ID，可空")
    candidate_pet_ids: Optional[List[int]] = Field(
        default=None, description="多宠物匹配候选；空列表表示无需匹配，省略兼容旧消息"
    )
    taken_at: Optional[datetime] = Field(default=None, description="素材拍摄时间")
    uploaded_at: Optional[datetime] = Field(default=None, description="素材上传入库时间")
    message_id: Optional[str] = Field(default=None, description="幂等键")


class PetSearchCandidate(BaseModel):
    """当前用户可供名称模糊匹配的宠物。"""

    pet_id: int = Field(..., description="宠物 ID")
    name: str = Field(..., min_length=1, description="宠物名称")


class PhotoSearchRequest(BaseModel):
    """语义检索请求（Java → Python）"""

    user_id: int = Field(..., description="用户 ID")
    query: str = Field(..., min_length=1, description="自然语言查询")
    limit: int = Field(default=20, ge=1, le=100, description="返回数量")
    pet_id: Optional[int] = Field(default=None, description="限定宠物，可空")
    pet_candidates: List[PetSearchCandidate] = Field(
        default_factory=list,
        description="多宠物时的受限名称匹配候选；单宠物或显式 pet_id 时为空",
    )
    start_date: Optional[date] = Field(default=None, description="起始日期 yyyy-MM-dd")
    end_date: Optional[date] = Field(default=None, description="结束日期 yyyy-MM-dd")
