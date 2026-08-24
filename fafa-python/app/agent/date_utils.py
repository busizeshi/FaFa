"""对话中相对日期的解析。"""

import calendar
import re
from datetime import date, timedelta


DATE_PATTERN = re.compile(r"(20\d{2})[-年](\d{1,2})[-月](\d{1,2})")


def parse_date_range(message: str, today: date | None = None) -> tuple[date, date]:
    """将常见中文时间表达转为闭区间日期。"""

    current = today or date.today()
    exact_dates = _parse_exact_dates(message)
    if len(exact_dates) >= 2:
        return min(exact_dates[:2]), max(exact_dates[:2])
    if "去年夏天" in message:
        return date(current.year - 1, 6, 1), date(current.year - 1, 8, 31)
    if "上个月" in message:
        year, month = _shift_month(current.year, current.month, -1)
        return date(year, month, 1), date(year, month, calendar.monthrange(year, month)[1])
    if "本月" in message or "这个月" in message:
        return date(current.year, current.month, 1), current
    days_match = re.search(r"近(\d{1,3})天", message)
    if days_match:
        days = max(int(days_match.group(1)), 1)
        return current - timedelta(days=days - 1), current
    if any(word in message for word in ("最近", "近期", "这段时间")):
        return current - timedelta(days=29), current
    return current - timedelta(days=29), current


def _shift_month(year: int, month: int, delta: int) -> tuple[int, int]:
    """移动月份，避免引入额外日期依赖。"""

    index = year * 12 + month - 1 + delta
    return index // 12, index % 12 + 1


def _parse_exact_dates(message: str) -> list[date]:
    """忽略用户输入中的非法日期，让 Agent 回到默认近期范围。"""

    result: list[date] = []
    for year, month, day in DATE_PATTERN.findall(message):
        try:
            result.append(date(int(year), int(month), int(day)))
        except ValueError:
            continue
    return result
