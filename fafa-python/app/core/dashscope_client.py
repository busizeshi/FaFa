"""
通义千问 API 客户端 (基于 qwen3-vl-embedding 多模态向量化)
"""
import dashscope
from dashscope import Generation, MultiModalEmbedding
from loguru import logger
from app.core.config import settings
from typing import Union, List

dashscope.api_key = settings.DASHSCOPE_API_KEY


async def call_qwen_text(prompt: str, model: str = None) -> str:
    """
    调用通义千问文本生成
    
    Args:
        prompt: 提示词
        model: 模型名称，默认使用配置中的模型
        
    Returns:
        生成的文本
    """
    if model is None:
        model = settings.AI_MODEL
        
    try:
        response = Generation.call(
            model=model,
            prompt=prompt,
            result_format='message'
        )
        
        if response.status_code == 200:
            return response.output.choices[0].message.content
        else:
            logger.error(f"通义千问调用失败: {response}")
            raise Exception(f"API 调用失败: {response.message}")
            
    except Exception as e:
        logger.error(f"调用通义千问失败: {e}")
        raise


async def generate_multimodal_embedding(
    inputs: Union[str, dict],
    embedding_dim: int = None
) -> List[float]:
    """
    使用 qwen3-vl-embedding 生成多模态向量
    
    支持的输入类型:
    - 文本: 直接传字符串
    - 图片: 传 {"image": "图片URL或base64"}
    - 视频: 传 {"video": "视频URL"}
    
    Args:
        inputs: 输入内容 (文本、图片URL、视频URL)
        embedding_dim: 向量维度，默认使用配置中的维度
        
    Returns:
        向量数组
    """
    if embedding_dim is None:
        embedding_dim = settings.QWEN3_VL_EMBEDDING_DIM
        
    try:
        if isinstance(inputs, str):
            input_data = {"text": inputs}
        elif isinstance(inputs, dict):
            input_data = inputs
        else:
            raise ValueError(f"不支持的输入类型: {type(inputs)}")
        
        response = MultiModalEmbedding.call(
            model=settings.AI_MULTIMODAL_EMBEDDING_MODEL,
            input=input_data,
            dimension=embedding_dim
        )
        
        if response.status_code == 200:
            return response.output.embeddings[0].embedding
        else:
            logger.error(f"多模态向量化失败: {response}")
            raise Exception(f"API 调用失败: {response.message}")
            
    except Exception as e:
        logger.error(f"生成多模态向量失败: {e}")
        raise


async def generate_text_embedding(text: str) -> List[float]:
    """
    生成纯文本向量 (使用 qwen3-vl-embedding 的文本模式)
    
    Args:
        text: 文本内容
        
    Returns:
        向量数组
    """
    return await generate_multimodal_embedding(text)


async def generate_image_embedding(image_url: str, embedding_dim: int = None) -> List[float]:
    """
    生成图片向量
    
    Args:
        image_url: 图片 URL
        embedding_dim: 向量维度
        
    Returns:
        向量数组
    """
    return await generate_multimodal_embedding({"image": image_url}, embedding_dim)


async def generate_video_embedding(video_url: str, embedding_dim: int = None) -> List[float]:
    """
    生成视频向量
    
    Args:
        video_url: 视频 URL
        embedding_dim: 向量维度
        
    Returns:
        向量数组
    """
    return await generate_multimodal_embedding({"video": video_url}, embedding_dim)


async def batch_generate_embeddings(
    inputs_list: List[Union[str, dict]],
    embedding_dim: int = None
) -> List[List[float]]:
    """
    批量生成向量
    
    Args:
        inputs_list: 输入列表
        embedding_dim: 向量维度
        
    Returns:
        向量列表
    """
    embeddings = []
    for inputs in inputs_list:
        embedding = await generate_multimodal_embedding(inputs, embedding_dim)
        embeddings.append(embedding)
    return embeddings

