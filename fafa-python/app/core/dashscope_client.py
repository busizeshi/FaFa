"""
DashScope 多模态嵌入客户端

通过阿里云百炼 DashScope SDK 调用 qwen3-vl-embedding 模型，
对图片 URL 生成 1024 维向量，用于宠物三视图向量化与后续个体识别。

关键点：
1. SDK 的 input 直接传列表（如 [{'image': url}]），与旧版 {'contents': [...]} 写法不同；
2. 模型默认输出 2560 维，必须显式传 dimension 与 Qdrant 集合维度保持一致；
3. DashScope 由云端拉取图片，URL 必须公网可达；内网地址（开发环境 MinIO）
   先在本地下载并转 Base64 Data URI 后上传。
"""

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


def _is_private_url(url: str) -> bool:
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
    if _is_private_url(image_url):
        logger.info("内网图片转 Base64 上传: url={}", image_url)
        return await _download_as_data_uri(image_url)
    return image_url


async def get_multimodal_embedding(image_url: str) -> List[float]:
    """
    对图片 URL 生成多模态向量

    使用 qwen3-vl-embedding 模型，输出维度与 Qdrant 集合一致（1024 维）。

    Args:
        image_url: 图片 URL（MinIO/OSS 地址）

    Returns:
        1024 维浮点向量

    Raises:
        RuntimeError: DashScope API 调用失败
    """
    settings = get_settings()
    dashscope.api_key = settings.dashscope_api_key

    # 内网地址云端拉不到，先本地下载转 Base64
    url = image_url
    url = await prepare_image_for_dashscope(url)

    logger.info("调用多模态嵌入: model={}, image_url={}", settings.model_embed_multimodal, image_url)

    try:
        # SDK 为同步阻塞调用，放线程池避免卡住 FastAPI 事件循环
        result = await asyncio.to_thread(
            MultiModalEmbedding.call,
            model=settings.model_embed_multimodal,
            input=[{"image": url}],
            dimension=VECTOR_DIM,
        )

        if result.status_code != 200:
            logger.error(
                "多模态嵌入失败: code={}, message={}",
                result.code if hasattr(result, "code") else "unknown",
                result.message if hasattr(result, "message") else str(result),
            )
            raise RuntimeError(
                f"DashScope 多模态嵌入失败: {result.message if hasattr(result, 'message') else result}"
            )

        embedding = result.output["embeddings"][0]["embedding"]
        logger.info("多模态嵌入成功: dim={}", len(embedding))
        return embedding

    except Exception as e:
        if isinstance(e, RuntimeError):
            raise
        logger.error("多模态嵌入异常: image_url={}, error={}", image_url, str(e))
        raise RuntimeError(f"多模态嵌入异常: {e}") from e
