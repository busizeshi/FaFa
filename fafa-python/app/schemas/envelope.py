from typing import Any, Generic, TypeVar

from pydantic import BaseModel, ConfigDict, Field

DataT = TypeVar("DataT")


class ApiResponse(BaseModel, Generic[DataT]):
    model_config = ConfigDict(populate_by_name=True)

    code: str
    message: str
    data: DataT | None = None
    request_id: str = Field(alias="requestId")

    @classmethod
    def success(cls, data: DataT, request_id: str) -> "ApiResponse[DataT]":
        return cls(code="0", message="OK", data=data, requestId=request_id)

    @classmethod
    def failure(
        cls,
        code: str,
        message: str,
        request_id: str,
        data: Any | None = None,
    ) -> "ApiResponse[Any]":
        return cls(code=code, message=message, data=data, requestId=request_id)
