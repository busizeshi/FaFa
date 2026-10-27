"""API 层：FastAPI 路由与 Pydantic 契约模型。

职责（架构规范 §5.2）：
    - 只做协议与格式：请求模型校验（Pydantic）、响应模型白名单序列化；
    - 用例编排委托 application 层；本层不执行模型调用与业务规则。
"""
