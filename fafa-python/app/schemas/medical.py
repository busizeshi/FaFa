"""医疗摘要接口的请求与响应模型。"""

from typing import Any

from pydantic import BaseModel, Field


class MedicalSummaryRequest(BaseModel):
    """Java 传入的医疗摘要请求。"""

    user_id: int = Field(..., gt=0)
    pet_id: int = Field(..., gt=0)
    start_date: str
    end_date: str
    include_media: bool = False
    trace_id: str = "no-trace"


class MedicalSummaryResponse(BaseModel):
    """对外返回的结构化医疗摘要。"""

    pet: dict[str, Any] = Field(default_factory=dict)
    range: dict[str, str] = Field(default_factory=dict)
    overview: str
    symptom_timeline: list[dict[str, Any]] = Field(default_factory=list,
                                                    alias="symptomTimeline")
    medical_visits: list[dict[str, Any]] = Field(default_factory=list,
                                                 alias="medicalVisits")
    medications: list[dict[str, Any]] = Field(default_factory=list)
    attention_items: list[str] = Field(default_factory=list, alias="attentionItems")
    warnings: list[str] = Field(default_factory=list)
    plain_text: str = Field(alias="plainText")
    disclaimer: str

    model_config = {"populate_by_name": True}
