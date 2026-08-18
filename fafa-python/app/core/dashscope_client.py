"""
通义千问 API 客户端
"""
import dashscope
from dashscope import Generation, MultiModalConversation, TextEmbedding
from loguru import logger
from app.core.config import settings

# 设置 API Key
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


async def call_qwen_vision(image_url: str, prompt: str) -> dict:
    """
    调用通义千问视觉理解
    
    Args:
        image_url: 图片 URL
        prompt: 提示词
        
    Returns:
        识别结果（JSON）
    """
    try:
        messages = [
            {
                "role": "user",
                "content": [
                    {"image": image_url},
                    {"text": prompt}
                ]
            }
        ]
        
        response = MultiModalConversation.call(
            model=settings.AI_VISION_MODEL,
            messages=messages
        )
        
        if response.status_code == 200:
            content = response.output.choices[0].message.content
            return {"description": content}
        else:
            logger.error(f"视觉理解调用失败: {response}")
            raise Exception(f"API 调用失败: {response.message}")
            
    except Exception as e:
        logger.error(f"调用视觉理解失败: {e}")
        raise


async def generate_embedding(text: str) -> list:
    """
    生成文本 Embedding
    
    Args:
        text: 文本内容
        
    Returns:
        向量数组
    """
    try:
        response = TextEmbedding.call(
            model=settings.AI_EMBEDDING_MODEL,
            input=text
        )
        
        if response.status_code == 200:
            return response.output.embeddings[0].embedding
        else:
            logger.error(f"Embedding 生成失败: {response}")
            raise Exception(f"API 调用失败: {response.message}")
            
    except Exception as e:
        logger.error(f"生成 Embedding 失败: {e}")
        raise
