"""AI 对话请求模型"""

from pydantic import BaseModel, Field


class ChatSendRequest(BaseModel):
    """对话请求"""

    user_id: int = Field(description="用户ID")
    session_id: str = Field(description="会话ID")
    message: str = Field(min_length=1, description="用户消息")
    pet_id: int | None = Field(default=None, description="当前宠物上下文，可空")
