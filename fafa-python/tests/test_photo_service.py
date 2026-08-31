import asyncio
import unittest
from datetime import datetime
from types import SimpleNamespace
from unittest.mock import AsyncMock, Mock, patch

from app.service.photo_service import PhotoService


class PhotoServiceTest(unittest.TestCase):
    def test_video_uses_embedding_only_and_keeps_permanent_file_url(self) -> None:
        service = PhotoService()

        with (
            patch(
                "app.service.photo_service.get_video_multimodal_embedding",
                new=AsyncMock(return_value=[0.1, 0.2, 0.3]),
            ) as embedding,
            patch.object(service, "_upsert_media_vector") as upsert,
            patch(
                "app.service.photo_service.send_photo_analysis_callback",
                new=AsyncMock(),
            ) as callback,
        ):
            asyncio.run(
                service.analyze(
                    user_id=3,
                    photo_id=3,
                    url="https://signed.example.com/cat.mp4",
                    file_url="oss://media/cat.mp4",
                    media_type="video",
                    tags=["阳台"],
                    pet_id=1,
                    candidate_pet_ids=[],
                    message_id="media-3",
                    taken_at=datetime(2026, 8, 20, 15, 30),
                    uploaded_at=datetime(2026, 8, 20, 15, 40),
                )
            )

        embedding.assert_awaited_once_with("https://signed.example.com/cat.mp4")
        upsert.assert_called_once()
        self.assertEqual(upsert.call_args.kwargs["pet_ids"], [1])
        self.assertEqual(upsert.call_args.kwargs["file_url"], "oss://media/cat.mp4")
        self.assertEqual(upsert.call_args.kwargs["uploaded_at"], datetime(2026, 8, 20, 15, 40))
        callback.assert_awaited_once()
        self.assertTrue(callback.call_args.kwargs["success"])
        self.assertEqual(callback.call_args.kwargs["recognized_pet_ids"], [])

    def test_multiple_pets_are_all_returned_when_matching(self) -> None:
        service = PhotoService()

        with (
            patch(
                "app.service.photo_service.get_multimodal_embedding",
                new=AsyncMock(return_value=[0.1, 0.2, 0.3]),
            ),
            patch.object(
                service,
                "_recognize_pets",
                return_value=[(2, 0.95), (1, 0.91)],
            ) as recognize,
            patch.object(service, "_upsert_media_vector") as upsert,
            patch(
                "app.service.photo_service.send_photo_analysis_callback",
                new=AsyncMock(),
            ) as callback,
        ):
            asyncio.run(
                service.analyze(
                    user_id=3,
                    photo_id=9,
                    url="https://cdn.example.com/cat.jpg",
                    file_url="oss://media/cat.jpg",
                    media_type="photo",
                    tags=["户外"],
                    pet_id=None,
                    candidate_pet_ids=[1, 2],
                    message_id="media-9",
                )
            )

        recognize.assert_called_once_with(3, [0.1, 0.2, 0.3], [1, 2])
        self.assertEqual(upsert.call_args.kwargs["pet_ids"], [2, 1])
        self.assertEqual(callback.call_args.kwargs["recognized_pet_ids"], [2, 1])
        self.assertEqual(callback.call_args.kwargs["recognition_score"], 0.95)

    def test_recognition_returns_all_pets_above_configured_threshold(self) -> None:
        service = PhotoService()
        qdrant_client = SimpleNamespace(
            query_points=Mock(
                return_value=SimpleNamespace(
                    points=[
                        SimpleNamespace(payload={"pet_id": 1}, score=0.91),
                        SimpleNamespace(payload={"pet_id": 1}, score=0.88),
                        SimpleNamespace(payload={"pet_id": 2}, score=0.90),
                        SimpleNamespace(payload={"pet_id": 2}, score=0.70),
                    ]
                )
            )
        )
        settings = SimpleNamespace(recognition_threshold=0.85)

        with (
            patch("app.service.photo_service.get_client", return_value=qdrant_client),
            patch("app.service.photo_service.get_settings", return_value=settings),
        ):
            matches = service._recognize_pets(3, [0.1, 0.2], [1, 2])

        self.assertEqual(matches, [(1, 0.91), (2, 0.9)])
        qdrant_client.query_points.assert_called_once()
        self.assertEqual(qdrant_client.query_points.call_args.kwargs["limit"], 6)

    def test_upsert_persists_taken_and_uploaded_timestamps(self) -> None:
        service = PhotoService()
        qdrant_client = SimpleNamespace(upsert=Mock())

        with patch("app.service.photo_service.get_client", return_value=qdrant_client):
            service._upsert_media_vector(
                media_id=7,
                user_id=3,
                media_type="photo",
                tags=["阳台"],
                pet_ids=[1, 2],
                file_url="oss://media/cat.jpg",
                taken_at=datetime(2026, 8, 20, 15, 30),
                uploaded_at=datetime(2026, 8, 20, 16, 0),
                embedding=[0.1, 0.2],
            )

        point = qdrant_client.upsert.call_args.kwargs["points"][0]
        self.assertEqual(point.payload["taken_ts"], int(datetime(2026, 8, 20, 15, 30).timestamp() * 1000))
        self.assertEqual(point.payload["uploaded_ts"], int(datetime(2026, 8, 20, 16, 0).timestamp() * 1000))


if __name__ == "__main__":
    unittest.main()
