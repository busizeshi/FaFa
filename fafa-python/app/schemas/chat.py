"""AI 对话与自然语言记录的 API 模型。"""

from typing import Any

from pydantic import BaseModel, Field


class ChatHistoryItem(BaseModel):
    """Java 传入的历史消息。"""

    role: str
    content: str


class ChatSendRequest(BaseModel):
    """Java -> Python 的 AI 对话请求。"""

    user_id: int = Field(..., gt=0)
    pet_id: int | None = Field(default=None, gt=0)
    message: str = Field(..., min_length=1, max_length=8000)
    session_id: str = Field(..., min_length=1, max_length=64)
    message_id: str = Field(..., min_length=1, max_length=64)
    history: list[ChatHistoryItem] = Field(default_factory=list, max_length=20)
    trace_id: str = Field(default="no-trace", max_length=64)


class ChatUsage(BaseModel):
    """模型用量。"""

    model: str | None = None
    prompt_tokens: int | None = None
    completion_tokens: int | None = None


class ChatSendResponse(BaseModel):
    """AI Agent 结构化响应。"""

    answer: str
    blocks: list[dict[str, Any]] = Field(default_factory=list)
    citations: list[dict[str, Any]] = Field(default_factory=list)
    suggested_questions: list[str] = Field(default_factory=list)
    record_draft: dict[str, Any] | None = None
    tool_calls: list[dict[str, Any]] = Field(default_factory=list)
    usage: ChatUsage | None = None
    trace_id: str


class RecordParseRequest(BaseModel):
    """自然语言记录解析请求。"""

    user_id: int = Field(..., gt=0)
    pet_id: int = Field(..., gt=0)
    message: str = Field(..., min_length=1, max_length=8000)
    session_id: str | None = Field(default=None, max_length=64)
    trace_id: str = Field(default="no-trace", max_length=64)


class RecordParseResponse(BaseModel):
    """自然语言记录草稿响应。"""

    records: list[dict[str, Any]] = Field(default_factory=list)
    requires_user_confirmation: bool = True
    warnings: list[str] = Field(default_factory=list)
