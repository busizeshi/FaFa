"""照片/视频素材 API（骨架，M5 链路联调时补实现）

服务间契约（与 fafa-java PythonAiClient 逐字节对齐，变更须双侧同步）：
- POST /api/photos/analyze   媒体理解与向量化
- POST /api/photos/search    语义检索
"""

from fastapi import APIRouter, HTTPException, status

from app.schemas.photo import AnalyzeMediaRequest, SearchPhotosRequest
from loguru import logger

router = APIRouter(prefix="/photos", tags=["photo"])


@router.post("/analyze")
def analyze_media(req: AnalyzeMediaRequest) -> dict:
    """媒体理解与向量化（Windows 开发链路由 Java 直推，Linux 走 MQ 消费同一逻辑）

    TODO(M5): 调用 photo_service 执行 qwen-vl 理解 + 多模态嵌入 + Qdrant upsert + 宠物识别
    """
    logger.info(f"[骨架] 媒体分析请求: photo_id={req.photo_id}, media_type={req.media_type}")
    raise HTTPException(status_code=status.HTTP_501_NOT_IMPLEMENTED, detail="媒体分析待实现（M5）")


@router.post("/search")
def search_photos(req: SearchPhotosRequest) -> dict:
    """自然语言语义检索素材

    TODO(M5): 文本嵌入 + Qdrant 向量检索 + payload 过滤（user_id/tags/时间）
    """
    logger.info(f"[骨架] 语义检索请求: user_id={req.user_id}, query={req.query!r}")
    raise HTTPException(status_code=status.HTTP_501_NOT_IMPLEMENTED, detail="语义检索待实现（M5）")
