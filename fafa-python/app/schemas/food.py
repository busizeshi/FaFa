"""食品包装识别接口契约。"""

from typing import Any

from pydantic import BaseModel, Field


class FoodRecognitionRequest(BaseModel):
    """Java 传入的食品包装识别请求。"""

    task_id: int = Field(..., alias="taskId", gt=0)
    user_id: int = Field(..., alias="userId", gt=0)
    pet_id: int = Field(..., alias="petId", gt=0)
    media_id: int = Field(..., alias="mediaId", gt=0)
    image_url: str = Field(..., alias="imageUrl", min_length=1)
    prompt_version: str = Field(default="food-recognition-v1", alias="promptVersion", min_length=1)
    trace_id: str = Field(default="no-trace", alias="traceId", min_length=1)

    model_config = {"populate_by_name": True}


class FoodRecognitionResponse(BaseModel):
    """食品包装识别结构化响应。"""

    task_id: int = Field(alias="taskId")
    status: str = "completed"
    model: str
    prompt_version: str = Field(alias="promptVersion")
    draft: dict[str, Any] = Field(default_factory=dict)
    warnings: list[str] = Field(default_factory=list)
    usage: dict[str, Any] = Field(default_factory=dict)
    trace_id: str = Field(alias="traceId")

    model_config = {"populate_by_name": True}
