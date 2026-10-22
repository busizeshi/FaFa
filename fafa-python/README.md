# fafa-python

独立的 FastAPI 服务用于 AI 流程编排和模型供应商适配。它必须通过经过授权的 Biz API 读取业务事实，不得直接写入 Java 服务拥有的业务数据。

## 前置要求

- Python 3.12。
- 使用 `uv` 解析并安装已提交的 `uv.lock`。

## 本地运行

```powershell
uv sync --locked
uv run uvicorn app.main:app --reload --app-dir .
```

健康检查地址为 `GET /api/v1/health`；通过 Gateway 访问时为 `GET /api/ai/api/v1/health`。共享响应字段为 `code`、`message`、`data` 和 `requestId`。未知异常会记录堆栈，但返回给客户端时不暴露内部详情。

配置使用 `FAFA_PYTHON_` 环境变量前缀。需要时可参考 `../deploy/docker-compose/.env.example`，真实 API Key 不得提交到仓库。
