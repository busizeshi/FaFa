# RocketMQ 学习资料索引

## 📚 学习路径

作为一个新手，建议按照以下顺序学习：

### 第一阶段：理解基础概念（30分钟）
📖 **阅读顺序**：
1. [RocketMQ学习指南.md](./RocketMQ学习指南.md) - 从零开始的完整教程
   - 第一部分：什么是消息队列？（生活中的例子）
   - 第二部分：RocketMQ 核心概念（NameServer、Broker、Topic）
   - 第三部分：Java 端详解（如何发送消息）
   - 第四部分：Python 端详解（如何接收消息）
   - 第五部分：完整流程串讲（时序图）

**学习目标**：
- ✅ 理解消息队列的作用
- ✅ 掌握 Topic、Producer、Consumer 等概念
- ✅ 知道消息是如何从 Java 传到 Python 的

---

### 第二阶段：看懂流程图（20分钟）
📖 **阅读顺序**：
2. [RocketMQ消息流转图解.md](./RocketMQ消息流转图解.md) - 可视化流程
   - 一、整体架构图
   - 二、照片上传完整流程图
   - 三、消息流转详细步骤
   - 七、对比：同步调用 vs 消息队列

**学习目标**：
- ✅ 看懂系统架构
- ✅ 理解每个步骤的耗时
- ✅ 明白为什么要用消息队列

---

### 第三阶段：实战部署（1小时）
📖 **阅读顺序**：
3. [RocketMQ部署指南.md](./RocketMQ部署指南.md) - 生产环境部署
   - 二、部署 RocketMQ Server
   - 三、部署 Python 服务（Linux）
   - 四、部署 Java 服务
   - 五、完整测试流程

**学习目标**：
- ✅ 在 Linux 上部署 RocketMQ
- ✅ 配置 Java 和 Python 服务
- ✅ 完成端到端测试

---

### 第四阶段：解决问题（30分钟）
📖 **阅读顺序**：
4. [RocketMQ部署指南.md](./RocketMQ部署指南.md) - 故障排查
   - 七、常见问题排查
   - 问题 1：消费者无法连接 NameServer
   - 问题 2：消息堆积
   - 问题 3：消息重复消费
   - 问题 4：Windows 开发环境测试

**学习目标**：
- ✅ 会排查常见问题
- ✅ 会查看日志和监控
- ✅ 知道如何优化性能

---

## 🗂️ 文档清单

### 1. RocketMQ学习指南.md
**内容**：从零开始的完整教程，适合新手
**篇幅**：约 8000 字
**亮点**：
- 📝 用快递站的例子解释消息队列
- 💻 每段代码都有详细注释
- 🎯 有练习题和记忆口诀
- ⏱️ 标注了每个步骤的耗时

**适合人群**：完全不懂消息队列的新手

---

### 2. RocketMQ消息流转图解.md
**内容**：可视化的流程图和架构图
**篇幅**：约 5000 字
**亮点**：
- 🎨 ASCII 艺术图，直观易懂
- 📊 完整的时序图
- 🔄 对比同步 vs 异步的区别
- 🧩 展示数据在各层的转换过程

**适合人群**：视觉学习型，喜欢看图的人

---

### 3. RocketMQ部署指南.md
**内容**：生产环境部署和运维
**篇幅**：约 6000 字
**亮点**：
- 🚀 完整的部署步骤（复制粘贴即可）
- 🔍 详细的故障排查方法
- 📈 性能优化建议
- ✅ 部署检查清单

**适合人群**：需要实际部署到服务器的人

---

### 4. RocketMQ消息流程.md
**内容**：架构概览和配置说明（之前创建的）
**篇幅**：约 3000 字
**亮点**：
- 📋 配置项对照表
- 🔑 关键配置说明
- ⚠️ 注意事项

**适合人群**：快速查阅配置的人

---

## 🎓 学习建议

### 对于完全新手
```
第 1 天：阅读"学习指南"的前两部分
        理解：什么是消息队列？为什么需要它？
        
第 2 天：阅读"学习指南"的第三、四部分
        理解：Java 如何发送？Python 如何接收？
        
第 3 天：阅读"消息流转图解"
        理解：完整的流程是怎样的？
        
第 4 天：动手部署（Linux 环境）
        跟着"部署指南"一步步操作
```

### 对于有经验的开发者
```
直接看"消息流转图解" → 理解架构
然后看"部署指南" → 快速部署
遇到问题查"学习指南"的对应章节
```

---

## 💡 快速答疑

### Q1：为什么 Windows 不能用？
**答**：`rocketmq-client-python` 库依赖 Linux 系统调用，不支持 Windows。
**解决**：
- 开发环境：使用 HTTP 回调接口（已实现在 `callback.py`）
- 生产环境：部署到 Linux 服务器（正常使用 RocketMQ）

### Q2：消息会丢失吗？
**答**：不会。RocketMQ 会把消息持久化到磁盘。
- Java 发送成功 → 消息已保存
- Python 处理失败 → 自动重试（最多 16 次）
- 全部失败 → 进入死信队列，人工处理

### Q3：能保证消息顺序吗？
**答**：默认不保证。如果需要顺序，要：
- 使用顺序消息
- 把同一组消息发到同一个 Queue

### Q4：如何监控消息堆积？
**答**：
- 方式 1：RocketMQ Dashboard（Web 界面）
- 方式 2：命令行工具 `mqadmin consumerProgress`
- 方式 3：查看日志中的 TPS（每秒处理数）

### Q5：消息重复消费怎么办？
**答**：在业务代码中做幂等性处理。
```python
# 检查是否已处理过
if photo.status == 'analyzed':
    logger.info("已分析过，跳过")
    return
```

---

## 🔧 代码位置

### Java 端（生产者）
```
fafa-java/
├── src/main/java/com/fafa/infrastructure/mq/
│   ├── MqProducerService.java          # 发送消息的服务
│   └── PhotoAnalysisMessage.java       # 消息实体类
└── src/main/resources/
    └── application-dev.yml              # RocketMQ 配置
```

### Python 端（消费者）
```
fafa-python/
├── app/
│   ├── consumer/
│   │   └── photo_consumer.py           # 消费者实现
│   ├── api/
│   │   └── callback.py                 # HTTP 回调接口（Windows）
│   └── core/
│       └── config.py                   # RocketMQ 配置
└── main.py                             # 启动消费者
```

---

## 📞 遇到问题？

### 开发环境问题（Windows）
1. 使用 HTTP 回调接口测试
2. 查看 Python 日志：`logs/python-service.log`
3. 查看 Java 日志：`logs/java-service.log`

### 生产环境问题（Linux）
1. 检查 RocketMQ 是否启动
   ```bash
   ps aux | grep rocketmq
   ```
2. 检查消费者是否在线
   ```bash
   sh bin/mqadmin consumerConnection -g photo-analysis-consumer-group -n 192.168.1.14:9876
   ```
3. 查看消息堆积
   ```bash
   sh bin/mqadmin consumerProgress -g photo-analysis-consumer-group -n 192.168.1.14:9876
   ```

---

## 🎯 核心要点总结

### 记住这 5 点
1. **Topic 要一致**：Java 和 Python 使用相同的 Topic 名称
2. **Group 要不同**：生产者组和消费者组必须不同
3. **JSON 传输**：消息用 JSON 格式传输
4. **回调处理**：Python 的 `_callback` 函数是核心
5. **返回状态**：`CONSUME_SUCCESS` 或 `RECONSUME_LATER`

### 一句话总结
```
Java 把消息发到 RocketMQ，
Python 从 RocketMQ 取消息，
两边通过 Topic 名称对应。
```

---

## 📖 推荐阅读顺序

```
新手路径：
学习指南 (基础) → 流转图解 (可视化) → 部署指南 (实战)

快速路径：
流转图解 (看懂架构) → 部署指南 (直接部署)

查阅路径：
消息流程 (配置速查) → 学习指南 (详细说明)
```

---

## 🌟 最后的话

消息队列不难，核心就是：
- **发送方**：把数据打包成 JSON，发到指定 Topic
- **接收方**：订阅 Topic，收到消息后处理

就像寄快递一样简单！📦

祝你学习顺利！如果有任何问题，随时查阅这些文档。🚀
