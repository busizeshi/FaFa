"""
FaFa 宠物小程序后端服务入口
"""
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from loguru import logger

from app.api import chat, image, pet, photo, record, reminder, vector, callback

# 注意：RocketMQ Python 客户端不支持 Windows
# 在 Linux 环境下取消下面的注释
# from app.consumer.photo_consumer import start_photo_consumer, stop_photo_consumer


@asynccontextmanager
async def lifespan(app: FastAPI):
    """应用生命周期管理"""
    logger.info("FaFa 宠物小程序后端服务启动")
    
    # 在 Linux 环境下取消下面的注释以启动 RocketMQ 消费者
    # try:
    #     start_photo_consumer()
    #     logger.info("RocketMQ 消费者启动成功")
    # except Exception as e:
    #     logger.error(f"RocketMQ 消费者启动失败: {e}")
    
    yield
    
    # 在 Linux 环境下取消下面的注释以停止 RocketMQ 消费者
    # try:
    #     stop_photo_consumer()
    #     logger.info("RocketMQ 消费者已关闭")
    # except Exception as e:
    #     logger.error(f"RocketMQ 消费者关闭失败: {e}")
    
    logger.info("FaFa 宠物小程序后端服务关闭")


app = FastAPI(
    title="FaFa 宠物小程序 API",
    description="基于 FastAPI + LangChain 的智能宠物管理系统",
    version="1.0.0",
    lifespan=lifespan
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(chat.router, prefix="/api/chat", tags=["对话"])
app.include_router(image.router, prefix="/api/image", tags=["图像"])
app.include_router(pet.router, prefix="/api/pet", tags=["宠物"])
app.include_router(photo.router, prefix="/api/photo", tags=["照片"])
app.include_router(record.router, prefix="/api/record", tags=["记录"])
app.include_router(reminder.router, prefix="/api/reminder", tags=["提醒"])
app.include_router(vector.router, prefix="/api/vector", tags=["向量"])
app.include_router(callback.router, prefix="/api/callback", tags=["回调"])


@app.get("/")
async def root():
    """根路径"""
    return {
        "message": "FaFa 宠物小程序 API",
        "version": "1.0.0",
        "docs": "/docs"
    }


@app.get("/health")
async def health_check():
    """健康检查"""
    return {"status": "ok"}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=8000,
        reload=True,
        log_level="info"
    )
