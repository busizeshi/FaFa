import asyncio
import unittest
from types import SimpleNamespace
from unittest.mock import patch

from app.core import dashscope_client
from app.core.qdrant import VECTOR_DIM


class DashScopeClientTest(unittest.TestCase):
    def test_text_embedding_uses_fusion_and_collection_dimension(self) -> None:
        result = SimpleNamespace(
            status_code=200,
            output={"embeddings": [{"embedding": [0.1] * VECTOR_DIM}]},
        )
        settings = SimpleNamespace(
            dashscope_api_key="test-key",
            model_embed_multimodal="qwen3-vl-embedding",
        )

        with (
            patch.object(dashscope_client, "get_settings", return_value=settings),
            patch.object(
                dashscope_client.MultiModalEmbedding,
                "call",
                return_value=result,
            ) as call,
        ):
            embedding = asyncio.run(
                dashscope_client.get_text_multimodal_embedding("逗猫棒")
            )

        self.assertEqual(len(embedding), VECTOR_DIM)
        call.assert_called_once_with(
            model="qwen3-vl-embedding",
            input=[{"text": "逗猫棒"}],
            dimension=VECTOR_DIM,
            enable_fusion=True,
        )

    def test_embedding_dimension_mismatch_is_rejected(self) -> None:
        result = SimpleNamespace(
            status_code=200,
            output={"embeddings": [{"embedding": [0.1]}]},
        )
        settings = SimpleNamespace(
            dashscope_api_key="test-key",
            model_embed_multimodal="qwen3-vl-embedding",
        )

        with (
            patch.object(dashscope_client, "get_settings", return_value=settings),
            patch.object(
                dashscope_client.MultiModalEmbedding,
                "call",
                return_value=result,
            ),
            self.assertRaises(RuntimeError),
        ):
            asyncio.run(
                dashscope_client.get_text_multimodal_embedding("逗猫棒")
            )

    def test_video_embedding_uses_public_video_url_and_sampling_fps(self) -> None:
        result = SimpleNamespace(
            status_code=200,
            output={"embeddings": [{"embedding": [0.2] * VECTOR_DIM}]},
        )
        settings = SimpleNamespace(
            dashscope_api_key="test-key",
            model_embed_multimodal="qwen3-vl-embedding",
            video_embedding_fps=0.5,
        )

        with (
            patch.object(dashscope_client, "get_settings", return_value=settings),
            patch.object(
                dashscope_client.MultiModalEmbedding,
                "call",
                return_value=result,
            ) as call,
        ):
            embedding = asyncio.run(
                dashscope_client.get_video_multimodal_embedding(
                    "https://cdn.example.com/cat.mp4"
                )
            )

        self.assertEqual(len(embedding), VECTOR_DIM)
        call.assert_called_once_with(
            model="qwen3-vl-embedding",
            input=[{"video": "https://cdn.example.com/cat.mp4"}],
            dimension=VECTOR_DIM,
            enable_fusion=True,
            fps=0.5,
        )

    def test_private_video_url_is_rejected_before_remote_call(self) -> None:
        with self.assertRaises(dashscope_client.VideoEmbeddingInputError):
            asyncio.run(
                dashscope_client.get_video_multimodal_embedding(
                    "http://192.168.1.14:9000/fafa-media/media/cat.mp4"
                )
            )


if __name__ == "__main__":
    unittest.main()
