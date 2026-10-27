"""FaFa Python AI 服务包。

架构定位（架构规范 §1 / §10）：
    - 只执行受控的 AI/OCR/检索计算，输出回答或候选草稿；
    - 不直连、不写入业务 MySQL；不拥有业务事实；
    - 所有业务写操作回到 Java fafa-biz。

包结构：
    api/          FastAPI 路由与 Pydantic 请求响应模型
    application/  用例与授权上下文编排
    workflows/    LangGraph 工作流（图/节点/checkpoint）
    integrations/ Qwen、Qdrant、对象存储、OCR 适配器
    tools/        FastMCP 受控工具（allowlist）
    messaging/    RabbitMQ 消费者、幂等与结果回传
    config/       统一 Settings 与密钥读取
"""

__version__ = "0.1.0"
