"""
报告生成服务：周报 / 月报 / 季报 / 年报 + 分享笔记

TODO P1/P2 实现，AI 总结原则：信息密度高于语言修饰。
"""

from loguru import logger


class ReportService:
    """报告生成服务"""

    async def generate_monthly_report(self, user_id: int, pet_id: int, period: str) -> str:
        """生成月报"""
        logger.info("生成月报: user_id={}, pet_id={}, period={}", user_id, pet_id, period)
        raise NotImplementedError("报告生成随 P1 实现")
