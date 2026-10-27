"""健康检查接口测试。

运行方式：``uv run pytest``（依赖见 pyproject dev 组）
"""

from __future__ import annotations

from fastapi.testclient import TestClient

from fafa_ai.main import create_app


def test_healthz_returns_up() -> None:
    """存活探针应返回 UP 状态与服务标识。"""
    client = TestClient(create_app())
    response = client.get("/healthz")

    assert response.status_code == 200
    body = response.json()
    assert body["service"] == "fafa-ai"
    assert body["status"] == "UP"
    # requestId 中间件应回写响应头
    assert response.headers.get("X-Request-Id")
