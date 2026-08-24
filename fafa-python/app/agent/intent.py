"""AI 对话意图识别。

路由先使用可测试的确定性规则，避免每条消息都额外消耗一次模型调用。
"""

from enum import StrEnum


class Intent(StrEnum):
    """已开放的对话意图。"""

    DATA_QUERY = "data_query"
    TREND_ANALYSIS = "trend_analysis"
    EXPENSE_QUERY = "expense_query"
    REPORT_QUERY = "report_query"
    MEDIA_SEARCH = "media_search"
    RECORD_DRAFT = "record_draft"
    UNSUPPORTED = "unsupported_feature"
    GENERAL = "general_pet_question"


def classify_intent(message: str) -> Intent:
    """根据用户原文选择最小工具计划。"""

    text = message.strip().lower()
    if any(word in text for word in ("小红书", "就医摘要", "分享笔记")):
        return Intent.UNSUPPORTED
    if any(word in text for word in ("月报", "季报", "年报", "周期报告", "成长报告")):
        return Intent.UNSUPPORTED if "生成" in text or "创建" in text else Intent.REPORT_QUERY
    if any(word in text for word in ("记录一下", "记录", "打卡", "称一下", "体重是", "喂了")):
        return Intent.RECORD_DRAFT
    if any(word in text for word in ("照片", "图片", "视频", "素材", "找豆包", "找一下")):
        return Intent.MEDIA_SEARCH
    if any(word in text for word in ("开支", "花了多少钱", "花费", "费用", "账单", "支出", "消费", "预算")):
        return Intent.EXPENSE_QUERY
    if any(word in text for word in ("趋势", "变化", "最近怎么样", "体重怎么样", "走势")):
        return Intent.TREND_ANALYSIS
    if any(word in text for word in ("拒食", "食欲", "精神", "饮水", "喝水", "排便", "喂食", "吃了", "提醒")):
        return Intent.DATA_QUERY
    return Intent.GENERAL
