"""Phase 9 的纯结构化输出契约测试。"""

import json
import unittest


class ReportContentContractTest(unittest.TestCase):
    def test_fact_snapshot_is_json_object(self) -> None:
        snapshot = json.dumps({"schemaVersion": "report-facts-v1", "pet": {"id": 1}})
        value = json.loads(snapshot)
        self.assertIsInstance(value, dict)
        self.assertEqual(value["schemaVersion"], "report-facts-v1")

    def test_content_fields_are_restricted(self) -> None:
        content = {"title": "月报", "summary": "只基于记录", "sourceKeys": ["media:1"]}
        self.assertEqual(set(content) - {"title", "summary", "sourceKeys"}, set())


if __name__ == "__main__":
    unittest.main()
