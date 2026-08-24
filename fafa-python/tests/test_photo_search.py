import asyncio
import unittest
from types import SimpleNamespace
from unittest.mock import AsyncMock, Mock, patch

from app.api.photos import search_photos
from app.schemas.photo import PhotoSearchRequest


class PhotoSearchRouteTest(unittest.TestCase):
    def test_search_uses_current_qdrant_query_points_api(self) -> None:
        qdrant_client = SimpleNamespace(
            query_points=Mock(
                return_value=SimpleNamespace(
                    points=[
                        SimpleNamespace(
                            payload={"media_id": 101},
                            score=0.87,
                        )
                    ]
                )
            )
        )
        settings = SimpleNamespace(search_score_threshold=0.30)
        request = PhotoSearchRequest(user_id=3, query="逗猫棒", limit=20)

        with (
            patch("app.api.photos.get_client", return_value=qdrant_client),
            patch(
                "app.api.photos.get_text_multimodal_embedding",
                new=AsyncMock(return_value=[0.1, 0.2, 0.3]),
            ),
            patch("app.api.photos.get_settings", return_value=settings),
        ):
            response = asyncio.run(search_photos(request))

        self.assertEqual(response, {"results": [{"media_id": 101, "score": 0.87}]})
        qdrant_client.query_points.assert_called_once()
        call_kwargs = qdrant_client.query_points.call_args.kwargs
        self.assertEqual(call_kwargs["query"], [0.1, 0.2, 0.3])
        self.assertNotIn("query_vector", call_kwargs)


if __name__ == "__main__":
    unittest.main()
