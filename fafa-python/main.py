"""
FaFa 宠物小程序后端服务入口
"""
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from loguru import logger

from app.api import chat, image, pet, photo, record, reminder, vector

app = FastAPI(
    title="FaFa 宠物小程序 API",
    description="基于 FastAPI + LangChain 的智能宠物管理系统",
    version="1.0.0"
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


@app.on_event("startup")
async def startup_event():
    """启动事件"""
    logger.info("FaFa 宠物小程序后端服务启动")


@app.on_event("shutdown")
async def shutdown_event():
    """关闭事件"""
    logger.info("FaFa 宠物小程序后端服务关闭")


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
