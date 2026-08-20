# RocketMQ 消息流程说明

## 一、架构概览

```
Java 服务 (Producer)  ---[RocketMQ]---> Python 服务 (Consumer)
    ↓                                        ↓
发送照片分析消息                          接收并处理消息
```

## 二、Java 端（生产者）

### 1. 配置信息

**文件**: `fafa-java/src/main/resources/application-dev.yml`

```yaml
rocketmq:
  name-server: 192.168.1.14:9876
  producer:
    group: fafa-producer-group
    send-message-timeout: 3000
    retry-times-when-send-failed: 2
```

### 2. 消息发送

**文件**: `com.fafa.infrastructure.mq.MqProducerService`

```java
// Topic 名称
private static final String PHOTO_ANALYSIS_TOPIC = "photo-analysis";

// 发送消息
public void sendPhotoAnalysisMessage(PhotoAnalysisMessage message) {
    String jsonMessage = objectMapper.writeValueAsString(message);
    rocketMQTemplate.syncSend(
        PHOTO_ANALYSIS_TOPIC,
        MessageBuilder.withPayload(jsonMessage).build()
    );
}
```

### 3. 消息格式

**文件**: `com.fafa.infrastructure.mq.PhotoAnalysisMessage`

```json
{
  "photoId": 1001,
  "petId": 2001,
  "userId": 3001,
  "url": "http://192.168.1.14:9000/fafa/photos/xxx.jpg",
  "thumbnailUrl": "http://192.168.1.14:9000/fafa/photos/thumb_xxx.jpg",
  "takenAt": "2024-08-19T12:00:00"
}
```

## 三、Python 端（消费者）

### 1. 配置信息

**文件**: `fafa-python/app/core/config.py`

```python
# RocketMQ 配置
ROCKETMQ_NAME_SERVER: str = "192.168.1.14:9876"

# RocketMQ Topics
ROCKETMQ_TOPIC_PHOTO_ANALYSIS: str = "photo-analysis"

# RocketMQ Consumer Groups
ROCKETMQ_CONSUMER_GROUP_PHOTO: str = "fafa-photo-consumer"
```

### 2. 消费者启动

**文件**: `fafa-python/main.py`

```python
@asynccontextmanager
async def lifespan(app: FastAPI):
    """应用生命周期管理"""
    # 启动 RocketMQ 消费者
    start_photo_consumer()
    
    yield
    
    # 关闭 RocketMQ 消费者
    stop_photo_consumer()
```

### 3. 消息处理

**文件**: `fafa-python/app/consumer/photo_consumer.py`

```python
class PhotoAnalysisConsumer:
    """照片分析消费者"""
    
    def start(self):
        """启动消费者"""
        self.consumer = PushConsumer('photo-analysis-consumer-group')
        self.consumer.set_name_server_address(settings.ROCKETMQ_NAME_SERVER)
        self.consumer.subscribe('photo-analysis', self._callback)
        self.consumer.start()
    
    def _callback(self, msg):
        """消息回调处理"""
        # 1. 解析消息
        body = msg.body.decode('utf-8')
        message_data = json.loads(body)
        
        # 2. 异步处理照片分析
        asyncio.create_task(self._process_photo_analysis(message_data))
        
        # 3. 返回消费状态
        return ConsumeStatus.CONSUME_SUCCESS
```

## 四、消息流程

### 1. Java 发送消息

```java
// 1. 构建消息对象
PhotoAnalysisMessage message = PhotoAnalysisMessage.builder()
    .photoId(photoId)
    .petId(petId)
    .userId(userId)
    .url(photoUrl)
    .thumbnailUrl(thumbnailUrl)
    .takenAt(takenAt)
    .build();

// 2. 发送到 RocketMQ
mqProducerService.sendPhotoAnalysisMessage(message);
```

### 2. Python 接收消息

```python
# 1. RocketMQ 推送消息到消费者
# 2. 调用 _callback 回调函数
# 3. 解析 JSON 消息体
# 4. 创建异步任务处理照片分析
# 5. 返回消费成功状态
```

### 3. 消息处理流程

```
Java 发送消息
    ↓
RocketMQ Broker (192.168.1.14:9876)
    ↓
Python Consumer 接收
    ↓
解析 JSON 消息
    ↓
调用 PhotoAnalysisService
    ↓
AI 照片分析处理
    ↓
保存分析结果到数据库
```

## 五、关键配置项对比

| 配置项 | Java (Producer) | Python (Consumer) |
|--------|----------------|-------------------|
| NameServer | 192.168.1.14:9876 | 192.168.1.14:9876 |
| Topic | photo-analysis | photo-analysis |
| Group | fafa-producer-group | photo-analysis-consumer-group |
| 序列化 | JSON (Jackson) | JSON (json.loads) |

## 六、注意事项

### 1. 消费者组命名

- Java 和 Python 必须使用**不同的消费者组名**
- Java: `fafa-producer-group`
- Python: `photo-analysis-consumer-group`

### 2. Topic 命名

- 两端必须使用**相同的 Topic 名称**: `photo-analysis`

### 3. 消息格式

- 使用 JSON 格式传输
- Java 端序列化为 JSON 字符串
- Python 端反序列化为字典对象

### 4. 消费模式

- Python 使用 **Push 模式**（PushConsumer）
- RocketMQ 主动推送消息到消费者

### 5. 错误处理

- 消费成功：返回 `ConsumeStatus.CONSUME_SUCCESS`
- 消费失败：返回 `ConsumeStatus.RECONSUME_LATER`（会重新消费）

## 七、启动顺序

1. **启动 RocketMQ** (192.168.1.14:9876)
2. **启动 Python 服务** (消费者先启动，等待消息)
3. **启动 Java 服务** (生产者发送消息)

## 八、验证方法

### 1. 查看 Python 日志

```
RocketMQ 照片分析消费者启动成功
收到照片分析消息: photoId=1001
照片分析完成: photoId=1001
```

### 2. 查看 Java 日志

```
发送照片分析消息成功，photoId=1001, petId=2001
```

### 3. 检查 RocketMQ 控制台

- 访问 RocketMQ Dashboard
- 查看 Topic: `photo-analysis`
- 查看消费者组: `photo-analysis-consumer-group`
- 查看消费进度和堆积情况
