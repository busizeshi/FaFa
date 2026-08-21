"""Pydantic 请求模型（snake_case，与 Java 侧消息体经 camelCase->snake_case 映射后对齐）"""

from pydantic import BaseModel, Field


class AnalyzeMediaRequest(BaseModel):
    """媒体分析请求"""

    user_id: int = Field(description="归属用户")
    photo_id: int = Field(description="素材ID（Java 侧主键）")
    url: str = Field(description="素材访问 URL")
    media_type: str = Field(default="photo", description="photo / video")
    pet_id: int | None = Field(default=None, description="归属宠物，可空")
    tags: list[str] = Field(default_factory=list, description="用户选择的标签")
    trace_id: str | None = Field(default=None, description="链路追踪ID")


class SearchPhotosRequest(BaseModel):
    """语义检索请求"""

    user_id: int = Field(description="归属用户（必填，数据隔离边界）")
    query: str = Field(min_length=1, description="自然语言查询")
    limit: int = Field(default=20, ge=1, le=100, description="返回条数")
    pet_id: int | None = Field(default=None, description="按宠物过滤，可空")
    tags: list[str] | None = Field(default=None, description="按标签过滤")
    date_from: str | None = Field(default=None, description="拍摄日期起（yyyy-MM-dd）")
    date_to: str | None = Field(default=None, description="拍摄日期止（yyyy-MM-dd）")
