import asyncio
import unittest
from types import SimpleNamespace

from fastapi import BackgroundTasks

from app.api.photos import analyze_photo
from app.schemas.photo import PhotoAnalyzeRequest
from app.service.photo_service import photo_service


class PhotoAnalyzeApiTest(unittest.TestCase):
    def test_analyze_photo_enqueues_complete_analysis_contract(self) -> None:
        request = PhotoAnalyzeRequest(
            user_id=7,
            photo_id=101,
            url="https://signed.example.com/media.jpg",
            file_url="https://oss.example.com/media.jpg",
            media_type="photo",
            tags=["阳台"],
            candidate_pet_ids=[11, 12],
            message_id="media-101",
        )
        background_tasks = BackgroundTasks()

        response = asyncio.run(analyze_photo(
            request,
            background_tasks,
            SimpleNamespace(headers={"x-trace-id": "trace-101"}),
        ))

        self.assertEqual(response, {
            "accepted": True,
            "media_id": 101,
            "message_id": "media-101",
        })
        self.assertEqual(len(background_tasks.tasks), 1)
        task = background_tasks.tasks[0]
        self.assertEqual(task.func.__self__, photo_service)
        self.assertEqual(task.func.__name__, "analyze")
        self.assertEqual(task.kwargs, {
            "user_id": 7,
            "photo_id": 101,
            "url": "https://signed.example.com/media.jpg",
            "file_url": "https://oss.example.com/media.jpg",
            "media_type": "photo",
            "tags": ["阳台"],
            "pet_id": None,
            "candidate_pet_ids": [11, 12],
            "message_id": "media-101",
            "taken_at": None,
            "uploaded_at": None,
            "trace_id": "trace-101",
        })


if __name__ == "__main__":
    unittest.main()
