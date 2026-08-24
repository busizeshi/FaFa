"""AI 对话路由。"""

from fastapi import APIRouter, HTTPException, status
from loguru import logger

from app.agent.graph import AgentError
from app.schemas.chat import ChatSendRequest, ChatSendResponse
from app.service.chat_service import chat_service


router = APIRouter(prefix="/api/chat", tags=["chat"])


@router.post("/send")
async def send_chat(request: ChatSendRequest) -> ChatSendResponse:
    """AI 助手对话入口。"""

    logger.info(
        "收到对话请求: user_id={}, session_id={}, message_len={}",
        request.user_id, request.session_id, len(request.message),
    )
    try:
        return ChatSendResponse.model_validate(await chat_service.chat(request))
    except AgentError as exc:
        logger.error("AI Agent 执行失败: trace_id={}", request.trace_id)
        raise HTTPException(status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                            detail="AI 服务暂时不可用") from exc
