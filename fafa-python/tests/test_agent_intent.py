"""Agent 意图路由测试。"""

import unittest

from app.agent.intent import Intent, classify_intent


class AgentIntentTest(unittest.TestCase):
    def test_classify_media_search(self) -> None:
        self.assertEqual(classify_intent("帮我找去年夏天在阳台的照片和视频"), Intent.MEDIA_SEARCH)

    def test_classify_unsupported_report(self) -> None:
        self.assertEqual(classify_intent("生成一份八月月报"), Intent.UNSUPPORTED)

    def test_classify_daily_query(self) -> None:
        self.assertEqual(classify_intent("豆包上个月有几天拒食？"), Intent.DATA_QUERY)

    def test_classify_expense_query(self) -> None:
        self.assertEqual(classify_intent("最近一个月花了多少钱？"), Intent.EXPENSE_QUERY)


if __name__ == "__main__":
    unittest.main()
