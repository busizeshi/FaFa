"""
记录数据服务层
"""
from sqlalchemy import text
from sqlalchemy.ext.asyncio import AsyncSession
from datetime import date, datetime
from typing import Optional, List
from loguru import logger


class RecordService:
    
    def __init__(self, db: AsyncSession):
        self.db = db
    
    async def get_feed_records(self, pet_id: int, start_date: Optional[date] = None, 
                               end_date: Optional[date] = None, limit: int = 50) -> List[dict]:
        """
        查询喂食记录
        """
        query = text("""
            SELECT 
                id, pet_id, feed_time, food_name, food_type, 
                amount, unit, brand, remarks
            FROM feed_record 
            WHERE pet_id = :pet_id
            AND (:start_date IS NULL OR DATE(feed_time) >= :start_date)
            AND (:end_date IS NULL OR DATE(feed_time) <= :end_date)
            ORDER BY feed_time DESC
            LIMIT :limit
        """)
        
        result = await self.db.execute(query, {
            "pet_id": pet_id,
            "start_date": start_date,
            "end_date": end_date,
            "limit": limit
        })
        rows = result.fetchall()
        
        records = []
        for row in rows:
            records.append({
                "id": row[0],
                "pet_id": row[1],
                "feed_time": row[2],
                "food_name": row[3],
                "food_type": row[4],
                "amount": row[5],
                "unit": row[6],
                "brand": row[7],
                "remarks": row[8],
            })
        
        return records
    
    async def get_weight_records(self, pet_id: int, start_date: Optional[date] = None, 
                                 end_date: Optional[date] = None) -> List[dict]:
        """
        查询体重记录
        """
        query = text("""
            SELECT 
                id, pet_id, record_date, weight, bcs_score, remarks
            FROM weight_record 
            WHERE pet_id = :pet_id
            AND (:start_date IS NULL OR record_date >= :start_date)
            AND (:end_date IS NULL OR record_date <= :end_date)
            ORDER BY record_date DESC
        """)
        
        result = await self.db.execute(query, {
            "pet_id": pet_id,
            "start_date": start_date,
            "end_date": end_date
        })
        rows = result.fetchall()
        
        records = []
        for row in rows:
            records.append({
                "id": row[0],
                "pet_id": row[1],
                "record_date": row[2],
                "weight": float(row[3]) if row[3] else None,
                "bcs_score": row[4],
                "remarks": row[5],
            })
        
        return records
    
    async def get_water_records(self, pet_id: int, start_date: Optional[date] = None, 
                                end_date: Optional[date] = None, limit: int = 50) -> List[dict]:
        """
        查询饮水记录
        """
        query = text("""
            SELECT 
                id, pet_id, water_time, amount, remarks
            FROM water_record 
            WHERE pet_id = :pet_id
            AND (:start_date IS NULL OR DATE(water_time) >= :start_date)
            AND (:end_date IS NULL OR DATE(water_time) <= :end_date)
            ORDER BY water_time DESC
            LIMIT :limit
        """)
        
        result = await self.db.execute(query, {
            "pet_id": pet_id,
            "start_date": start_date,
            "end_date": end_date,
            "limit": limit
        })
        rows = result.fetchall()
        
        records = []
        for row in rows:
            records.append({
                "id": row[0],
                "pet_id": row[1],
                "water_time": row[2],
                "amount": row[3],
                "remarks": row[4],
            })
        
        return records
    
    async def get_excretion_records(self, pet_id: int, start_date: Optional[date] = None, 
                                    end_date: Optional[date] = None, limit: int = 50) -> List[dict]:
        """
        查询排便记录
        """
        query = text("""
            SELECT 
                id, pet_id, excretion_time, type, color, 
                shape, abnormal, remarks
            FROM excretion_record 
            WHERE pet_id = :pet_id
            AND (:start_date IS NULL OR DATE(excretion_time) >= :start_date)
            AND (:end_date IS NULL OR DATE(excretion_time) <= :end_date)
            ORDER BY excretion_time DESC
            LIMIT :limit
        """)
        
        result = await self.db.execute(query, {
            "pet_id": pet_id,
            "start_date": start_date,
            "end_date": end_date,
            "limit": limit
        })
        rows = result.fetchall()
        
        records = []
        for row in rows:
            records.append({
                "id": row[0],
                "pet_id": row[1],
                "excretion_time": row[2],
                "type": row[3],
                "color": row[4],
                "shape": row[5],
                "abnormal": bool(row[6]) if row[6] is not None else False,
                "remarks": row[7],
            })
        
        return records
    
    async def get_event_records(self, pet_id: int, event_type: Optional[str] = None,
                                start_date: Optional[date] = None, 
                                end_date: Optional[date] = None, limit: int = 50) -> List[dict]:
        """
        查询事件记录
        """
        query = text("""
            SELECT 
                id, pet_id, event_time, event_type, title, 
                content, images, remarks
            FROM event_record 
            WHERE pet_id = :pet_id
            AND (:event_type IS NULL OR event_type = :event_type)
            AND (:start_date IS NULL OR DATE(event_time) >= :start_date)
            AND (:end_date IS NULL OR DATE(event_time) <= :end_date)
            ORDER BY event_time DESC
            LIMIT :limit
        """)
        
        result = await self.db.execute(query, {
            "pet_id": pet_id,
            "event_type": event_type,
            "start_date": start_date,
            "end_date": end_date,
            "limit": limit
        })
        rows = result.fetchall()
        
        records = []
        for row in rows:
            records.append({
                "id": row[0],
                "pet_id": row[1],
                "event_time": row[2],
                "event_type": row[3],
                "title": row[4],
                "content": row[5],
                "images": row[6],
                "remarks": row[7],
            })
        
        return records
    
    async def get_records_summary(self, pet_id: int, days: int = 7) -> dict:
        """
        获取记录汇总信息（最近 N 天）
        """
        # 喂食次数
        feed_query = text("""
            SELECT COUNT(*) 
            FROM feed_record 
            WHERE pet_id = :pet_id 
            AND feed_time >= DATE_SUB(NOW(), INTERVAL :days DAY)
        """)
        feed_result = await self.db.execute(feed_query, {"pet_id": pet_id, "days": days})
        feed_count = feed_result.scalar()
        
        # 最新体重
        weight_query = text("""
            SELECT weight, record_date
            FROM weight_record 
            WHERE pet_id = :pet_id 
            ORDER BY record_date DESC 
            LIMIT 1
        """)
        weight_result = await self.db.execute(weight_query, {"pet_id": pet_id})
        weight_row = weight_result.fetchone()
        
        # 饮水总量
        water_query = text("""
            SELECT COALESCE(SUM(amount), 0) 
            FROM water_record 
            WHERE pet_id = :pet_id 
            AND water_time >= DATE_SUB(NOW(), INTERVAL :days DAY)
        """)
        water_result = await self.db.execute(water_query, {"pet_id": pet_id, "days": days})
        water_total = water_result.scalar()
        
        # 排便次数（粪便）
        excretion_query = text("""
            SELECT COUNT(*) 
            FROM excretion_record 
            WHERE pet_id = :pet_id 
            AND type = 'feces'
            AND excretion_time >= DATE_SUB(NOW(), INTERVAL :days DAY)
        """)
        excretion_result = await self.db.execute(excretion_query, {"pet_id": pet_id, "days": days})
        excretion_count = excretion_result.scalar()
        
        return {
            "pet_id": pet_id,
            "summary_days": days,
            "feed_count": feed_count,
            "latest_weight": float(weight_row[0]) if weight_row and weight_row[0] else None,
            "latest_weight_date": str(weight_row[1]) if weight_row and weight_row[1] else None,
            "water_total_ml": water_total,
            "excretion_count": excretion_count,
        }
