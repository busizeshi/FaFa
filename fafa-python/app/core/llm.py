"""
模型路由层（唯一模型调用入口）

业务代码只声明「用途」（Purpose），不直接写模型名；
模型名与参数配置在环境变量/.env 中维护，换模型不改代码。
所有 Qwen 系列模型经百炼 OpenAI 兼容接口调用。
"""

from enum import Enum

from langchain_openai import ChatOpenAI, OpenAIEmbeddings
from loguru import logger

from app.core.config import get_settings


class Purpose(str, Enum):
    """模型用途枚举"""

    CHAT_LIGHT = "chat_light"            # 轻量对话 / 意图识别 / 标签生成
    CHAT_DEEP = "chat_deep"              # 复杂推理 / Agent 编排 / 报告生成
    VISION = "vision"                    # 图像理解（照片描述、包装识别、病历 OCR）
    EMBED_MULTIMODAL = "embed_multimodal"  # 多模态嵌入（照片与三视图，1024 维）
    EMBED_TEXT = "embed_text"            # 文本嵌入（事件、日记）
    VIDEO_GEN = "video_gen"              # 视频生成（预留 MiniMax H3 扩展点）


class NotEnabledError(Exception):
    """能力未启用异常（如视频生成暂不接入）"""


def _model_name(purpose: Purpose) -> str:
    """用途 -> 模型名映射"""
    settings = get_settings()
    mapping = {
        Purpose.CHAT_LIGHT: settings.model_chat_light,
        Purpose.CHAT_DEEP: settings.model_chat_deep,
        Purpose.VISION: settings.model_vision,
        Purpose.EMBED_MULTIMODAL: settings.model_embed_multimodal,
        Purpose.EMBED_TEXT: settings.model_embed_text,
    }
    return mapping[purpose]


def get_chat_model(purpose: Purpose) -> ChatOpenAI:
    """
    获取对话模型客户端

    Args:
        purpose: 模型用途

    Returns:
        ChatOpenAI 客户端（直连百炼 OpenAI 兼容接口）

    Raises:
        NotEnabledError: 用途对应的模型未启用（如 VIDEO_GEN）
    """
    if purpose == Purpose.VIDEO_GEN:
        # 预留 MiniMax H3 第三方 API provider 扩展点，当前直接拒绝
        raise NotEnabledError("视频生成能力暂未接入，预留 MiniMax H3 扩展点")

    settings = get_settings()
    logger.info("获取对话模型: purpose={}, model={}", purpose.value, _model_name(purpose))
    return ChatOpenAI(
        model=_model_name(purpose),
        api_key=settings.dashscope_api_key,
        base_url=settings.dashscope_base_url,
    )


def get_embedding_model(purpose: Purpose) -> OpenAIEmbeddings:
    """
    获取文本嵌入模型客户端

    注意：EMBED_MULTIMODAL（qwen3-vl-embedding）的多模态调用路径
    需在照片链路开发时对照百炼文档核实（OpenAI 兼容 /embeddings
    端点是否支持图片输入），届时在此扩展。

    Raises:
        NotEnabledError: 用途对应的模型未启用
    """
    if purpose == Purpose.VIDEO_GEN:
        raise NotEnabledError("视频生成能力暂未接入")

    settings = get_settings()
    return OpenAIEmbeddings(
        model=_model_name(purpose),
        api_key=settings.dashscope_api_key,
        base_url=settings.dashscope_base_url,
    )
