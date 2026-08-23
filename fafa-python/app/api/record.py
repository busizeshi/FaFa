"""
记录路由：AI 自然语言记录解析（契约预留）
"""

from fastapi import APIRouter, HTTPException, status
from loguru import logger
from pydantic import BaseModel, Field

router = APIRouter(prefix="/api/records", tags=["records"])


class RecordParseRequest(BaseModel):
    """自然语言记录解析请求（Java → Python）"""

    user_id: int = Field(..., description="用户 ID")
    pet_id: int = Field(..., description="宠物 ID")
    message: str = Field(..., min_length=1, description="自然语言记录内容")


@router.post("/parse")
def parse_record(request: RecordParseRequest) -> dict:
    """
    自然语言记录解析：返回结构化记录信息 + 跳转页面 url，
    最终确认保存由用户在对应页面完成，AI 不越权直接写入。
    """
    logger.info("收到记录解析请求: user_id={}, pet_id={}", request.user_id, request.pet_id)
    raise HTTPException(
        status_code=status.HTTP_501_NOT_IMPLEMENTED,
        detail="自然语言记录随 P1 AI 助手实现",
    )
