"""内容草稿内部生成接口。"""

from fastapi import APIRouter

from app.schemas.content import ContentGenerationRequest, ContentGenerationResponse
from app.service.content_generation_service import content_generation_service

router = APIRouter(prefix="/api/content", tags=["content"])


@router.post("/generate", response_model=ContentGenerationResponse)
async def generate_content(request: ContentGenerationRequest) -> ContentGenerationResponse:
    return ContentGenerationResponse.model_validate(await content_generation_service.generate(request))
