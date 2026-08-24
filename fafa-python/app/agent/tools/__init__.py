"""AI 数据查询工具包。

Phase 5 已实现的工具集中在 :mod:`app.agent.tools.registry`，统一经 Java
内部只读接口取数；未落地的健康、食品、报告和记账工具不会被注册给 Agent。
"""

from app.agent.tools.registry import ToolRegistry

__all__ = ["ToolRegistry"]
