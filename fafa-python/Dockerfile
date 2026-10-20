FROM ghcr.io/astral-sh/uv:0.13.3 AS uv

FROM python:3.12.12-slim
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    UV_COMPILE_BYTECODE=1 \
    UV_LINK_MODE=copy \
    PATH="/app/.venv/bin:$PATH"
COPY --from=uv /uv /bin/uv
WORKDIR /app
COPY fafa-python/pyproject.toml fafa-python/uv.lock fafa-python/README.md ./
RUN uv sync --frozen --no-dev --no-install-project
COPY fafa-python/app ./app
RUN uv sync --frozen --no-dev
EXPOSE 8000
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
