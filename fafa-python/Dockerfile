# ==================================================================
# FaFa AI 服务（FastAPI）运行镜像
#
# 构建：docker build -f fafa-python/Dockerfile fafa-python
#
# 说明：
#   1. 两阶段构建：builder 用 uv 按 uv.lock 精确安装依赖，运行镜像只带 venv 与源码；
#   2. 依赖索引默认走阿里云 PyPI 镜像（服务器直连 pypi.org 可用但较慢，可用构建参数覆盖）；
#   3. 容器内不配置任何业务 MySQL 连接（架构规范 §6.6 硬边界）。
# ==================================================================

# ---------- 阶段一：安装依赖 ----------
FROM docker.m.daocloud.io/library/python:3.12-slim AS builder

ARG PIP_INDEX_URL=https://mirrors.aliyun.com/pypi/simple/
ARG PIP_TRUSTED_HOST=mirrors.aliyun.com

ENV PIP_INDEX_URL=${PIP_INDEX_URL} \
    PIP_TRUSTED_HOST=${PIP_TRUSTED_HOST} \
    UV_DEFAULT_INDEX=${PIP_INDEX_URL} \
    UV_LINK_MODE=copy \
    UV_COMPILE_BYTECODE=1

RUN pip install --no-cache-dir uv

WORKDIR /app

# 先只复制依赖声明，命中 Docker 层缓存
COPY pyproject.toml uv.lock ./
RUN uv sync --frozen --no-dev --no-install-project

# 再复制源码并安装本项目
COPY src ./src
RUN uv sync --frozen --no-dev

# ---------- 阶段二：运行 ----------
FROM docker.m.daocloud.io/library/python:3.12-slim

ENV TZ=Asia/Shanghai \
    PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    PATH=/app/.venv/bin:$PATH

RUN apt-get update \
 && apt-get install -y --no-install-recommends tzdata \
 && rm -rf /var/lib/apt/lists/* \
 && ln -snf /usr/share/zoneinfo/$TZ /etc/localtime \
 && echo $TZ > /etc/timezone \
 && useradd --create-home --uid 10001 fafa

WORKDIR /app

COPY --from=builder --chown=fafa:fafa /app/.venv /app/.venv
COPY --from=builder --chown=fafa:fafa /app/src /app/src

USER fafa

EXPOSE 8090

# 存活探针 /healthz（见 fafa_ai/api/routes/health.py）
HEALTHCHECK --interval=15s --timeout=3s --start-period=20s --retries=5 \
    CMD python -c "import urllib.request,sys; sys.exit(0 if urllib.request.urlopen('http://127.0.0.1:8090/healthz', timeout=2).status == 200 else 1)"

CMD ["uvicorn", "fafa_ai.main:app", "--host", "0.0.0.0", "--port", "8090"]
