# RocketMQ 消息队列完整学习指南

## 第一部分：什么是消息队列？

### 1.1 生活中的例子

想象一个快递站：
- **寄件人（生产者）**：把包裹放到快递站
- **快递站（消息队列）**：暂存包裹
- **收件人（消费者）**：从快递站取包裹

**好处**：
- 寄件人不需要等收件人在家，放到快递站就可以走
- 收件人可以随时去取包裹
- 如果包裹很多，快递站可以排队处理

### 1.2 在我们项目中的应用

```
Java 服务（上传照片）
    ↓
"嘿，Python 服务，帮我分析这张照片"
    ↓
RocketMQ（消息队列）
    ↓
Python 服务（AI 分析照片）
```

**为什么需要消息队列？**
1. **解耦**：Java 不需要知道 Python 在哪里、怎么调用
2. **异步**：Java 发完消息就可以返回，不用等 AI 分析完（可能要 10 秒）
3. **削峰**：如果用户一次上传 100 张照片，Python 可以慢慢处理

---

## 第二部分：RocketMQ 核心概念

### 2.1 四大角色

#### ① NameServer（地址服务）
- 就像"114 查号台"
- 告诉生产者和消费者：消息队列在哪里

```
地址：192.168.1.14:9876
```

#### ② Broker（消息存储）
- 就像"快递仓库"
- 真正存储消息的地方

#### ③ Producer（生产者）
- 就像"寄件人"
- 在我们项目中：**Java 服务**

#### ④ Consumer（消费者）
- 就像"收件人"
- 在我们项目中：**Python 服务**

### 2.2 三大概念

#### ① Topic（主题）
- 就像"快递的类型"：生鲜、文件、家具
- 在我们项目中：`photo-analysis`（照片分析）

**为什么需要 Topic？**
- 不同的业务用不同的 Topic
- 消费者可以只订阅自己感兴趣的 Topic

#### ② Message（消息）
- 就像"具体的包裹"
- 包含要传递的数据

```json
{
  "photoId": 1001,
  "petId": 2001,
  "url": "http://xxx.jpg"
}
```

#### ③ Consumer Group（消费者组）
- 就像"收件地址"
- 同一个组里的消费者，每条消息只会被一个消费者处理

```
Java: fafa-producer-group
Python: photo-analysis-consumer-group
```

---

## 第三部分：Java 端（生产者）详解

### 3.1 依赖引入

**文件**：`pom.xml`

```xml
<dependency>
    <groupId>org.apache.rocketmq</groupId>
    <artifactId>rocketmq-spring-boot-starter</artifactId>
    <version>2.2.3</version>
</dependency>
```

### 3.2 配置文件

**文件**：`application-dev.yml`

```yaml
rocketmq:
  # NameServer 地址（必须）
  name-server: 192.168.1.14:9876
  
  producer:
    # 生产者组名（必须）
    group: fafa-producer-group
    # 发送超时时间（毫秒）
    send-message-timeout: 3000
    # 发送失败重试次数
    retry-times-when-send-failed: 2
```

**配置说明**：
- `name-server`：告诉 Java "消息队列在哪里"
- `group`：生产者的身份标识
- `send-message-timeout`：3 秒内没发送成功就算失败
- `retry-times-when-send-failed`：失败了重试 2 次

### 3.3 消息实体类

**文件**：`PhotoAnalysisMessage.java`

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhotoAnalysisMessage implements Serializable {
    
    private Long photoId;      // 照片 ID
    private Long petId;        // 宠物 ID
    private Long userId;       // 用户 ID
    private String url;        // 照片 URL
    private String thumbnailUrl;  // 缩略图 URL
    private String takenAt;    // 拍摄时间
}
```

**为什么要单独定义一个类？**
- 规范消息格式
- 方便序列化成 JSON
- 类型安全，不会传错字段

### 3.4 发送消息的服务

**文件**：`MqProducerService.java`

```java
@Service
public class MqProducerService {
    
    @Resource
    private RocketMQTemplate rocketMQTemplate;  // Spring 提供的发送工具
    
    private final ObjectMapper objectMapper = new ObjectMapper();  // JSON 工具
    
    // Topic 名称（常量）
    private static final String PHOTO_ANALYSIS_TOPIC = "photo-analysis";
    
    /**
     * 发送照片分析消息
     */
    public void sendPhotoAnalysisMessage(PhotoAnalysisMessage message) {
        try {
            // 第 1 步：把对象转成 JSON 字符串
            String jsonMessage = objectMapper.writeValueAsString(message);
            // 结果：{"photoId":1001,"petId":2001,...}
            
            // 第 2 步：构建 Spring Message
            Message<String> springMessage = MessageBuilder
                .withPayload(jsonMessage)  // 消息内容
                .build();
            
            // 第 3 步：发送消息
            rocketMQTemplate.syncSend(
                PHOTO_ANALYSIS_TOPIC,  // 发送到哪个 Topic
                springMessage          // 消息内容
            );
            
            log.info("发送照片分析消息成功，photoId={}", message.getPhotoId());
            
        } catch (JsonProcessingException e) {
            log.error("序列化照片分析消息失败", e);
            throw new RuntimeException("发送消息失败", e);
        }
    }
}
```

**代码解析**：

1. **为什么要转成 JSON？**
   - Java 对象不能直接传输
   - JSON 是通用格式，Python 也能解析

2. **syncSend 是什么意思？**
   - `sync`：同步发送，等消息发送成功才返回
   - 还有 `asyncSend`：异步发送，发完就返回，不等结果

3. **发送流程**：
```
Java 对象 → JSON 字符串 → Spring Message → RocketMQ
```

### 3.5 如何使用

**在需要发送消息的地方注入服务**：

```java
@Service
public class PhotoService {
    
    @Resource
    private MqProducerService mqProducerService;
    
    public void uploadPhoto(MultipartFile file) {
        // 1. 保存照片到 OSS
        String photoUrl = ossService.upload(file);
        
        // 2. 保存照片记录到数据库
        Photo photo = photoRepository.save(...);
        
        // 3. 发送消息到 RocketMQ，让 Python 去分析
        PhotoAnalysisMessage message = PhotoAnalysisMessage.builder()
            .photoId(photo.getId())
            .petId(photo.getPetId())
            .userId(photo.getUserId())
            .url(photoUrl)
            .build();
        
        mqProducerService.sendPhotoAnalysisMessage(message);
        
        // 4. 立即返回，不等 AI 分析完
        return "上传成功";
    }
}
```

**流程图**：
```
用户上传照片
    ↓
保存到 OSS (1 秒)
    ↓
保存到数据库 (0.1 秒)
    ↓
发送 MQ 消息 (0.01 秒)
    ↓
返回"上传成功" (总共 1.11 秒)
    ↓
Python 后台慢慢分析 (10 秒，用户不用等)
```

---

## 第四部分：Python 端（消费者）详解

### 4.1 依赖引入

**文件**：`requirements.txt`

```txt
rocketmq-client-python==2.0.0
```

**注意**：这个库不支持 Windows，只能在 Linux/Mac 上运行

### 4.2 配置文件

**文件**：`app/core/config.py`

```python
class Settings(BaseSettings):
    # RocketMQ 配置
    ROCKETMQ_NAME_SERVER: str = "192.168.1.14:9876"  # NameServer 地址
    
    # Topic 名称（必须和 Java 一致）
    ROCKETMQ_TOPIC_PHOTO_ANALYSIS: str = "photo-analysis"
    
    # Consumer Group（必须和 Java 不同）
    ROCKETMQ_CONSUMER_GROUP_PHOTO: str = "photo-analysis-consumer-group"
```

### 4.3 消费者服务

**文件**：`app/consumer/photo_consumer.py`

```python
from rocketmq.client import PushConsumer, ConsumeStatus
import json

class PhotoAnalysisConsumer:
    """照片分析消费者"""
    
    def __init__(self):
        self.consumer = None
        self.photo_service = PhotoAnalysisService()
    
    def start(self):
        """启动消费者"""
        # 第 1 步：创建消费者
        self.consumer = PushConsumer('photo-analysis-consumer-group')
        
        # 第 2 步：设置 NameServer 地址
        self.consumer.set_name_server_address('192.168.1.14:9876')
        
        # 第 3 步：订阅 Topic 和回调函数
        self.consumer.subscribe('photo-analysis', self._callback)
        
        # 第 4 步：启动消费者（开始监听消息）
        self.consumer.start()
        
        logger.info("RocketMQ 消费者启动成功，等待消息...")
    
    def _callback(self, msg):
        """
        消息回调函数
        当收到消息时，RocketMQ 会自动调用这个函数
        """
        try:
            # 第 1 步：获取消息内容（字节）
            body_bytes = msg.body
            # body_bytes = b'{"photoId":1001,"petId":2001,...}'
            
            # 第 2 步：转成字符串
            body_str = body_bytes.decode('utf-8')
            # body_str = '{"photoId":1001,"petId":2001,...}'
            
            # 第 3 步：解析 JSON
            message_data = json.loads(body_str)
            # message_data = {"photoId": 1001, "petId": 2001, ...}
            
            logger.info(f"收到照片分析消息: photoId={message_data.get('photoId')}")
            
            # 第 4 步：处理业务逻辑
            asyncio.create_task(self._process_photo_analysis(message_data))
            
            # 第 5 步：返回消费状态
            return ConsumeStatus.CONSUME_SUCCESS  # 告诉 RocketMQ：我处理成功了
            
        except Exception as e:
            logger.error(f"处理消息失败: {e}")
            return ConsumeStatus.RECONSUME_LATER  # 告诉 RocketMQ：我处理失败了，稍后重发
    
    async def _process_photo_analysis(self, message_data: dict):
        """处理照片分析"""
        photo_id = message_data.get('photoId')
        pet_id = message_data.get('petId')
        url = message_data.get('url')
        
        # 调用 AI 服务分析照片
        await self.photo_service.analyze_photo(
            photo_id=photo_id,
            pet_id=pet_id,
            url=url
        )
        
        logger.info(f"照片分析完成: photoId={photo_id}")
```

**代码解析**：

1. **PushConsumer 是什么？**
   - Push 模式：RocketMQ 主动推送消息给消费者
   - Pull 模式：消费者主动去拉取消息

2. **subscribe 方法**：
   ```python
   consumer.subscribe('topic名称', 回调函数)
   ```
   - 订阅一个 Topic
   - 指定收到消息后调用哪个函数

3. **回调函数执行流程**：
   ```
   RocketMQ 推送消息
       ↓
   调用 _callback(msg)
       ↓
   解析消息内容
       ↓
   处理业务逻辑
       ↓
   返回处理结果
   ```

4. **ConsumeStatus 的含义**：
   - `CONSUME_SUCCESS`：处理成功，RocketMQ 会删除这条消息
   - `RECONSUME_LATER`：处理失败，RocketMQ 会稍后重新发送

### 4.4 启动消费者

**文件**：`main.py`

```python
from app.consumer.photo_consumer import start_photo_consumer, stop_photo_consumer

@asynccontextmanager
async def lifespan(app: FastAPI):
    """应用生命周期管理"""
    
    # 启动时：启动消费者
    start_photo_consumer()
    logger.info("RocketMQ 消费者已启动")
    
    yield
    
    # 关闭时：停止消费者
    stop_photo_consumer()
    logger.info("RocketMQ 消费者已关闭")
```

**为什么要在这里启动？**
- FastAPI 启动时自动启动消费者
- FastAPI 关闭时自动停止消费者
- 保证消费者的生命周期和应用一致

---

## 第五部分：完整流程串讲

### 5.1 时序图

```
用户          Java服务        RocketMQ        Python服务       AI服务
 │              │               │               │               │
 ├─上传照片────>│               │               │               │
 │              ├─保存OSS       │               │               │
 │              ├─保存DB        │               │               │
 │              ├─发送消息────>│               │               │
 │<─返回成功────┤              │               │               │
 │              │               ├─推送消息────>│               │
 │              │               │               ├─解析消息      │
 │              │               │               ├─调用AI───────>│
 │              │               │               │<─返回结果─────┤
 │              │               │               ├─保存结果到DB  │
 │              │               │<─返回SUCCESS──┤               │
```

### 5.2 详细步骤

#### 步骤 1：用户上传照片
```
用户点击"上传照片"按钮
```

#### 步骤 2：Java 处理上传
```java
// 1. 上传到 MinIO
String url = minioService.upload(file);

// 2. 保存记录到数据库
Photo photo = Photo.builder()
    .userId(userId)
    .petId(petId)
    .url(url)
    .build();
photoRepository.save(photo);

// 3. 发送消息到 RocketMQ
PhotoAnalysisMessage message = PhotoAnalysisMessage.builder()
    .photoId(photo.getId())
    .url(url)
    .build();
mqProducerService.sendPhotoAnalysisMessage(message);

// 4. 立即返回
return Result.success("上传成功");
```

#### 步骤 3：RocketMQ 存储消息
```
消息存储在 Broker
等待 Python 消费者来取
```

#### 步骤 4：Python 消费消息
```python
# RocketMQ 自动调用回调函数
def _callback(self, msg):
    # 1. 解析消息
    data = json.loads(msg.body.decode('utf-8'))
    
    # 2. 创建异步任务
    asyncio.create_task(self._process_photo_analysis(data))
    
    # 3. 立即返回成功
    return ConsumeStatus.CONSUME_SUCCESS
```

#### 步骤 5：Python 处理照片分析
```python
async def _process_photo_analysis(self, data):
    # 1. 调用 AI 分析照片
    result = await ai_service.analyze_image(data['url'])
    
    # 2. 保存分析结果到数据库
    await photo_repository.update_analysis(
        photo_id=data['photoId'],
        tags=result['tags'],
        description=result['description']
    )
```

### 5.3 关键时间节点

```
T=0s    用户上传照片
T=0.5s  保存到 OSS
T=0.6s  保存到数据库
T=0.61s 发送 MQ 消息
T=0.62s 返回"上传成功"给用户  ← 用户只等了 0.62 秒
────────────────────────────────────
T=0.63s Python 收到消息
T=0.64s Python 开始分析
T=10s   AI 分析完成
T=10.1s 保存分析结果
```

**如果不用消息队列**：
```
T=0s    用户上传照片
T=0.5s  保存到 OSS
T=0.6s  保存到数据库
T=0.7s  调用 Python AI 分析
T=10s   AI 分析完成
T=10.1s 返回"上传成功"给用户  ← 用户要等 10.1 秒！
```

---

## 第六部分：常见问题

### Q1：Topic 名称必须一致吗？
**答**：是的！Java 发送到 `photo-analysis`，Python 也必须订阅 `photo-analysis`

### Q2：Consumer Group 必须不同吗？
**答**：生产者组和消费者组必须不同。如果有多个消费者实例，可以用同一个组。

### Q3：消息丢失怎么办？
**答**：
- RocketMQ 会把消息持久化到磁盘
- 消费失败会自动重试
- 可以设置消息保留时间

### Q4：消息重复怎么办？
**答**：
- 在业务代码中做幂等性处理
- 例如：根据 `photoId` 判断是否已经分析过

### Q5：消费顺序能保证吗？
**答**：
- 默认不保证顺序
- 如果需要顺序，要使用顺序消息

### Q6：Windows 开发怎么办？
**答**：
- 方案 1：使用 Docker 运行 Linux 容器
- 方案 2：使用 HTTP 回调替代（开发环境）
- 方案 3：直接在 Linux 服务器上开发

---

## 第七部分：实战练习

### 练习 1：修改 Topic 名称
```java
// Java
private static final String NEW_TOPIC = "my-topic";
```

```python
# Python
ROCKETMQ_TOPIC = "my-topic"
consumer.subscribe('my-topic', callback)
```

### 练习 2：添加新的消息类型
```java
// Java: 创建新的消息类
public class UserNotificationMessage {
    private Long userId;
    private String title;
    private String content;
}
```

```python
# Python: 创建新的消费者
class NotificationConsumer:
    def start(self):
        self.consumer.subscribe('user-notification', self._callback)
```

### 练习 3：查看消费进度
```bash
# 在 Linux 服务器上
./mqadmin consumerProgress -g photo-analysis-consumer-group
```

---

## 第八部分：总结

### 核心要点
1. **消息队列是异步通信的桥梁**
2. **Topic 是消息的分类**
3. **Producer 发送，Consumer 接收**
4. **回调函数是消费的核心**
5. **返回状态决定是否重试**

### 记忆口诀
```
名称要一致（Topic）
组要不一样（Group）
JSON 来传递（Message）
回调要处理（Callback）
成功要返回（Status）
```

### 下一步学习
1. RocketMQ 事务消息
2. RocketMQ 顺序消息
3. RocketMQ 延迟消息
4. 消息堆积和性能优化

---

希望这份指南能帮助你理解 RocketMQ！🚀
