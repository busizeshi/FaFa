"""fafa-java 内部回调客户端。"""

import asyncio
from decimal import Decimal
from typing import Any

import httpx
from loguru import logger

from app.core.config import get_settings


async def send_photo_analysis_callback(
    *,
    media_id: int,
    success: bool,
    recognized_pet_ids: list[int] | None = None,
    recognized_pet_id: int | None = None,
    recognition_score: float | Decimal | None = None,
    error_message: str | None = None,
    trace_id: str | None = None,
) -> None:
    """回写素材分析结果，网络异常或 Java 5xx 按指数退避重试。"""
    settings = get_settings()
    url = f"{settings.java_base_url.rstrip('/')}/api/callback/photo-analysis"
    payload: dict[str, Any] = {
        "media_id": media_id,
        "success": success,
        "recognized_pet_ids": recognized_pet_ids or ([] if recognized_pet_id is None else [recognized_pet_id]),
        "recognized_pet_id": recognized_pet_id,
        "recognition_score": float(recognition_score) if recognition_score is not None else None,
        "error_message": error_message,
    }
    headers = {"X-Internal-Token": settings.internal_token}
    if trace_id:
        headers["X-Trace-Id"] = trace_id

    last_error: Exception | None = None
    retry_count = max(settings.callback_retry, 1)
    for attempt in range(retry_count):
        try:
            async with httpx.AsyncClient(timeout=30) as client:
                response = await client.post(url, json=payload, headers=headers)
                response.raise_for_status()
            logger.info("素材分析回调成功: media_id={}, success={}, attempt={}", media_id, success, attempt + 1)
            return
        except httpx.HTTPStatusError as exc:
            last_error = exc
            # 4xx 通常是契约或数据问题，继续重试没有收益。
            if exc.response.status_code < 500:
                break
            logger.warning("素材分析回调收到服务端错误: media_id={}, status={}, attempt={}",
                           media_id, exc.response.status_code, attempt + 1)
        except httpx.HTTPError as exc:
            last_error = exc
            logger.warning("素材分析回调网络失败: media_id={}, attempt={}, error={}",
                           media_id, attempt + 1, str(exc))

        if attempt < retry_count - 1:
            await asyncio.sleep(2 ** attempt)

    if last_error is not None:
        logger.opt(exception=last_error).error("素材分析回调最终失败: media_id={}, success={}", media_id, success)
    else:
        logger.error("素材分析回调最终失败: media_id={}, success={}", media_id, success)
    raise RuntimeError(f"素材分析回调失败: media_id={media_id}") from last_error


async def send_pet_vectorization_callback(
    *,
    pet_id: int,
    success: bool,
    message: str | None = None,
    trace_id: str | None = None,
) -> bool:
    """回写宠物三视图向量化结果（POST /api/callback/pet-vectorization）。

    契约（snake_case，对应 Java PetVectorizationCallback）：pet_id / success / message。

    与素材分析回调不同，本函数不抛出异常，而是返回是否送达，由调用方决策：
    - 成功回调送达失败时，调用方抛出让 MQ 重试整条任务（嵌入幂等，重跑无副作用）；
    - 失败回调（success=False）在异常处理分支中尽力而为，不能掩盖原始异常。
    """
    settings = get_settings()
    url = f"{settings.java_base_url.rstrip('/')}/api/callback/pet-vectorization"
    payload: dict[str, Any] = {
        "pet_id": pet_id,
        "success": success,
        "message": message,
    }
    headers = {"X-Internal-Token": settings.internal_token}
    if trace_id:
        headers["X-Trace-Id"] = trace_id

    last_error: Exception | None = None
    retry_count = max(settings.callback_retry, 1)
    for attempt in range(retry_count):
        try:
            async with httpx.AsyncClient(timeout=30) as client:
                response = await client.post(url, json=payload, headers=headers)
                response.raise_for_status()
            logger.info("宠物向量化回调成功: pet_id={}, success={}, attempt={}", pet_id, success, attempt + 1)
            return True
        except httpx.HTTPStatusError as exc:
            last_error = exc
            # 4xx 通常是契约或数据问题，继续重试没有收益。
            if exc.response.status_code < 500:
                break
            logger.warning("宠物向量化回调收到服务端错误: pet_id={}, status={}, attempt={}",
                           pet_id, exc.response.status_code, attempt + 1)
        except httpx.HTTPError as exc:
            last_error = exc
            logger.warning("宠物向量化回调网络失败: pet_id={}, attempt={}, error={}",
                           pet_id, attempt + 1, str(exc))

        if attempt < retry_count - 1:
            await asyncio.sleep(2 ** attempt)

    if last_error is not None:
        logger.opt(exception=last_error).error("宠物向量化回调最终失败: pet_id={}, success={}", pet_id, success)
    else:
        logger.error("宠物向量化回调最终失败: pet_id={}, success={}", pet_id, success)
    return False
