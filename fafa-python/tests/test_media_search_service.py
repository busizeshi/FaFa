import asyncio
import unittest
from datetime import date
from types import SimpleNamespace
from unittest.mock import AsyncMock, Mock, patch

from app.core.llm import Purpose
from app.schemas.photo import PetSearchCandidate, PhotoSearchRequest
from app.service.media_search_service import MediaSearchService, SearchContext


class MediaSearchServiceTest(unittest.TestCase):
    def test_multiple_pets_are_matched_with_qwen_turbo_in_parallel_with_time_parsing(self) -> None:
        service = MediaSearchService()

        async def invoke(messages):
            prompt = messages[-1].content
            if "时间解析器" in prompt:
                return SimpleNamespace(content='{"start_date":"2026-08-01","end_date":"2026-08-31"}')
            return SimpleNamespace(content='{"pet_ids":[2]}')

        model = SimpleNamespace(ainvoke=AsyncMock(side_effect=invoke))
        candidates = [PetSearchCandidate(pet_id=1, name="豆包"), PetSearchCandidate(pet_id=2, name="奶糖")]
        with patch("app.service.media_search_service.get_chat_model", return_value=model) as get_model:
            context = asyncio.run(service.resolve_context("找八月奶糖晒太阳的照片", candidates))

        self.assertEqual(context, SearchContext(date(2026, 8, 1), date(2026, 8, 31), [2]))
        self.assertEqual(get_model.call_args_list[0].args[0], Purpose.MEDIA_QUERY)
        self.assertEqual(get_model.call_count, 2)

    def test_single_pet_skips_name_matching(self) -> None:
        service = MediaSearchService()
        model = SimpleNamespace(ainvoke=AsyncMock(return_value=SimpleNamespace(
            content='{"start_date":null,"end_date":null}'
        )))
        with patch("app.service.media_search_service.get_chat_model", return_value=model):
            context = asyncio.run(service.resolve_context(
                "找晒太阳的照片", [PetSearchCandidate(pet_id=1, name="豆包")]
            ))

        self.assertEqual(context.matched_pet_ids, [])
        model.ainvoke.assert_awaited_once()

    def test_search_merges_semantic_results_before_exact_only_results(self) -> None:
        service = MediaSearchService()
        request = PhotoSearchRequest(
            user_id=3,
            query="找八月奶糖晒太阳的照片",
            pet_candidates=[PetSearchCandidate(pet_id=1, name="豆包"), PetSearchCandidate(pet_id=2, name="奶糖")],
            limit=3,
        )
        client = SimpleNamespace(
            query_points=Mock(return_value=SimpleNamespace(points=[
                SimpleNamespace(payload={"media_id": 11}, score=0.92),
                SimpleNamespace(payload={"media_id": 12}, score=0.88),
            ])),
            scroll=Mock(return_value=([
                SimpleNamespace(payload={"media_id": 12}),
                SimpleNamespace(payload={"media_id": 13}),
            ], None)),
        )
        with (
            patch.object(service, "resolve_context", new=AsyncMock(return_value=SearchContext(
                date(2026, 8, 1), date(2026, 8, 31), [2]
            ))),
            patch("app.service.media_search_service.get_text_multimodal_embedding", new=AsyncMock(return_value=[0.1, 0.2])),
            patch("app.service.media_search_service.get_client", return_value=client),
            patch("app.service.media_search_service.get_settings", return_value=SimpleNamespace(search_score_threshold=0.3)),
        ):
            results = asyncio.run(service.search(request))

        self.assertEqual(results, [
            {"media_id": 11, "score": 0.92},
            {"media_id": 12, "score": 0.88},
            {"media_id": 13, "score": 0.0},
        ])
        self.assertEqual(client.query_points.call_count, 1)
        self.assertEqual(client.scroll.call_count, 1)
        self.assertEqual(client.scroll.call_args.kwargs["order_by"].key, "taken_ts")

    def test_explicit_and_parsed_time_conditions_use_intersection(self) -> None:
        start, end = MediaSearchService._merge_time_bounds(
            date(2026, 8, 10), date(2026, 8, 31), date(2026, 8, 1), date(2026, 8, 20)
        )
        self.assertEqual((start, end), (date(2026, 8, 10), date(2026, 8, 20)))


if __name__ == "__main__":
    unittest.main()
