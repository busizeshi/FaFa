"""周期报告生成接口契约。"""

from typing import Any

from pydantic import BaseModel, Field


class ReportGenerationRequest(BaseModel):
    report_id: int = Field(..., alias="reportId", gt=0)
    pet_id: int = Field(..., alias="petId", gt=0)
    period_type: str = Field(..., alias="periodType")
    start_date: str = Field(..., alias="startDate")
    end_date: str = Field(..., alias="endDate")
    fact_snapshot_json: str = Field(..., alias="factSnapshotJson", min_length=2)
    prompt_version: str = Field(..., alias="promptVersion")

    model_config = {"populate_by_name": True}


class ReportContent(BaseModel):
    title: str
    summary: str
    highlights: list[str] = Field(default_factory=list)
    care_suggestions: list[str] = Field(default_factory=list, alias="careSuggestions")
    timeline: list[dict[str, Any]] = Field(default_factory=list)
    media_suggestions: list[dict[str, Any]] = Field(default_factory=list, alias="mediaSuggestions")
    source_keys: list[str] = Field(default_factory=list, alias="sourceKeys")

    model_config = {"populate_by_name": True}


class ReportGenerationResponse(BaseModel):
    content: ReportContent
    mode: str
    model_name: str = Field(alias="modelName")
    prompt_version: str = Field(alias="promptVersion")

    model_config = {"populate_by_name": True}
