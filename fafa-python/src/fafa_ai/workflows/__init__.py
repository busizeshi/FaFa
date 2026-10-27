"""LangGraph 工作流包。

约定（架构规范 §3 / §6.6）：
    - LangGraph 用于<strong>有状态、需要 checkpoint/恢复、流式步骤或人工确认</strong>的流程
      （如：AI 照护记录解析 → 候选草稿 → 用户确认）；
    - 简单单次模型调用不强行包装成 Agent/Graph，直接走 LangChain 集成；
    - 图（graph）、节点（node）、checkpoint 存储按工作流分文件组织，命名如
      ``care_draft.py``（照护记录草稿工作流）。

提示词安全：外部内容（OCR 文本、用户输入）一律作为不可信数据拼接，禁止拼入系统指令。
"""
