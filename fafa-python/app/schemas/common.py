"""
通用响应模型
"""

from typing import Any, Optional

from pydantic import BaseModel


class ApiResponse(BaseModel):
    """统一响应结构（与 fafa-java Result 对齐）"""

    code: int = 0
    message: str = "success"
    data: Optional[Any] = None
