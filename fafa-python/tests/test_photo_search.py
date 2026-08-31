import asyncio
import unittest
from unittest.mock import AsyncMock, patch

from app.api.photos import search_photos
from app.schemas.photo import PhotoSearchRequest
from app.service.media_search_service import MediaSearchService


class PhotoSearchRouteTest(unittest.TestCase):
    def test_search_route_delegates_to_dual_recall_service(self) -> None:
        request = PhotoSearchRequest(user_id=3, query="逗猫棒", limit=20)

        with patch(
            "app.api.photos.media_search_service.search",
            new=AsyncMock(return_value=[{"media_id": 101, "score": 0.87}]),
        ) as search:
            response = asyncio.run(search_photos(request))

        self.assertEqual(response, {"results": [{"media_id": 101, "score": 0.87}]})
        search.assert_awaited_once_with(request)

    def test_pet_filter_supports_new_multi_pet_and_legacy_single_pet_payloads(self) -> None:
        query_filter = MediaSearchService._build_filter(
            user_id=3,
            pet_ids=[7],
            start_date=None,
            end_date=None,
        )

        pet_filter = query_filter.must[1]
        self.assertEqual(
            [condition.key for condition in pet_filter.should],
            ["pet_ids", "pet_id"],
        )


if __name__ == "__main__":
    unittest.main()
