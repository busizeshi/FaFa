import asyncio
import unittest
from datetime import datetime
from unittest.mock import AsyncMock, patch

from app.service.photo_service import PhotoService


class PhotoServiceVideoTest(unittest.TestCase):
    def test_video_analysis_generates_embedding_and_callback(self) -> None:
        service = PhotoService()

        with (
            patch(
                "app.service.photo_service.get_video_multimodal_embedding",
                new=AsyncMock(return_value=[0.1, 0.2, 0.3]),
            ),
            patch.object(
                service,
                "_prepare_video_for_vision",
                new=AsyncMock(return_value="https://cdn.example.com/cat.mp4"),
            ),
            patch.object(
                service,
                "_understand_video",
                new=AsyncMock(return_value=("小猫在玩耍", ["小猫", "玩耍"])),
            ),
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
                    url="https://cdn.example.com/cat.mp4",
                    media_type="video",
                    tags=["阳台"],
                    pet_id=1,
                    message_id="media-3",
                    taken_at=datetime(2026, 8, 20, 15, 30),
                )
            )

        upsert.assert_called_once()
        self.assertEqual(upsert.call_args.kwargs["media_type"], "video")
        self.assertEqual(upsert.call_args.kwargs["media_id"], 3)
        callback.assert_awaited_once()
        self.assertTrue(callback.call_args.kwargs["success"])
        self.assertEqual(callback.call_args.kwargs["description"], "小猫在玩耍")


if __name__ == "__main__":
    unittest.main()
