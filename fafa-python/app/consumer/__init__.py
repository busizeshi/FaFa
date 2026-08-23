"""
RocketMQ 消费者（仅 Linux 环境启用）

Windows 开发环境不支持 rocketmq Python 客户端，
开发链路由 Java 直接 HTTP 调用替代（见技术文档 7.1）。

已实现：
- photo_consumer：宠物照片向量化消费者
  - Consumer Group: fafa_python_photo_group
  - Topic: fafa_pet_photo_vectorization
  - 消息体 camelCase → snake_case 映射
  - point_id 确定性生成，upsert 幂等，重复消费无副作用
  - 失败返回 RECONSUME_LATER 由 broker 重试，超限进死信人工处理
"""
