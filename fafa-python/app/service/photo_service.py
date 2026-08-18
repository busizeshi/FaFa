"""
照片分析服务
"""
import asyncio
from typing import List, Dict, Any, Optional
from loguru import logger
from dashscope import MultiModalConversation
import dashscope

from app.core.config import settings
from app.core.database import get_async_session
from app.core.qdrant import qdrant_client
from app.repository.photo_repository import PhotoRepository
from sqlalchemy import text


class PhotoAnalysisService:
    """照片分析服务"""
    
    def __init__(self):
        self.photo_repo = PhotoRepository()
        dashscope.api_key = settings.DASHSCOPE_API_KEY
    
    async def analyze_photo(self, photo_id: int, pet_id: int, url: str):
        """
        分析照片
        
        Args:
            photo_id: 照片ID
            pet_id: 宠物ID
            url: 照片URL
        """
        try:
            # 1. 调用 qwen-vl-plus 分析照片
            analysis_result = await self._call_qwen_vl(url)
            
            if not analysis_result:
                logger.error(f"照片分析失败: photoId={photo_id}")
                return
            
            # 2. 生成 Embedding
            embedding = await self._generate_embedding(analysis_result['description'])
            
            if not embedding:
                logger.error(f"生成 Embedding 失败: photoId={photo_id}")
                return
            
            # 3. 存入 Qdrant
            embedding_id = f"photo_{photo_id}"
            await self._save_to_qdrant(
                embedding_id=embedding_id,
                vector=embedding,
                payload={
                    'photo_id': photo_id,
                    'pet_id': pet_id,
                    'url': url,
                    'description': analysis_result['description'],
                    'scene': analysis_result.get('scene', ''),
                    'behavior': analysis_result.get('behavior', ''),
                    'objects': analysis_result.get('objects', [])
                }
            )
            
            # 4. 更新 MySQL photo 表
            await self._update_photo_ai_result(
                photo_id=photo_id,
                ai_tags=analysis_result.get('objects', []),
                ai_description=analysis_result['description'],
                embedding_id=embedding_id
            )
            
            logger.info(f"照片分析完成: photoId={photo_id}, embeddingId={embedding_id}")
            
        except Exception as e:
            logger.error(f"照片分析失败: photoId={photo_id}, error={e}", exc_info=True)
            raise
    
    async def _call_qwen_vl(self, image_url: str) -> Optional[Dict[str, Any]]:
        """
        调用 qwen-vl-plus 分析照片
        
        Args:
            image_url: 图片URL
            
        Returns:
            分析结果字典
        """
        try:
            messages = [
                {
                    'role': 'user',
                    'content': [
                        {'image': image_url},
                        {'text': '请详细描述这张宠物照片，包括：1. 场景（室内/室外/具体位置）；2. 宠物的行为和状态；3. 周围的物品或环境特征。请用简洁的语言描述。'}
                    ]
                }
            ]
            
            # 调用通义千问视觉理解模型
            response = MultiModalConversation.call(
                model='qwen-vl-plus',
                messages=messages
            )
            
            if response.status_code == 200:
                ai_description = response.output.choices[0].message.content[0]['text']
                
                # 简单解析结果（实际可能需要更复杂的解析逻辑）
                result = {
                    'description': ai_description,
                    'scene': self._extract_scene(ai_description),
                    'behavior': self._extract_behavior(ai_description),
                    'objects': self._extract_objects(ai_description)
                }
                
                logger.info(f"qwen-vl-plus 分析成功: {image_url}")
                return result
            else:
                logger.error(f"qwen-vl-plus 调用失败: {response.code}, {response.message}")
                return None
                
        except Exception as e:
            logger.error(f"调用 qwen-vl-plus 失败: {e}", exc_info=True)
            return None
    
    def _extract_scene(self, description: str) -> str:
        """从描述中提取场景"""
        # 简单的关键词匹配
        if '室内' in description or '家里' in description or '房间' in description:
            return '室内'
        elif '室外' in description or '户外' in description or '公园' in description:
            return '室外'
        else:
            return '未知'
    
    def _extract_behavior(self, description: str) -> str:
        """从描述中提取行为"""
        behaviors = ['睡觉', '吃饭', '玩耍', '坐着', '站着', '躺着', '跑步', '跳跃']
        for behavior in behaviors:
            if behavior in description:
                return behavior
        return '未知'
    
    def _extract_objects(self, description: str) -> List[str]:
        """从描述中提取物品标签"""
        # 简单的关键词提取
        keywords = ['猫', '狗', '猫粮', '狗粮', '玩具', '猫窝', '狗窝', '水碗', '食盆', 
                   '沙发', '床', '地板', '窗户', '阳台', '草地', '树木']
        objects = [kw for kw in keywords if kw in description]
        return objects[:5]  # 最多返回5个标签
    
    async def _generate_embedding(self, text: str) -> Optional[List[float]]:
        """
        生成文本 Embedding
        
        Args:
            text: 文本内容
            
        Returns:
            向量列表
        """
        try:
            from dashscope import TextEmbedding
            
            response = TextEmbedding.call(
                model=TextEmbedding.Models.text_embedding_v2,
                input=text
            )
            
            if response.status_code == 200:
                embedding = response.output['embeddings'][0]['embedding']
                logger.info(f"生成 Embedding 成功，维度: {len(embedding)}")
                return embedding
            else:
                logger.error(f"生成 Embedding 失败: {response.code}, {response.message}")
                return None
                
        except Exception as e:
            logger.error(f"生成 Embedding 失败: {e}", exc_info=True)
            return None
    
    async def _save_to_qdrant(self, embedding_id: str, vector: List[float], payload: Dict[str, Any]):
        """
        保存向量到 Qdrant
        
        Args:
            embedding_id: 向量ID
            vector: 向量数据
            payload: 附加数据
        """
        try:
            from qdrant_client.models import PointStruct
            
            await qdrant_client.upsert(
                collection_name='pet_photos',
                points=[
                    PointStruct(
                        id=embedding_id,
                        vector=vector,
                        payload=payload
                    )
                ]
            )
            
            logger.info(f"向量保存成功: embeddingId={embedding_id}")
            
        except Exception as e:
            logger.error(f"保存向量失败: {e}", exc_info=True)
            raise
    
    async def _update_photo_ai_result(self, photo_id: int, ai_tags: List[str], 
                                     ai_description: str, embedding_id: str):
        """
        更新照片的 AI 分析结果
        
        Args:
            photo_id: 照片ID
            ai_tags: AI标签
            ai_description: AI描述
            embedding_id: 向量ID
        """
        try:
            import json
            
            async with get_async_session() as session:
                sql = text("""
                    UPDATE photo 
                    SET ai_tags = :ai_tags,
                        ai_description = :ai_description,
                        embedding_id = :embedding_id,
                        updated_at = NOW()
                    WHERE id = :photo_id
                """)
                
                await session.execute(sql, {
                    'ai_tags': json.dumps(ai_tags, ensure_ascii=False),
                    'ai_description': ai_description,
                    'embedding_id': embedding_id,
                    'photo_id': photo_id
                })
                
                await session.commit()
                
            logger.info(f"更新照片 AI 结果成功: photoId={photo_id}")
            
        except Exception as e:
            logger.error(f"更新照片 AI 结果失败: {e}", exc_info=True)
            raise
