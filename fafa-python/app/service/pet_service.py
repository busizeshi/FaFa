"""
宠物服务层
"""
from sqlalchemy import select, text
from sqlalchemy.ext.asyncio import AsyncSession
from datetime import date
from loguru import logger


class PetService:
    
    def __init__(self, db: AsyncSession):
        self.db = db
    
    async def get_pet_by_id(self, pet_id: int) -> dict:
        """
        根据 ID 查询宠物
        """
        query = text("""
            SELECT 
                id, user_id, name, avatar, species, breed, gender, 
                birth_date, adopt_date, weight, is_neutered, 
                coat_color, remarks, status, sort_order,
                TIMESTAMPDIFF(MONTH, birth_date, CURDATE()) as age_in_months
            FROM pet 
            WHERE id = :pet_id AND status = 1
        """)
        
        result = await self.db.execute(query, {"pet_id": pet_id})
        row = result.fetchone()
        
        if not row:
            return None
        
        return {
            "id": row[0],
            "user_id": row[1],
            "name": row[2],
            "avatar": row[3],
            "species": row[4],
            "breed": row[5],
            "gender": row[6],
            "birth_date": row[7],
            "adopt_date": row[8],
            "weight": row[9],
            "is_neutered": row[10],
            "coat_color": row[11],
            "remarks": row[12],
            "status": row[13],
            "sort_order": row[14],
            "age_in_months": row[15],
        }
    
    async def get_pets_by_user(self, user_id: int) -> list:
        """
        根据用户 ID 查询宠物列表
        """
        query = text("""
            SELECT 
                id, name, species, breed, gender, birth_date, weight,
                TIMESTAMPDIFF(MONTH, birth_date, CURDATE()) as age_in_months
            FROM pet 
            WHERE user_id = :user_id AND status = 1
            ORDER BY sort_order, id
        """)
        
        result = await self.db.execute(query, {"user_id": user_id})
        rows = result.fetchall()
        
        pets = []
        for row in rows:
            pets.append({
                "id": row[0],
                "name": row[1],
                "species": row[2],
                "breed": row[3],
                "gender": row[4],
                "birth_date": row[5],
                "weight": row[6],
                "age_in_months": row[7],
            })
        
        return pets
