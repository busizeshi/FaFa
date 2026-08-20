"""
数据迁移脚本: 从旧的 qwen-vl-plus 向量迁移到 qwen3-vl-embedding

使用场景:
1. 清理旧的 Qdrant 集合 (pet_photos)
2. 重建 photo 表中的 AI 相关字段
3. 清理 pet 表中的旧向量数据

注意: 本脚本只清理旧数据，不迁移数据
新数据将在用户重新上传照片/视频时自动生成
"""
import asyncio
import sys
from pathlib import Path

# 添加项目根目录到路径
sys.path.insert(0, str(Path(__file__).parent.parent))

from loguru import logger
from sqlalchemy import text
from qdrant_client import QdrantClient

from app.core.config import settings
from app.core.database import get_async_session
from app.core.qdrant import qdrant_client


async def clean_old_qdrant_collections():
    """
    清理旧的 Qdrant 集合
    
    删除旧集合: pet_photos (基于 qwen-vl-plus)
    保留新集合: fafa_media, fafa_pet_profiles (基于 qwen3-vl-embedding)
    """
    try:
        logger.info("=== 开始清理旧的 Qdrant 集合 ===")
        
        # 获取所有集合
        collections = qdrant_client.get_collections().collections
        collection_names = [c.name for c in collections]
        
        logger.info(f"当前集合: {collection_names}")
        
        # 删除旧集合
        old_collections = ['pet_photos', 'photos', 'embeddings']
        
        for old_collection in old_collections:
            if old_collection in collection_names:
                logger.info(f"删除旧集合: {old_collection}")
                qdrant_client.delete_collection(collection_name=old_collection)
                logger.info(f"✓ 已删除: {old_collection}")
            else:
                logger.info(f"旧集合不存在，跳过: {old_collection}")
        
        logger.info("=== 旧 Qdrant 集合清理完成 ===")
        
    except Exception as e:
        logger.error(f"清理 Qdrant 集合失败: {e}", exc_info=True)
        raise


async def reset_photo_ai_fields():
    """
    重置 photo 表中的 AI 相关字段
    
    将以下字段重置为默认值:
    - embedding_id: NULL
    - auto_recognized: 0
    - recognition_confidence: NULL
    - recognized_pet_ids: NULL
    """
    try:
        logger.info("=== 开始重置 photo 表 AI 字段 ===")
        
        async with get_async_session() as session:
            # 统计需要重置的记录数
            count_sql = text("""
                SELECT COUNT(*) 
                FROM photo 
                WHERE embedding_id IS NOT NULL 
                   OR auto_recognized = 1
                   OR recognition_confidence IS NOT NULL
                   OR recognized_pet_ids IS NOT NULL
            """)
            
            result = await session.execute(count_sql)
            count = result.scalar()
            
            logger.info(f"需要重置的照片数量: {count}")
            
            if count == 0:
                logger.info("没有需要重置的数据")
                return
            
            # 重置字段
            reset_sql = text("""
                UPDATE photo 
                SET 
                    embedding_id = NULL,
                    auto_recognized = 0,
                    recognition_confidence = NULL,
                    recognized_pet_ids = NULL
                WHERE 
                    embedding_id IS NOT NULL 
                    OR auto_recognized = 1
                    OR recognition_confidence IS NOT NULL
                    OR recognized_pet_ids IS NOT NULL
            """)
            
            await session.execute(reset_sql)
            await session.commit()
            
            logger.info(f"✓ 已重置 {count} 条照片记录")
        
        logger.info("=== photo 表 AI 字段重置完成 ===")
        
    except Exception as e:
        logger.error(f"重置 photo 表失败: {e}", exc_info=True)
        raise


async def reset_pet_profile_fields():
    """
    重置 pet 表中的三视图相关字段
    
    将以下字段重置为默认值:
    - front_view_url: NULL
    - side_view_url: NULL
    - top_view_url: NULL
    - profile_embedding_id: NULL
    """
    try:
        logger.info("=== 开始重置 pet 表三视图字段 ===")
        
        async with get_async_session() as session:
            # 统计需要重置的记录数
            count_sql = text("""
                SELECT COUNT(*) 
                FROM pet 
                WHERE front_view_url IS NOT NULL 
                   OR side_view_url IS NOT NULL
                   OR top_view_url IS NOT NULL
                   OR profile_embedding_id IS NOT NULL
            """)
            
            result = await session.execute(count_sql)
            count = result.scalar()
            
            logger.info(f"需要重置的宠物数量: {count}")
            
            if count == 0:
                logger.info("没有需要重置的数据")
                return
            
            # 重置字段
            reset_sql = text("""
                UPDATE pet 
                SET 
                    front_view_url = NULL,
                    side_view_url = NULL,
                    top_view_url = NULL,
                    profile_embedding_id = NULL
                WHERE 
                    front_view_url IS NOT NULL 
                    OR side_view_url IS NOT NULL
                    OR top_view_url IS NOT NULL
                    OR profile_embedding_id IS NOT NULL
            """)
            
            await session.execute(reset_sql)
            await session.commit()
            
            logger.info(f"✓ 已重置 {count} 条宠物记录")
        
        logger.info("=== pet 表三视图字段重置完成 ===")
        
    except Exception as e:
        logger.error(f"重置 pet 表失败: {e}", exc_info=True)
        raise


async def verify_new_collections():
    """
    验证新 Qdrant 集合是否正确创建
    """
    try:
        logger.info("=== 验证新 Qdrant 集合 ===")
        
        collections = qdrant_client.get_collections().collections
        collection_names = [c.name for c in collections]
        
        required_collections = [
            settings.QDRANT_COLLECTION_MEDIA,
            settings.QDRANT_COLLECTION_PET_PROFILES
        ]
        
        for collection_name in required_collections:
            if collection_name in collection_names:
                # 获取集合信息
                collection_info = qdrant_client.get_collection(collection_name)
                vector_size = collection_info.config.params.vectors.size
                
                logger.info(f"✓ 集合存在: {collection_name} (向量维度: {vector_size})")
                
                if vector_size != settings.QWEN3_VL_EMBEDDING_DIM:
                    logger.warning(f"⚠ 向量维度不匹配: 期望 {settings.QWEN3_VL_EMBEDDING_DIM}, 实际 {vector_size}")
            else:
                logger.error(f"✗ 集合不存在: {collection_name}")
        
        logger.info("=== 新 Qdrant 集合验证完成 ===")
        
    except Exception as e:
        logger.error(f"验证 Qdrant 集合失败: {e}", exc_info=True)
        raise


async def main():
    """
    主函数: 执行完整的数据迁移流程
    """
    try:
        logger.info("=" * 60)
        logger.info("开始数据迁移: qwen-vl-plus -> qwen3-vl-embedding")
        logger.info("=" * 60)
        
        # 1. 清理旧的 Qdrant 集合
        await clean_old_qdrant_collections()
        
        # 2. 重置 photo 表 AI 字段
        await reset_photo_ai_fields()
        
        # 3. 重置 pet 表三视图字段
        await reset_pet_profile_fields()
        
        # 4. 验证新集合
        await verify_new_collections()
        
        logger.info("=" * 60)
        logger.info("数据迁移完成！")
        logger.info("=" * 60)
        logger.info("")
        logger.info("后续步骤:")
        logger.info("1. 重启 Python 服务以初始化新的 Qdrant 集合")
        logger.info("2. 用户重新上传照片/视频，系统将自动使用 qwen3-vl-embedding 生成向量")
        logger.info("3. 用户上传宠物三视图照片以启用自动识别功能")
        logger.info("")
        
    except Exception as e:
        logger.error(f"数据迁移失败: {e}", exc_info=True)
        sys.exit(1)


if __name__ == "__main__":
    asyncio.run(main())
