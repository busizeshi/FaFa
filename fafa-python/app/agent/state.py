"""LangGraph Agent 状态定义。"""

from typing import Any, TypedDict

from app.agent.intent import Intent
from app.schemas.chat import ChatSendRequest


class ChatState(TypedDict, total=False):
    """一次 Agent 请求的内部状态。"""

    request: ChatSendRequest
    intent: Intent
    start_date: str
    end_date: str
    tool_plan: list[str]
    evidence: list[dict[str, Any]]
    tool_calls: list[dict[str, Any]]
    answer: str
    blocks: list[dict[str, Any]]
    citations: list[dict[str, Any]]
    suggested_questions: list[str]
    record_draft: dict[str, Any] | None
    usage: dict[str, Any] | None
