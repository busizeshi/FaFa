"""医疗摘要内部接口。"""

from fastapi import APIRouter
from loguru import logger

from app.schemas.medical import MedicalSummaryRequest, MedicalSummaryResponse
from app.service.medical_summary_service import MedicalSummaryService


router = APIRouter(prefix="/api/medical", tags=["medical"])
medical_summary_service = MedicalSummaryService()


@router.post("/summaries", response_model=MedicalSummaryResponse)
async def generate_summary(request: MedicalSummaryRequest) -> MedicalSummaryResponse:
    """查询 Java 事实并生成医疗摘要。"""

    logger.info("收到医疗摘要请求: user_id={}, pet_id={}", request.user_id, request.pet_id)
    return MedicalSummaryResponse.model_validate(await medical_summary_service.generate(request))
