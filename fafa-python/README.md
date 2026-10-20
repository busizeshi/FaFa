# fafa-python

The standalone FastAPI service is the home for AI orchestration and provider adapters. It must read business facts through authorized Biz APIs and must not write Java-owned business data directly.

## Requirements

- Python 3.12
- `uv` for resolving and installing the committed `uv.lock`

## Local run

```powershell
uv sync --locked
uv run uvicorn app.main:app --reload --app-dir .
```

Health is available at `GET /api/v1/health`; through Gateway it is available at `GET /api/ai/api/v1/health`. The shared response fields are `code`, `message`, `data`, and `requestId`. Unknown exceptions are logged with a stack trace and returned without internal details.

Settings use the `FAFA_PYTHON_` environment prefix. Copy values from `../deploy/docker-compose/.env.example` when needed; do not commit real API keys.
