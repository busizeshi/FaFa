"""DashScope 多模态嵌入客户端。"""

import asyncio
import base64
import ipaddress
from typing import List
from urllib.parse import urlparse

import dashscope
import httpx
from dashscope import MultiModalEmbedding
from loguru import logger

from app.core.config import get_settings
from app.core.qdrant import VECTOR_DIM


class VideoEmbeddingInputError(ValueError):
    """视频 Embedding 输入不满足百炼 URL 约束。"""


def is_private_url(url: str) -> bool:
    """判断 URL 主机是否内网地址（DashScope 云端不可达）"""
    host = urlparse(url).hostname or ""
    if host == "localhost" or host.startswith("127."):
        return True
    try:
        return ipaddress.ip_address(host).is_private
    except ValueError:
        # 非 IP 主机名（如 OSS 域名）默认公网可达
        return False


async def _download_as_data_uri(url: str) -> str:
    """下载图片并转为 Base64 Data URI（内网图片转传云端）"""
    async with httpx.AsyncClient(timeout=30) as client:
        resp = await client.get(url)
        resp.raise_for_status()
    content_type = resp.headers.get("content-type", "image/jpeg").split(";")[0]
    data = base64.b64encode(resp.content).decode()
    return f"data:{content_type};base64,{data}"


async def prepare_image_for_dashscope(image_url: str) -> str:
    """将内网图片转为百炼可读取的 Data URI，公网地址原样返回。"""
    if is_private_url(image_url):
        logger.info("内网图片转 Base64 上传: url={}", image_url)
        return await _download_as_data_uri(image_url)
    return image_url


async def get_multimodal_embedding(image_url: str) -> List[float]:
    """为图片生成可用于跨模态检索的融合向量。"""
    url = await prepare_image_for_dashscope(image_url)
    return await _call_multimodal_embedding([{"image": url}], "image")


async def get_video_multimodal_embedding(video_url: str) -> List[float]:
    """为视频生成与图片、文本同空间的融合向量。"""
    parsed = urlparse(video_url.strip())
    if parsed.scheme not in {"http", "https"} or not parsed.netloc:
        raise VideoEmbeddingInputError("视频向量化只接受有效的 HTTP(S) URL")
    if is_private_url(video_url):
        raise VideoEmbeddingInputError(
            "视频向量化需要百炼可访问的公网 URL，当前视频地址属于内网地址"
        )
    settings = get_settings()
    return await _call_multimodal_embedding(
        [{"video": video_url}],
        "video",
        fps=settings.video_embedding_fps,
    )


async def get_text_multimodal_embedding(text: str) -> List[float]:
    """为文本查询生成与图片向量同空间的融合向量。"""
    if not text.strip():
        raise ValueError("文本嵌入输入不能为空")
    return await _call_multimodal_embedding([{"text": text}], "text")


async def _call_multimodal_embedding(
    input_data: list[dict[str, str]],
    input_type: str,
    *,
    fps: float | None = None,
) -> List[float]:
    """调用 DashScope 原生多模态接口并校验 Qdrant 向量维度。"""
    settings = get_settings()
    dashscope.api_key = settings.dashscope_api_key
    logger.info(
        "调用多模态融合嵌入: model={}, input_type={}, dimension={}",
        settings.model_embed_multimodal,
        input_type,
        VECTOR_DIM,
    )

    call_kwargs = {
        "model": settings.model_embed_multimodal,
        "input": input_data,
        "dimension": VECTOR_DIM,
        "enable_fusion": True,
    }
    if fps is not None:
        call_kwargs["fps"] = fps
    result = await asyncio.to_thread(MultiModalEmbedding.call, **call_kwargs)
    if result.status_code != 200:
        message = getattr(result, "message", str(result))
        raise RuntimeError(f"DashScope 多模态嵌入失败: {message}")

    try:
        raw_embedding = result.output["embeddings"][0]["embedding"]
        embedding = [float(value) for value in raw_embedding]
    except (KeyError, IndexError, TypeError, ValueError) as exc:
        raise RuntimeError("DashScope 多模态嵌入响应格式无效") from exc
    if len(embedding) != VECTOR_DIM:
        raise RuntimeError(
            f"DashScope 向量维度不匹配: expected={VECTOR_DIM}, actual={len(embedding)}"
        )

    logger.info("多模态融合嵌入成功: dim={}", len(embedding))
    return embedding
