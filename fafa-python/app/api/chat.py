"""
AI 对话 API
"""
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from loguru import logger

from app.service.chat_service import ChatService

router = APIRouter()
chat_service = ChatService()


class ChatRequest(BaseModel):
    """对话请求"""
    user_id: int
    pet_id: int | None = None
    message: str
    conversation_id: int | None = None


class ChatResponse(BaseModel):
    """对话响应"""
    conversation_id: int
    message: str
    token_count: int | None = None


@router.post("/send", response_model=ChatResponse)
async def send_message(request: ChatRequest):
    """
    发送消息给 AI
    
    Args:
        request: 对话请求
    
    Returns:
        AI 响应
    """
    try:
        logger.info(f"收到对话请求: user_id={request.user_id}, message={request.message[:50]}")
        
        response = await chat_service.chat(
            user_id=request.user_id,
            pet_id=request.pet_id,
            message=request.message,
            conversation_id=request.conversation_id,
        )
        
        logger.info(f"对话响应成功: conversation_id={response['conversation_id']}")
        
        return ChatResponse(**response)
        
    except Exception as e:
        logger.error(f"对话失败: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"对话失败: {str(e)}")


@router.get("/history/{conversation_id}")
async def get_conversation_history(conversation_id: int):
    """
    获取对话历史
    
    Args:
        conversation_id: 对话 ID
    
    Returns:
        对话历史
    """
    try:
        history = await chat_service.get_history(conversation_id)
        return {"conversation_id": conversation_id, "messages": history}
        
    except Exception as e:
        logger.error(f"获取对话历史失败: {str(e)}")
        raise HTTPException(status_code=500, detail=f"获取对话历史失败: {str(e)}")
