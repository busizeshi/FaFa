"""
AI 助手对话服务：LangGraph 状态图编排

TODO P1 实现，图结构（见技术文档 5.4）：
意图分类 → 工具调用（查记录/查照片/查就医档案，经 HTTP 回调 Java 只读接口取数）
→ 回答生成 → 敏感边界检查（不输出医疗诊断结论）
"""

from loguru import logger


class ChatService:
    """AI 助手编排服务"""

    async def chat(self, user_id: int, pet_id: int | None,
                   message: str, session_id: str) -> str:
        """执行对话（流式返回由路由层处理）"""
        logger.info("对话处理: user_id={}, session_id={}", user_id, session_id)
        raise NotImplementedError("AI 助手随 P1 LangGraph Agent 实现")
