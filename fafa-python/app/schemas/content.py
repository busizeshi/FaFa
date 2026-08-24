"""内容草稿生成接口契约。"""

from typing import Any

from pydantic import BaseModel, Field


class ContentGenerationRequest(BaseModel):
    draft_id: int = Field(..., alias="draftId", gt=0)
    pet_id: int = Field(..., alias="petId", gt=0)
    content_type: str = Field(..., alias="contentType")
    topic_type: str = Field(..., alias="topicType")
    custom_topic: str | None = Field(default=None, alias="customTopic")
    fact_snapshot_json: str = Field(..., alias="factSnapshotJson", min_length=2)
    selected_media_json: str = Field(default="[]", alias="selectedMediaJson")
    prompt_version: str = Field(..., alias="promptVersion")

    model_config = {"populate_by_name": True}


class ContentDraftContent(BaseModel):
    title: str
    body: str
    hashtags: list[str] = Field(default_factory=list)
    media_order: list[int] = Field(default_factory=list, alias="mediaOrder")
    source_keys: list[str] = Field(default_factory=list, alias="sourceKeys")

    model_config = {"populate_by_name": True}


class ContentGenerationResponse(BaseModel):
    content: ContentDraftContent
    mode: str
    model_name: str = Field(alias="modelName")
    prompt_version: str = Field(alias="promptVersion")

    model_config = {"populate_by_name": True}
