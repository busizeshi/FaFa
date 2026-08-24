"""周期报告内部生成接口。"""

from fastapi import APIRouter

from app.schemas.report import ReportGenerationRequest, ReportGenerationResponse
from app.service.report_generation_service import report_generation_service

router = APIRouter(prefix="/api/reports", tags=["reports"])


@router.post("/generate", response_model=ReportGenerationResponse)
async def generate_report(request: ReportGenerationRequest) -> ReportGenerationResponse:
    return ReportGenerationResponse.model_validate(await report_generation_service.generate(request))
