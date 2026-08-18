"""
记录数据查询路由
"""
from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.ext.asyncio import AsyncSession
from app.core.database import get_db
from app.service.record_service import RecordService
from datetime import date
from typing import Optional
from loguru import logger

router = APIRouter(prefix="/records", tags=["记录数据"])


@router.get("/feed/{pet_id}")
async def get_feed_records(
    pet_id: int,
    start_date: Optional[date] = Query(None, description="开始日期"),
    end_date: Optional[date] = Query(None, description="结束日期"),
    limit: int = Query(50, ge=1, le=100, description="返回记录数量限制"),
    db: AsyncSession = Depends(get_db)
):
    """
    获取喂食记录（供 AI 使用）
    """
    try:
        record_service = RecordService(db)
        records = await record_service.get_feed_records(pet_id, start_date, end_date, limit)
        
        return {
            "pet_id": pet_id,
            "count": len(records),
            "records": [
                {
                    "feed_time": str(r["feed_time"]),
                    "food_name": r["food_name"],
                    "food_type": r["food_type"],
                    "amount": r["amount"],
                    "unit": r["unit"],
                    "remarks": r["remarks"],
                }
                for r in records
            ]
        }
    except Exception as e:
        logger.error(f"获取喂食记录失败: {e}")
        raise HTTPException(status_code=500, detail="服务器内部错误")


@router.get("/weight/{pet_id}")
async def get_weight_records(
    pet_id: int,
    start_date: Optional[date] = Query(None, description="开始日期"),
    end_date: Optional[date] = Query(None, description="结束日期"),
    db: AsyncSession = Depends(get_db)
):
    """
    获取体重记录（供 AI 使用）
    """
    try:
        record_service = RecordService(db)
        records = await record_service.get_weight_records(pet_id, start_date, end_date)
        
        return {
            "pet_id": pet_id,
            "count": len(records),
            "records": [
                {
                    "record_date": str(r["record_date"]),
                    "weight": r["weight"],
                    "bcs_score": r["bcs_score"],
                    "remarks": r["remarks"],
                }
                for r in records
            ]
        }
    except Exception as e:
        logger.error(f"获取体重记录失败: {e}")
        raise HTTPException(status_code=500, detail="服务器内部错误")


@router.get("/water/{pet_id}")
async def get_water_records(
    pet_id: int,
    start_date: Optional[date] = Query(None, description="开始日期"),
    end_date: Optional[date] = Query(None, description="结束日期"),
    limit: int = Query(50, ge=1, le=100, description="返回记录数量限制"),
    db: AsyncSession = Depends(get_db)
):
    """
    获取饮水记录（供 AI 使用）
    """
    try:
        record_service = RecordService(db)
        records = await record_service.get_water_records(pet_id, start_date, end_date, limit)
        
        return {
            "pet_id": pet_id,
            "count": len(records),
            "records": [
                {
                    "water_time": str(r["water_time"]),
                    "amount": r["amount"],
                    "remarks": r["remarks"],
                }
                for r in records
            ]
        }
    except Exception as e:
        logger.error(f"获取饮水记录失败: {e}")
        raise HTTPException(status_code=500, detail="服务器内部错误")


@router.get("/excretion/{pet_id}")
async def get_excretion_records(
    pet_id: int,
    start_date: Optional[date] = Query(None, description="开始日期"),
    end_date: Optional[date] = Query(None, description="结束日期"),
    limit: int = Query(50, ge=1, le=100, description="返回记录数量限制"),
    db: AsyncSession = Depends(get_db)
):
    """
    获取排便记录（供 AI 使用）
    """
    try:
        record_service = RecordService(db)
        records = await record_service.get_excretion_records(pet_id, start_date, end_date, limit)
        
        return {
            "pet_id": pet_id,
            "count": len(records),
            "records": [
                {
                    "excretion_time": str(r["excretion_time"]),
                    "type": r["type"],
                    "color": r["color"],
                    "shape": r["shape"],
                    "abnormal": r["abnormal"],
                    "remarks": r["remarks"],
                }
                for r in records
            ]
        }
    except Exception as e:
        logger.error(f"获取排便记录失败: {e}")
        raise HTTPException(status_code=500, detail="服务器内部错误")


@router.get("/event/{pet_id}")
async def get_event_records(
    pet_id: int,
    event_type: Optional[str] = Query(None, description="事件类型"),
    start_date: Optional[date] = Query(None, description="开始日期"),
    end_date: Optional[date] = Query(None, description="结束日期"),
    limit: int = Query(50, ge=1, le=100, description="返回记录数量限制"),
    db: AsyncSession = Depends(get_db)
):
    """
    获取事件记录（供 AI 使用）
    """
    try:
        record_service = RecordService(db)
        records = await record_service.get_event_records(pet_id, event_type, start_date, end_date, limit)
        
        return {
            "pet_id": pet_id,
            "count": len(records),
            "records": [
                {
                    "event_time": str(r["event_time"]),
                    "event_type": r["event_type"],
                    "title": r["title"],
                    "content": r["content"],
                    "remarks": r["remarks"],
                }
                for r in records
            ]
        }
    except Exception as e:
        logger.error(f"获取事件记录失败: {e}")
        raise HTTPException(status_code=500, detail="服务器内部错误")


@router.get("/summary/{pet_id}")
async def get_records_summary(
    pet_id: int,
    days: int = Query(7, ge=1, le=90, description="统计天数"),
    db: AsyncSession = Depends(get_db)
):
    """
    获取记录汇总信息（供 AI 使用）
    """
    try:
        record_service = RecordService(db)
        summary = await record_service.get_records_summary(pet_id, days)
        
        return summary
    except Exception as e:
        logger.error(f"获取记录汇总失败: {e}")
        raise HTTPException(status_code=500, detail="服务器内部错误")
