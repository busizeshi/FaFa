"""自然语言记录解析纯函数测试。"""

import unittest
from datetime import date

from app.service.record_parser import parse_record_message


class RecordParserTest(unittest.TestCase):
    def test_parse_daily_status_and_weight(self) -> None:
        result = parse_record_message(
            pet_id=1,
            message="记录一下今天饮食正常，精神正常，便便正常，体重 5.2 公斤",
            today=date(2026, 8, 23),
        )

        self.assertTrue(result["requires_user_confirmation"])
        types = {item["type"] for item in result["records"]}
        self.assertEqual(types, {"daily_status", "weight_record"})
        daily = next(item for item in result["records"] if item["type"] == "daily_status")
        self.assertEqual(daily["draft"]["appetite"], "normal")
        self.assertEqual(daily["draft"]["energy"], "normal")
        weight = next(item for item in result["records"] if item["type"] == "weight_record")
        self.assertEqual(weight["draft"]["weight"], 5.2)

    def test_parse_feeding_without_amount_requires_follow_up(self) -> None:
        result = parse_record_message(1, "今天早上吃了半罐罐头", date(2026, 8, 23))

        self.assertEqual(len(result["records"]), 1)
        draft = result["records"][0]
        self.assertEqual(draft["type"], "feeding_record")
        self.assertIsNone(draft["draft"]["amount"])
        self.assertLess(draft["confidence"], 0.80)


if __name__ == "__main__":
    unittest.main()
