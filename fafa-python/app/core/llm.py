"""模型路由层：业务代码只声明用途，不直接写模型名

所有模型调用收敛到本模块。模型名/参数在 config.py 配置，
换模型（如 qwen-turbo -> qwen3.7-flash）只改环境变量不改代码。
"""

from enum import Enum

from langchain_openai import ChatOpenAI, OpenAIEmbeddings

from app.core.config import settings


class Purpose(str, Enum):
    """模型用途枚举"""

    CHAT_LIGHT = "chat_light"        # 轻量对话/意图识别/标签生成
    CHAT_DEEP = "chat_deep"          # 复杂推理/Agent 编排/报告生成
    VISION = "vision"                # 图像/视频理解
    EMBED_TEXT = "embed_text"        # 文本嵌入
    EMBED_MULTIMODAL = "embed_multimodal"  # 多模态嵌入（图文）
    VIDEO_GEN = "video_gen"          # 视频生成（预留 MiniMax H3）


class VideoGenNotEnabledError(Exception):
    """视频生成能力暂未开放（后期接入 MiniMax H3 第三方 API）"""


def get_chat_model(purpose: Purpose, temperature: float | None = None) -> ChatOpenAI:
    """按用途获取对话模型（走百炼 OpenAI 兼容接口）"""
    if purpose is Purpose.VIDEO_GEN:
        raise VideoGenNotEnabledError("视频生成暂未开放，后期接入 MiniMax H3")

    model_name = {
        Purpose.CHAT_LIGHT: settings.model_chat_light,
        Purpose.CHAT_DEEP: settings.model_chat_deep,
        # qwen-vl 系列同样走兼容接口，消息中携带 image_url 即为多模态输入
        Purpose.VISION: settings.model_vision,
    }.get(purpose)
    if model_name is None:
        raise ValueError(f"用途 {purpose} 不是对话类用途")

    kwargs: dict = {"model": model_name}
    if temperature is not None:
        kwargs["temperature"] = temperature
    return ChatOpenAI(
        api_key=settings.dashscope_api_key,
        base_url=settings.dashscope_base_url,
        **kwargs,
    )


def get_embedding_model(purpose: Purpose = Purpose.EMBED_TEXT) -> OpenAIEmbeddings:
    """获取文本嵌入模型（多模态嵌入请使用 embed_multimodal）"""
    if purpose is Purpose.EMBED_MULTIMODAL:
        raise ValueError("多模态嵌入请调用 embed_multimodal()（依赖 dashscope 原生 SDK）")
    return OpenAIEmbeddings(
        api_key=settings.dashscope_api_key,
        base_url=settings.dashscope_base_url,
        model=settings.model_embed_text,
    )


def embed_multimodal(image_url: str | None = None, text: str | None = None) -> list[float]:
    """多模态嵌入（qwen3-vl-embedding，1024 维）

    走 dashscope 原生 SDK（OpenAI 兼容接口不覆盖多模态嵌入输入格式）。
    photo_service 实现视觉理解链路时填充真实调用。
    """
    import dashscope

    inputs: list[dict] = []
    if image_url:
        inputs.append({"image": image_url})
    if text:
        inputs.append({"text": text})
    if not inputs:
        raise ValueError("image_url 与 text 至少提供一项")

    resp = dashscope.MultiModalEmbedding.call(
        model=settings.model_embed_multimodal,
        input=inputs,
        api_key=settings.dashscope_api_key,
    )
    if resp.status_code != 200:
        raise RuntimeError(f"多模态嵌入调用失败: code={resp.status_code}, msg={resp.message}")
    return resp.output["embeddings"][0]["embedding"]
