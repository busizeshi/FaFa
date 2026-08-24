import asyncio
import unittest
from types import SimpleNamespace
from unittest.mock import patch

from app.schemas.medical import MedicalSummaryRequest
from app.service.medical_summary_service import MedicalSummaryService


class FakeJavaClient:
    async def get_profile(self, *_args):
        return {"petId": 1, "name": "豆包", "type": "cat"}

    async def get_daily_status(self, *_args):
        return {"records": [{"recordDate": "2026-08-23", "appetite": "normal"}]}

    async def get_medical_visits(self, *_args):
        return {"records": [{"id": 10, "visitDate": "2026-08-20", "hospitalName": "安心"}]}

    async def get_medication_plans(self, *_args):
        return {"records": [], "executions": []}

    async def get_health_alerts(self, *_args):
        return {"records": [{"message": "连续记录需要关注"}]}


class MedicalSummaryServiceTest(unittest.TestCase):
    def test_generate_without_model_returns_fact_summary(self) -> None:
        request = MedicalSummaryRequest(
            user_id=1,
            pet_id=1,
            start_date="2026-08-01",
            end_date="2026-08-23",
        )
        settings = SimpleNamespace(dashscope_api_key="", agent_timeout_seconds=1)
        with patch("app.service.medical_summary_service.get_settings", return_value=settings):
            result = asyncio.run(MedicalSummaryService(FakeJavaClient()).generate(request))

        self.assertEqual(result["pet"]["name"], "豆包")
        self.assertEqual(len(result["medicalVisits"]), 1)
        self.assertIn("连续记录需要关注", result["attentionItems"])
        self.assertIn("不构成诊断", result["disclaimer"])


if __name__ == "__main__":
    unittest.main()
