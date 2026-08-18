"""
日志配置
"""
import sys
from loguru import logger


def setup_logging():
    """配置日志"""
    
    # 移除默认配置
    logger.remove()
    
    # 控制台输出
    logger.add(
        sys.stdout,
        format="<green>{time:YYYY-MM-DD HH:mm:ss.SSS}</green> | "
               "<level>{level: <8}</level> | "
               "<cyan>{name}</cyan>:<cyan>{function}</cyan>:<cyan>{line}</cyan> - "
               "<level>{message}</level>",
        level="DEBUG",
    )
    
    # 文件输出
    logger.add(
        "logs/fafa-python.log",
        rotation="100 MB",
        retention="30 days",
        compression="zip",
        format="{time:YYYY-MM-DD HH:mm:ss.SSS} | {level: <8} | {name}:{function}:{line} - {message}",
        level="INFO",
    )


# 初始化日志
setup_logging()
