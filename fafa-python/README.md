# FaFa 宠物生活助手 - Python AI 服务

基于 FastAPI + LangChain 的 AI 服务，提供智能对话、图片识别、向量搜索等能力。

## 技术栈

- **FastAPI**: 高性能 Web 框架
- **LangChain**: AI 应用开发框架
- **Qdrant**: 向量数据库客户端
- **通义千问**: 语言模型（qwen3.7-flash）

## 项目结构

```
fafa-python/
├── app/
│   ├── api/              # API 路由
│   ├── service/          # 业务服务
│   ├── domain/           # 领域模型
│   ├── repository/       # 数据访问
│   ├── core/             # 核心配置
│   └── main.py          # 应用入口
├── tests/               # 测试
├── requirements.txt     # 依赖
└── Dockerfile          # Docker 镜像
```

## 安装依赖

```bash
pip install -r requirements.txt
```

## 运行

```bash
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

## API 文档

启动后访问: http://localhost:8000/docs
