"""
FaFa 宠物生活助手 - Python AI 服务

主应用入口
"""
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from loguru import logger

from app.core.config import settings
from app.api import chat, image, vector

# 创建 FastAPI 应用
app = FastAPI(
    title="FaFa AI Service",
    description="FaFa 宠物生活助手 AI 服务",
    version="1.0.0",
)

# CORS 配置
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 注册路由
app.include_router(chat.router, prefix="/api/ai/chat", tags=["AI 对话"])
app.include_router(image.router, prefix="/api/ai/image", tags=["图片识别"])
app.include_router(vector.router, prefix="/api/ai/vector", tags=["向量搜索"])


@app.on_event("startup")
async def startup_event():
    """应用启动事件"""
    logger.info("FaFa AI 服务启动中...")
    logger.info(f"环境: {settings.ENVIRONMENT}")
    logger.info(f"模型: {settings.AI_MODEL}")


@app.on_event("shutdown")
async def shutdown_event():
    """应用关闭事件"""
    logger.info("FaFa AI 服务关闭")


@app.get("/")
async def root():
    """健康检查"""
    return {
        "service": "FaFa AI Service",
        "status": "running",
        "version": "1.0.0",
    }


@app.get("/health")
async def health_check():
    """健康检查接口"""
    return {"status": "healthy"}


if __name__ == "__main__":
    import uvicorn
    
    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=8000,
        reload=True,
    )
