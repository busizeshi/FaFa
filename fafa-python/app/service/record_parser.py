"""自然语言记录解析器。

Phase 5 只生成草稿，不写 Java 业务表。对高风险或无法确定的字段保留为空，
由用户在原有业务页面确认。
"""

import re
from datetime import date, datetime
from typing import Any


def parse_record_message(pet_id: int, message: str, today: date | None = None) -> dict[str, Any]:
    """解析每日状态、喂食、体重和事件草稿。"""

    current = today or date.today()
    records: list[dict[str, Any]] = []
    warnings: list[str] = []
    daily = _parse_daily_status(pet_id, message, current)
    if daily is not None:
        records.append(daily)
    feeding = _parse_feeding(pet_id, message, current)
    if feeding is not None:
        records.append(feeding)
    weight = _parse_weight(pet_id, message, current)
    if weight is not None:
        records.append(weight)
    if not records:
        warnings.append("没有识别出可填写的记录字段，请在对应页面手动记录")
    return {"records": records, "requires_user_confirmation": True, "warnings": warnings}


def _parse_daily_status(pet_id: int, message: str, current: date) -> dict[str, Any] | None:
    field_rules = (
        ("appetite", ("拒食", "不吃", "没胃口"), "refused"),
        ("appetite", ("食欲正常", "吃饭正常", "饮食正常"), "normal"),
        ("energy", ("精神不好", "精神差", "没精神"), "lethargic"),
        ("energy", ("精神正常", "状态正常", "精神好"), "normal"),
        ("water_intake", ("喝水多", "饮水多"), "more"),
        ("water_intake", ("饮水正常", "喝水正常"), "normal"),
        ("defecation", ("腹泻", "拉稀"), "diarrhea"),
        ("defecation", ("便便正常", "排便正常"), "normal"),
        ("vomiting", ("呕吐",), "once"),
        ("urination", ("排尿正常",), "normal"),
    )
    fields: dict[str, str] = {}
    for field, keywords, value in field_rules:
        if any(keyword in message for keyword in keywords):
            fields.setdefault(field, value)
    if not fields and not any(word in message for word in ("日常", "打卡", "记录状态")):
        return None
    draft = {"petId": pet_id, "recordDate": current.isoformat()}
    field_names = {
        "appetite": "appetite",
        "energy": "energy",
        "water_intake": "waterIntake",
        "defecation": "defecation",
        "vomiting": "vomiting",
        "urination": "urination",
    }
    draft.update({field_names[field]: value for field, value in fields.items()})
    return {
        "type": "daily_status",
        "confidence": 0.90 if fields else 0.60,
        "draft": draft,
        "missingFields": [field for field in (
            "appetite", "energy", "waterIntake", "defecation", "vomiting", "urination"
        ) if _snake(field) not in fields],
        "pageUrl": f"/pages/daily-record/edit?petId={pet_id}&date={current.isoformat()}",
    }


def _parse_feeding(pet_id: int, message: str, current: date) -> dict[str, Any] | None:
    if not any(word in message for word in ("喂", "吃了", "吃饭", "罐头", "猫粮")):
        return None
    amount_match = re.search(r"(\d+(?:\.\d+)?)\s*(克|g|毫升|ml)", message, re.IGNORECASE)
    food_type = "can" if "罐头" in message else "main_food"
    amount = float(amount_match.group(1)) if amount_match else None
    unit = "ml" if amount_match and amount_match.group(2).lower() in ("毫升", "ml") else "g"
    notes = None if amount_match else message.strip()
    return {
        "type": "feeding_record",
        "confidence": 0.92 if amount_match else 0.78,
        "draft": {
            "petId": pet_id,
            "feedingTime": datetime.combine(current, datetime.min.time()).isoformat(),
            "foodType": food_type,
            "amount": amount,
            "unit": unit,
            "notes": notes,
        },
        "pageUrl": f"/pages/feeding-record/edit?petId={pet_id}",
    }


def _parse_weight(pet_id: int, message: str, current: date) -> dict[str, Any] | None:
    match = re.search(r"(?:体重|称了?)\s*(\d+(?:\.\d+)?)\s*(公斤|千克|kg|斤)?", message, re.IGNORECASE)
    if match is None:
        return None
    value = float(match.group(1))
    unit = match.group(2) or "kg"
    if unit == "斤":
        value /= 2
    return {
        "type": "weight_record",
        "confidence": 0.98,
        "draft": {
            "petId": pet_id,
            "recordDate": current.isoformat(),
            "weight": value,
            "unit": "kg",
        },
        "pageUrl": f"/pages/weight/edit?petId={pet_id}&date={current.isoformat()}",
    }


def _snake(camel: str) -> str:
    return re.sub(r"(?<!^)(?=[A-Z])", "_", camel).lower()
