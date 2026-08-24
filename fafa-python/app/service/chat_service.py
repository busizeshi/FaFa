"""AI 助手服务入口。"""

import asyncio
from typing import Any

from loguru import logger

from app.agent.graph import AgentError, ChatAgent
from app.core.config import get_settings
from app.schemas.chat import ChatSendRequest


class ChatService:
    """AI 助手编排服务。"""

    def __init__(self) -> None:
        self.agent = ChatAgent()

    async def chat(self, request: ChatSendRequest) -> dict[str, Any]:
        """执行一次同步 Agent 请求。"""

        logger.info("开始处理 AI 对话: user_id={}, session_id={}, message_len={}",
                    request.user_id, request.session_id, len(request.message))
        try:
            return await asyncio.wait_for(
                self.agent.chat(request),
                timeout=get_settings().agent_timeout_seconds,
            )
        except asyncio.TimeoutError as exc:
            logger.error("AI Agent 超时: trace_id={}", request.trace_id)
            raise AgentError("AI Agent 超时") from exc


chat_service = ChatService()
