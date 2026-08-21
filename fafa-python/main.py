"""FaFa AI 服务（fafa-python）

FastAPI 入口。启动：
    uvicorn main:app --host 0.0.0.0 --port 8000 --reload
"""

from fastapi import FastAPI, Request

from app.api import chat, health, photo
from app.core.config import settings
from app.core.logging import setup_logging
from app.core.logging import trace_id_ctx
from app.core.qdrant import ensure_collections

setup_logging()

app = FastAPI(title="FaFa AI Service", version="0.1.0")


@app.middleware("http")
async def trace_id_middleware(request: Request, call_next):
    """透传/生成 traceId，与 Java 侧日志串联同一条链路"""
    trace_id = request.headers.get("X-Trace-Id")
    if not trace_id:
        from uuid import uuid4

        trace_id = uuid4().hex
    token = trace_id_ctx.set(trace_id)
    try:
        response = await call_next(request)
    finally:
        trace_id_ctx.reset(token)
    response.headers["X-Trace-Id"] = trace_id
    return response


app.include_router(health.router)
app.include_router(photo.router, prefix="/api")
app.include_router(chat.router, prefix="/api")


@app.on_event("startup")
async def on_startup() -> None:
    """启动时确保 Qdrant 集合存在；失败不阻断启动（Qdrant 未就绪时相关接口报错）"""
    ensure_collections()


if __name__ == "__main__":
    import uvicorn

    uvicorn.run("main:app", host="0.0.0.0", port=settings.port, reload=True)
