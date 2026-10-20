import logging

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from starlette.exceptions import HTTPException as StarletteHTTPException

from app.middleware.request_id import current_request_id
from app.schemas.envelope import ApiResponse

logger = logging.getLogger(__name__)


def register_exception_handlers(app: FastAPI) -> None:
    @app.exception_handler(RequestValidationError)
    async def handle_validation_error(
        request: Request, exception: RequestValidationError
    ) -> JSONResponse:
        request_id = getattr(request.state, "request_id", None) or current_request_id() or "-"
        details = [
            {
                "field": ".".join(str(part) for part in error["loc"]),
                "message": error["msg"],
            }
            for error in exception.errors()
        ]
        body = ApiResponse.failure(
            "FAFA-40000", "Request parameters are invalid", request_id, details
        )
        return JSONResponse(status_code=400, content=body.model_dump(by_alias=True))

    @app.exception_handler(StarletteHTTPException)
    async def handle_http_error(
        request: Request, exception: StarletteHTTPException
    ) -> JSONResponse:
        request_id = getattr(request.state, "request_id", None) or current_request_id() or "-"
        if exception.status_code == 404:
            code, message = "FAFA-40400", "The requested resource was not found"
        elif exception.status_code == 401:
            code, message = "FAFA-40100", "Authentication is required"
        elif exception.status_code == 403:
            code, message = "FAFA-40300", "You do not have permission to perform this action"
        else:
            code, message = "FAFA-40000", "The request could not be completed"
        body = ApiResponse.failure(code, message, request_id)
        return JSONResponse(
            status_code=exception.status_code,
            content=body.model_dump(by_alias=True),
            headers=exception.headers,
        )

    @app.exception_handler(Exception)
    async def handle_unexpected_error(request: Request, exception: Exception) -> JSONResponse:
        request_id = getattr(request.state, "request_id", None) or current_request_id() or "-"
        logger.exception("Unhandled request failure, request_id=%s", request_id)
        body = ApiResponse.failure(
            "FAFA-50000", "An unexpected server error occurred", request_id
        )
        return JSONResponse(status_code=500, content=body.model_dump(by_alias=True))
