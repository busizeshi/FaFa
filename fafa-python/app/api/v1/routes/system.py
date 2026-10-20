from fastapi import APIRouter, Request

from app.schemas.envelope import ApiResponse

router = APIRouter()


@router.get("/health", response_model=ApiResponse[dict[str, str]])
async def health(request: Request) -> ApiResponse[dict[str, str]]:
    return ApiResponse.success(
        data={"service": "fafa-python", "status": "UP"},
        request_id=request.state.request_id,
    )
