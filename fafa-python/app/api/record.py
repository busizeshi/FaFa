"""自然语言记录解析路由。"""

from fastapi import APIRouter
from loguru import logger

from app.schemas.chat import RecordParseRequest, RecordParseResponse
from app.service.record_parser import parse_record_message


router = APIRouter(prefix="/api/records", tags=["records"])


@router.post("/parse")
async def parse_record(request: RecordParseRequest) -> RecordParseResponse:
    """返回结构化记录草稿，不直接写 Java 业务表。"""

    logger.info("收到记录解析请求: user_id={}, pet_id={}", request.user_id, request.pet_id)
    return RecordParseResponse.model_validate(parse_record_message(request.pet_id, request.message))
