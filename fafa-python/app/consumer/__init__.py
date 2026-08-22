"""
RocketMQ 消费者（仅 Linux 环境启用）

Windows 开发环境不支持 rocketmq Python 客户端，
开发链路由 Java 直接 HTTP 调用 /api/photos/analyze 替代（见技术文档 7.1）。

TODO Linux 部署时实现：
- Consumer Group: fafa_python_photo_group
- Topic: fafa_photo_analysis_topic（Tag: photo）
- 消息体 camelCase → snake_case 映射
- 以 messageId / photo_id 幂等判重
- 失败重试 3 次后进死信，日志告警人工处理
"""
