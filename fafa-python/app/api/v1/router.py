from fastapi import APIRouter

from app.api.v1.routes import system

api_router = APIRouter()
api_router.include_router(system.router, tags=["system"])
