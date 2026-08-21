"""AI 对话 API（骨架，Agent 落地时补实现）"""

from fastapi import APIRouter, HTTPException, status
from loguru import logger

from app.schemas.chat import ChatSendRequest

router = APIRouter(prefix="/chat", tags=["chat"])


@router.post("/send")
def send_chat(req: ChatSendRequest) -> dict:
    """AI 对话（LangGraph Agent 编排：意图分类 -> 工具调用 -> 回答生成）

    TODO: app/agent 实现状态图后接入；工具通过 Java 只读接口取数
    """
    logger.info(f"[骨架] 对话请求: user_id={req.user_id}, session_id={req.session_id}")
    raise HTTPException(status_code=status.HTTP_501_NOT_IMPLEMENTED, detail="AI 对话待实现")
