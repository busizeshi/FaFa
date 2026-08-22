"""
AI 对话路由（契约见技术文档 7.3）

同步链路：小程序 → Java 转发 → 本服务 LangGraph Agent
（意图分类 → 工具调用 → 回答生成 → 敏感边界检查）→ 流式返回。
"""

from typing import Optional

from fastapi import APIRouter, HTTPException, status
from loguru import logger
from pydantic import BaseModel, Field

router = APIRouter(prefix="/api/chat", tags=["chat"])


class ChatSendRequest(BaseModel):
    """AI 对话请求（Java → Python）"""

    user_id: int = Field(..., description="用户 ID")
    pet_id: Optional[int] = Field(default=None, description="当前宠物上下文，可空")
    message: str = Field(..., min_length=1, description="用户消息")
    session_id: str = Field(..., description="会话 ID")


@router.post("/send")
def send_chat(request: ChatSendRequest) -> dict:
    """
    AI 助手对话入口
    """
    logger.info(
        "收到对话请求: user_id={}, session_id={}, message_len={}",
        request.user_id, request.session_id, len(request.message),
    )
    raise HTTPException(
        status_code=status.HTTP_501_NOT_IMPLEMENTED,
        detail="AI 助手随 P1 LangGraph Agent 实现",
    )
