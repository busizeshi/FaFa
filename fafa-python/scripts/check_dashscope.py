"""
百炼连通性验证脚本（技术文档 M4 里程碑验收）

执行一次 qwen 轻量模型调用，验证 API Key 与 OpenAI 兼容入口可用。

用法：
    python scripts/check_dashscope.py
"""

import sys
from pathlib import Path

# 允许从项目根目录直接运行
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from app.core.config import get_settings
from app.core.llm import Purpose, get_chat_model
from app.core.logging import setup_logging


def main() -> None:
    setup_logging()
    settings = get_settings()

    if not settings.dashscope_api_key:
        print("[FAIL] DASHSCOPE_API_KEY 未配置（检查 .env 或环境变量）")
        sys.exit(1)

    print(f"测试模型: {settings.model_chat_light}")
    print(f"入口: {settings.dashscope_base_url}")

    model = get_chat_model(Purpose.CHAT_LIGHT)
    response = model.invoke("请只回复两个字：正常")
    content = response.content

    print(f"模型返回: {content}")
    print("[OK] 百炼调用成功" if "正常" in str(content) else "[WARN] 返回内容异常，请检查模型配置")


if __name__ == "__main__":
    main()
