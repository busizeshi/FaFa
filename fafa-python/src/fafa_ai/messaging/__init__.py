"""RabbitMQ 消息层：AI 长任务消费、幂等与结果回传。

约定（架构规范 §6.3 / §9.3）：
    - Python AI Worker 消费指定队列（durable queue + persistent message）；
    - 手动 ACK：完成持久副作用（调用 biz 回传接口成功）后才 ACK；
      处理后、ACK 前崩溃会重投，因此以 ``messageId/jobId`` 唯一键去重（幂等）；
    - 可恢复错误：有限次数延迟重试 + 退避；不可恢复/超限消息进入 DLQ；
      禁止无上限 requeue；
    - 消息信封字段：messageId / eventId / eventType / schemaVersion / jobId /
      occurredAt / traceId；消息内<strong>禁止</strong>携带病历原文、图片二进制或长期签名 URL，
      消费者按 jobId 调用 biz 接口取数；
    - 结果回传：处理结果通过 biz 私网回传接口提交，job 状态只由 biz 持久化。
"""
