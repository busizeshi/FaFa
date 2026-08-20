# RocketMQ 实战部署指南

## 一、环境准备

### 1.1 Windows 开发环境 vs Linux 生产环境

| 环境 | RocketMQ 支持 | 解决方案 |
|------|--------------|----------|
| Windows 开发 | ❌ Python 客户端不支持 | 使用 HTTP 回调接口 |
| Linux 生产 | ✅ 完全支持 | 使用 RocketMQ 消费者 |

### 1.2 系统要求

```bash
# Linux 环境（CentOS/Ubuntu/Debian）
Python 3.8+
RocketMQ Server 4.9+
Java 8+ (RocketMQ 依赖)
```

---

## 二、部署 RocketMQ Server

### 2.1 下载和安装

```bash
# 1. 下载 RocketMQ
cd /opt
wget https://archive.apache.org/dist/rocketmq/4.9.4/rocketmq-all-4.9.4-bin-release.zip
unzip rocketmq-all-4.9.4-bin-release.zip
cd rocketmq-4.9.4

# 2. 启动 NameServer
nohup sh bin/mqnamesrv &
tail -f ~/logs/rocketmqlogs/namesrv.log

# 3. 启动 Broker
nohup sh bin/mqbroker -n 192.168.1.14:9876 &
tail -f ~/logs/rocketmqlogs/broker.log
```

### 2.2 验证安装

```bash
# 检查进程
ps aux | grep rocketmq

# 应该看到两个进程：
# - NamesrvStartup (NameServer)
# - BrokerStartup (Broker)
```

### 2.3 创建 Topic

```bash
# 创建 photo-analysis Topic
sh bin/mqadmin updateTopic \
  -n 192.168.1.14:9876 \
  -t photo-analysis \
  -c DefaultCluster

# 查看 Topic 列表
sh bin/mqadmin topicList -n 192.168.1.14:9876
```

---

## 三、部署 Python 服务（Linux）

### 3.1 上传代码到 Linux 服务器

```bash
# 使用 scp 或 git clone
scp -r fafa-python root@192.168.1.14:/opt/
# 或
cd /opt
git clone <your-repo-url> fafa-python
```

### 3.2 安装 Python 依赖

```bash
cd /opt/fafa-python

# 创建虚拟环境
python3 -m venv .venv
source .venv/bin/activate

# 安装依赖
pip install -r requirements.txt

# 验证 rocketmq-client-python 安装成功
python -c "from rocketmq.client import PushConsumer; print('RocketMQ 客户端安装成功')"
```

### 3.3 配置环境变量

```bash
# 创建 .env 文件
cat > .env <<EOF
# RocketMQ 配置
ROCKETMQ_NAME_SERVER=192.168.1.14:9876

# MySQL 配置
MYSQL_HOST=192.168.1.14
MYSQL_PORT=3306
MYSQL_DATABASE=fafa
MYSQL_USERNAME=fafa
MYSQL_PASSWORD=fafa_123456

# Redis 配置
REDIS_HOST=192.168.1.14
REDIS_PORT=6379

# 通义千问 API
DASHSCOPE_API_KEY=your_api_key_here
EOF
```

### 3.4 启用 RocketMQ 消费者

```bash
# 编辑 main.py，取消注释
vim main.py
```

找到这几行，**取消注释**：

```python
# 修改前（注释状态）
# from app.consumer.photo_consumer import start_photo_consumer, stop_photo_consumer

# 修改后（取消注释）
from app.consumer.photo_consumer import start_photo_consumer, stop_photo_consumer
```

```python
# 修改前（注释状态）
# try:
#     start_photo_consumer()
#     logger.info("RocketMQ 消费者启动成功")
# except Exception as e:
#     logger.error(f"RocketMQ 消费者启动失败: {e}")

# 修改后（取消注释）
try:
    start_photo_consumer()
    logger.info("RocketMQ 消费者启动成功")
except Exception as e:
    logger.error(f"RocketMQ 消费者启动失败: {e}")
```

### 3.5 启动服务

```bash
# 前台运行（测试用）
python main.py

# 后台运行（生产环境）
nohup python main.py > logs/python-service.log 2>&1 &

# 查看日志
tail -f logs/python-service.log
```

---

## 四、部署 Java 服务

### 4.1 打包 Java 项目

```bash
# 在 Windows 开发机上打包
cd D:\dev\Java\FaFa\fafa-java
mvn clean package -DskipTests

# 生成的文件：target/fafa-java-1.0.0.jar
```

### 4.2 上传到 Linux 服务器

```bash
scp target/fafa-java-1.0.0.jar root@192.168.1.14:/opt/fafa-java/
```

### 4.3 启动 Java 服务

```bash
cd /opt/fafa-java

# 启动服务
nohup java -jar fafa-java-1.0.0.jar \
  --spring.profiles.active=prod \
  > logs/java-service.log 2>&1 &

# 查看日志
tail -f logs/java-service.log
```

---

## 五、完整测试流程

### 5.1 测试消息发送（Java 端）

```bash
# 方式 1：使用 curl 上传照片
curl -X POST http://192.168.1.14:8080/api/photos/upload \
  -H "Authorization: Bearer <token>" \
  -F "file=@test.jpg" \
  -F "petId=1"

# 方式 2：查看 Java 日志
tail -f /opt/fafa-java/logs/java-service.log
```

**期望看到的日志**：
```
2024-08-19 18:00:00.123 INFO  MqProducerService - 发送照片分析消息成功，photoId=1001, petId=2001
```

### 5.2 测试消息接收（Python 端）

```bash
# 查看 Python 日志
tail -f /opt/fafa-python/logs/python-service.log
```

**期望看到的日志**：
```
2024-08-19 18:00:00.125 | INFO | RocketMQ 消费者启动成功，等待消息...
2024-08-19 18:00:00.456 | INFO | 收到照片分析消息: photoId=1001
2024-08-19 18:00:05.789 | INFO | 照片分析完成: photoId=1001
```

### 5.3 验证消费进度

```bash
# 使用 RocketMQ 命令查看消费进度
cd /opt/rocketmq-4.9.4
sh bin/mqadmin consumerProgress -g photo-analysis-consumer-group -n 192.168.1.14:9876
```

**输出示例**：
```
#Topic                    #Broker Name     #QID   #Broker Offset   #Consumer Offset   #Diff
photo-analysis            broker-a         0      100              100                0
photo-analysis            broker-a         1      98               98                 0
photo-analysis            broker-a         2      102              102                0
photo-analysis            broker-a         3      95               95                 0

Consume TPS: 5.2
Diff Total: 0
```

**字段说明**：
- `Broker Offset`：Broker 中的消息总数
- `Consumer Offset`：消费者已消费的消息数
- `Diff`：堆积的消息数（应该为 0）

---

## 六、监控和运维

### 6.1 安装 RocketMQ Dashboard

```bash
# 1. 下载 Dashboard
cd /opt
git clone https://github.com/apache/rocketmq-dashboard.git
cd rocketmq-dashboard

# 2. 修改配置
vim src/main/resources/application.properties
# 修改：rocketmq.config.namesrvAddr=192.168.1.14:9876

# 3. 编译运行
mvn clean package -DskipTests
java -jar target/rocketmq-dashboard-1.0.0.jar

# 4. 访问 Dashboard
http://192.168.1.14:8080
```

### 6.2 常用监控命令

```bash
# 查看 Topic 状态
sh bin/mqadmin topicStatus -n 192.168.1.14:9876 -t photo-analysis

# 查看消费者组状态
sh bin/mqadmin consumerProgress -g photo-analysis-consumer-group -n 192.168.1.14:9876

# 查看消息堆积
sh bin/mqadmin consumerStatus -g photo-analysis-consumer-group -n 192.168.1.14:9876

# 重置消费位点（慎用！）
sh bin/mqadmin resetOffsetByTime \
  -n 192.168.1.14:9876 \
  -g photo-analysis-consumer-group \
  -t photo-analysis \
  -s -1  # -1 表示重置到最新
```

### 6.3 日志管理

```bash
# Python 服务日志
tail -f /opt/fafa-python/logs/python-service.log

# Java 服务日志
tail -f /opt/fafa-java/logs/java-service.log

# RocketMQ NameServer 日志
tail -f ~/logs/rocketmqlogs/namesrv.log

# RocketMQ Broker 日志
tail -f ~/logs/rocketmqlogs/broker.log
```

---

## 七、常见问题排查

### 问题 1：消费者无法连接 NameServer

**症状**：
```
ERROR | 连接 NameServer 失败: Connection refused
```

**排查步骤**：
```bash
# 1. 检查 NameServer 是否启动
ps aux | grep namesrv

# 2. 检查端口是否监听
netstat -nltp | grep 9876

# 3. 检查防火墙
systemctl status firewalld
firewall-cmd --list-ports
```

**解决方案**：
```bash
# 开放端口
firewall-cmd --add-port=9876/tcp --permanent
firewall-cmd --add-port=10911/tcp --permanent
firewall-cmd --reload
```

### 问题 2：消息堆积

**症状**：
```
Diff Total: 1000  # 有 1000 条消息未消费
```

**排查步骤**：
```bash
# 1. 查看消费者是否在线
sh bin/mqadmin consumerConnection -g photo-analysis-consumer-group -n 192.168.1.14:9876

# 2. 查看消费速度
sh bin/mqadmin consumerProgress -g photo-analysis-consumer-group -n 192.168.1.14:9876

# 3. 查看 Python 服务日志
tail -f /opt/fafa-python/logs/python-service.log | grep ERROR
```

**解决方案**：
- 增加消费者实例数量
- 优化消费逻辑（减少处理时间）
- 检查 AI 服务是否正常

### 问题 3：消息重复消费

**症状**：
```
同一个 photoId 被分析了多次
```

**原因**：
- 消费者处理超时，RocketMQ 认为消费失败
- 消费者返回了 RECONSUME_LATER

**解决方案**：
```python
# 在消费逻辑中增加幂等性检查
async def _process_photo_analysis(self, message_data: dict):
    photo_id = message_data.get('photoId')
    
    # 检查是否已经分析过
    photo = await photo_repository.get_by_id(photo_id)
    if photo.analysis_status == 'completed':
        logger.info(f"照片 {photo_id} 已经分析过，跳过")
        return
    
    # 继续分析...
```

### 问题 4：Windows 开发环境测试

**解决方案**：使用 HTTP 回调接口

```bash
# 在 Windows 开发环境测试
curl -X POST http://localhost:8000/api/callback/photo-analysis \
  -H "Content-Type: application/json" \
  -d '{
    "photoId": 1001,
    "petId": 2001,
    "userId": 3001,
    "url": "http://192.168.1.14:9000/fafa/test.jpg"
  }'
```

---

## 八、性能优化

### 8.1 增加消费者并发

```python
# 方式 1：启动多个 Python 服务实例
# 在不同端口启动多个实例，它们会自动负载均衡

# 服务器 1
python main.py --port 8000

# 服务器 2
python main.py --port 8000

# 方式 2：增加消费线程（修改 RocketMQ 配置）
self.consumer.set_message_model(ConsumeMode.CLUSTERING)  # 集群模式
```

### 8.2 调整批量大小

```python
# 批量拉取消息
self.consumer.set_pull_batch_size(10)  # 一次拉取 10 条
```

### 8.3 异步处理优化

```python
# 使用线程池处理 IO 密集型任务
from concurrent.futures import ThreadPoolExecutor

executor = ThreadPoolExecutor(max_workers=10)

async def _process_photo_analysis(self, message_data: dict):
    # 异步调用 AI 服务
    loop = asyncio.get_event_loop()
    result = await loop.run_in_executor(
        executor,
        sync_ai_service.analyze,
        message_data['url']
    )
```

---

## 九、生产环境部署检查清单

### 部署前检查

- [ ] RocketMQ NameServer 已启动
- [ ] RocketMQ Broker 已启动
- [ ] Topic `photo-analysis` 已创建
- [ ] MySQL 数据库可连接
- [ ] Redis 可连接
- [ ] 通义千问 API Key 已配置
- [ ] 防火墙端口已开放（9876, 10911）

### 代码检查

- [ ] Python `main.py` 中 RocketMQ 消费者代码已取消注释
- [ ] 配置文件 `.env` 已正确配置
- [ ] 日志目录已创建
- [ ] requirements.txt 依赖已安装

### 启动顺序

1. ✅ 启动 RocketMQ NameServer
2. ✅ 启动 RocketMQ Broker
3. ✅ 启动 MySQL
4. ✅ 启动 Redis
5. ✅ 启动 Python 服务（消费者）
6. ✅ 启动 Java 服务（生产者）

### 测试验证

- [ ] Java 服务可以发送消息
- [ ] Python 服务可以接收消息
- [ ] 消息处理成功，无堆积
- [ ] Dashboard 可以访问
- [ ] 日志正常输出

---

## 十、总结

### 开发环境（Windows）
```
Java 服务 → HTTP 回调 → Python 服务
          (临时方案)
```

### 生产环境（Linux）
```
Java 服务 → RocketMQ → Python 服务
          (正式方案)
```

### 关键配置对照表

| 配置项 | Java | Python | 说明 |
|--------|------|--------|------|
| NameServer | 192.168.1.14:9876 | 192.168.1.14:9876 | 必须一致 |
| Topic | photo-analysis | photo-analysis | 必须一致 |
| Producer Group | fafa-producer-group | - | 生产者组 |
| Consumer Group | - | photo-analysis-consumer-group | 消费者组 |

---

希望这份部署指南能帮助你顺利部署到 Linux 生产环境！🚀
