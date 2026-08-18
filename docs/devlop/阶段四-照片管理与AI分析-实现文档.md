# 阶段四：照片管理与 AI 分析 - 实现文档

## 概述

阶段四实现了照片管理、AI 视觉分析和语义搜索功能，包括：
- 照片上传与存储（OSS）
- 基于 RocketMQ 的异步 AI 分析
- 通义千问视觉理解（qwen-vl-plus）
- 向量化存储（Qdrant）
- 语义搜索功能

---

## 一、数据库设计

### 1.1 photo 表

```sql
CREATE TABLE photo (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    pet_id BIGINT NOT NULL COMMENT '宠物ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    url VARCHAR(512) NOT NULL COMMENT '图片URL',
    thumbnail_url VARCHAR(512) COMMENT '缩略图URL',
    original_url VARCHAR(512) COMMENT '原图URL',
    taken_at DATETIME NOT NULL COMMENT '拍摄时间',
    upload_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    description TEXT COMMENT '用户描述',
    tags JSON COMMENT '用户标签',
    ai_tags JSON COMMENT 'AI识别标签',
    ai_description TEXT COMMENT 'AI生成描述',
    width INT COMMENT '图片宽度',
    height INT COMMENT '图片高度',
    file_size BIGINT COMMENT '文件大小（字节）',
    is_cover TINYINT(1) DEFAULT 0 COMMENT '是否为封面',
    embedding_id VARCHAR(128) COMMENT '向量ID（Qdrant）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_pet_id (pet_id),
    INDEX idx_user_id (user_id),
    INDEX idx_taken_at (taken_at),
    INDEX idx_is_cover (pet_id, is_cover)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宠物照片表';
```

**字段说明**：
- `url`：CDN 访问地址
- `thumbnail_url`：缩略图地址（待实现压缩）
- `original_url`：OSS 原图地址
- `ai_tags`：AI 识别的物品标签，JSON 数组
- `ai_description`：AI 生成的照片描述
- `embedding_id`：Qdrant 向量 ID，格式 `photo_{id}`

---

## 二、领域模型设计

### 2.1 照片聚合根（Photo）

**文件**: `domain/model/photo/Photo.java`

```java
@Data
@Builder
public class Photo {
    private PhotoId photoId;
    private Long petId;
    private Long userId;
    private String url;
    private String thumbnailUrl;
    private String originalUrl;
    private LocalDateTime takenAt;
    private LocalDateTime uploadAt;
    private String description;
    private List<String> tags;
    private List<String> aiTags;
    private String aiDescription;
    private Integer width;
    private Integer height;
    private Long fileSize;
    private Boolean isCover;
    private String embeddingId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 工厂方法
    public static Photo create(Long petId, Long userId, String url, 
                               String thumbnailUrl, LocalDateTime takenAt, 
                               String description);
    
    // 更新描述
    public void updateDescription(String description);
    
    // 设置为封面
    public void setAsCover();
    
    // 更新元数据
    public void updateMetadata(Integer width, Integer height, Long fileSize, String originalUrl);
    
    // 更新 AI 分析结果
    public void updateAiResult(List<String> aiTags, String aiDescription, String embeddingId);
}
```

### 2.2 照片 ID 值对象（PhotoId）

**文件**: `domain/model/photo/PhotoId.java`

```java
@Data
public class PhotoId implements Serializable {
    private Long value;
    
    public static PhotoId of(Long id) {
        PhotoId photoId = new PhotoId();
        photoId.setValue(id);
        return photoId;
    }
}
```

### 2.3 照片仓储接口（PhotoRepository）

**文件**: `domain/repository/PhotoRepository.java`

```java
public interface PhotoRepository {
    Photo save(Photo photo);
    Optional<Photo> findById(PhotoId photoId);
    List<Photo> findByPetId(Long petId, Integer pageNum, Integer pageSize);
    List<Photo> findByPetIdAndDateRange(Long petId, LocalDate startDate, LocalDate endDate);
    Optional<Photo> findCoverByPetId(Long petId);
    int countByPetId(Long petId);
    void deleteById(PhotoId photoId);
    void unsetAllCoversByPetId(Long petId);
}
```

---

## 三、Java 服务实现

### 3.1 应用服务层

**文件**: `application/service/PhotoApplicationService.java`

**核心方法**：

1. **上传照片** - `uploadPhoto`
   - 验证宠物归属
   - 读取图片元数据（宽高）
   - 上传到 OSS
   - 创建照片记录
   - 发送 MQ 消息触发 AI 分析

2. **查询照片列表** - `listPhotos`
   - 分页查询
   - 权限校验

3. **根据日期范围查询** - `listPhotosByDateRange`
   - 支持开始和结束日期筛选

4. **更新照片描述** - `updatePhotoDescription`
   - 用户自定义描述

5. **设置封面照片** - `setCoverPhoto`
   - 取消宠物所有封面
   - 设置新封面

6. **删除照片** - `deletePhoto`
   - 删除数据库记录
   - 可选删除 OSS 文件
   - 可选删除 Qdrant 向量

### 3.2 RocketMQ 消息生产者

**文件**: `infrastructure/mq/MqProducerService.java`

```java
@Service
public class MqProducerService {
    @Resource
    private RocketMQTemplate rocketMQTemplate;
    
    private static final String PHOTO_ANALYSIS_TOPIC = "photo-analysis";
    
    public void sendPhotoAnalysisMessage(PhotoAnalysisMessage message) {
        String jsonMessage = objectMapper.writeValueAsString(message);
        rocketMQTemplate.syncSend(PHOTO_ANALYSIS_TOPIC, 
            MessageBuilder.withPayload(jsonMessage).build());
    }
}
```

**消息结构** (`PhotoAnalysisMessage`):
- `photoId`: 照片 ID
- `petId`: 宠物 ID
- `userId`: 用户 ID
- `url`: 照片 URL
- `thumbnailUrl`: 缩略图 URL
- `takenAt`: 拍摄时间

### 3.3 接口层（Controller）

**文件**: `interfaces/controller/PhotoController.java`

**API 列表**：

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/photos/upload` | 上传照片 |
| GET | `/api/photos` | 查询照片列表 |
| GET | `/api/photos/date-range` | 按日期范围查询 |
| GET | `/api/photos/{id}` | 查询照片详情 |
| PUT | `/api/photos/{id}/description` | 更新描述 |
| PUT | `/api/photos/{id}/cover` | 设置为封面 |
| DELETE | `/api/photos/{id}` | 删除照片 |
| GET | `/api/photos/count` | 统计照片数量 |
| POST | `/api/photos/search` | 语义搜索照片 |

**上传接口示例**：
```
POST /api/photos/upload
Content-Type: multipart/form-data

file: (binary)
petId: 1
takenAt: 2026-08-18 10:00:00
description: 在阳台晒太阳
```

**搜索接口示例**：
```
POST /api/photos/search
Content-Type: application/json

{
  "petId": 1,
  "query": "正在睡觉的照片",
  "limit": 20
}
```

### 3.4 Python AI 客户端

**文件**: `infrastructure/client/PythonAiClient.java`

```java
@Service
public class PythonAiClient {
    @Resource
    private RestTemplate restTemplate;
    
    @Value("${fafa.ai-service.base-url}")
    private String aiServiceBaseUrl;
    
    public List<PhotoSearchResult> searchPhotos(SearchPhotoRequest request) {
        String url = aiServiceBaseUrl + "/ai/photos/search";
        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
        // 解析结果
    }
}
```

---

## 四、Python 服务实现

### 4.1 RocketMQ 消费者

**文件**: `app/consumer/photo_consumer.py`

```python
class PhotoAnalysisConsumer:
    def __init__(self):
        self.consumer = PushConsumer('photo-analysis-consumer-group')
        self.photo_service = PhotoAnalysisService()
        
    def start(self):
        self.consumer.set_name_server_address(settings.ROCKETMQ_NAME_SERVER)
        self.consumer.subscribe('photo-analysis', self._callback)
        self.consumer.start()
    
    def _callback(self, msg):
        message_data = json.loads(msg.body.decode('utf-8'))
        asyncio.create_task(self._process_photo_analysis(message_data))
        return ConsumeStatus.CONSUME_SUCCESS
```

### 4.2 照片分析服务

**文件**: `app/service/photo_service.py`

**核心流程**：

1. **调用 qwen-vl-plus 分析照片** - `_call_qwen_vl`
   ```python
   messages = [{
       'role': 'user',
       'content': [
           {'image': image_url},
           {'text': '请详细描述这张宠物照片...'}
       ]
   }]
   response = MultiModalConversation.call(model='qwen-vl-plus', messages=messages)
   ```

2. **提取结构化信息**：
   - `_extract_scene`: 场景（室内/室外）
   - `_extract_behavior`: 行为（睡觉/玩耍/吃饭等）
   - `_extract_objects`: 物品标签

3. **生成 Embedding** - `_generate_embedding`
   ```python
   response = TextEmbedding.call(
       model=TextEmbedding.Models.text_embedding_v2,
       input=text
   )
   embedding = response.output['embeddings'][0]['embedding']
   ```

4. **保存到 Qdrant** - `_save_to_qdrant`
   ```python
   await qdrant_client.upsert(
       collection_name='pet_photos',
       points=[PointStruct(
           id=embedding_id,
           vector=embedding,
           payload={...}
       )]
   )
   ```

5. **更新 MySQL** - `_update_photo_ai_result`
   - 更新 `ai_tags`、`ai_description`、`embedding_id`

### 4.3 照片搜索 API

**文件**: `app/api/photo.py`

```python
@router.post("/search", response_model=PhotoSearchResponse)
async def search_photos(request: PhotoSearchRequest):
    # 1. 生成查询 Embedding
    query_embedding = await generate_query_embedding(request.query)
    
    # 2. 向量检索
    search_results = await qdrant_client.search(
        collection_name='pet_photos',
        query_vector=query_embedding,
        query_filter=Filter(must=[
            FieldCondition(key='pet_id', match=MatchValue(value=request.pet_id))
        ]),
        limit=request.limit,
        score_threshold=0.6
    )
    
    # 3. 返回结果
    return PhotoSearchResponse(total=len(results), results=results)
```

**响应结构**：
```json
{
  "total": 3,
  "results": [
    {
      "photo_id": 123,
      "pet_id": 1,
      "url": "https://...",
      "description": "在阳台晒太阳",
      "ai_description": "一只橘猫躺在阳台的地板上...",
      "score": 0.87
    }
  ],
  "insight": "找到 3 张相关照片"
}
```

### 4.4 照片仓储

**文件**: `app/repository/photo_repository.py`

```python
class PhotoRepository:
    async def get_photo_by_id(self, photo_id: int) -> Optional[dict]:
        # 查询照片详情
    
    async def list_photos_by_pet(self, pet_id: int, limit: int = 20) -> List[dict]:
        # 查询宠物照片列表
```

---

## 五、技术要点

### 5.1 图片上传流程

```
用户上传 → Java Controller → 读取元数据 → 上传 OSS → 创建记录 → 发送 MQ
                                                              ↓
Python Consumer ← RocketMQ ← photo-analysis Topic
       ↓
调用 qwen-vl-plus 分析
       ↓
生成 Embedding（text-embedding-v2）
       ↓
存入 Qdrant（pet_photos collection）
       ↓
更新 MySQL（ai_tags、ai_description、embedding_id）
```

### 5.2 语义搜索流程

```
用户搜索 → Java Controller → 转发到 Python API
                                    ↓
                          生成查询 Embedding
                                    ↓
                          Qdrant 向量检索（按 pet_id 过滤）
                                    ↓
                          返回 Top-K 结果（score > 0.6）
                                    ↓
                          Java Controller → 返回给用户
```

### 5.3 Qdrant 向量存储

**Collection 结构**：
- Collection 名称：`pet_photos`
- 向量维度：1536（text-embedding-v2）
- Payload 字段：
  - `photo_id`: 照片 ID
  - `pet_id`: 宠物 ID
  - `url`: 图片 URL
  - `description`: AI 描述
  - `scene`: 场景
  - `behavior`: 行为
  - `objects`: 物品标签

### 5.4 通义千问视觉理解

**模型**: `qwen-vl-plus`

**输入格式**:
```python
{
    'role': 'user',
    'content': [
        {'image': 'https://example.com/photo.jpg'},
        {'text': '请详细描述这张宠物照片...'}
    ]
}
```

**输出**: 自然语言描述

### 5.5 异步处理

- **上传流程**: 同步处理（用户等待）
- **AI 分析**: 异步处理（MQ 解耦）
- **搜索流程**: 同步处理（实时返回）

**优势**:
- 用户上传后立即返回，无需等待 AI 分析
- AI 分析失败不影响照片上传成功
- 削峰填谷，控制 AI API 调用频率

---

## 六、配置说明

### 6.1 Java 配置

**application.yml**:
```yaml
# RocketMQ 配置
rocketmq:
  name-server: ${ROCKETMQ_HOST:localhost}:${ROCKETMQ_PORT:9876}
  producer:
    group: fafa-producer-group
    send-message-timeout: 3000

# Python AI 服务
fafa:
  ai-service:
    base-url: ${AI_SERVICE_URL:http://localhost:8000}
    timeout: 30000
```

### 6.2 Python 配置

**.env**:
```bash
# 通义千问 API Key
DASHSCOPE_API_KEY=sk-xxx

# RocketMQ
ROCKETMQ_NAME_SERVER=localhost:9876

# Qdrant
QDRANT_HOST=localhost
QDRANT_PORT=6333

# MySQL
MYSQL_HOST=localhost
MYSQL_PORT=3306
MYSQL_DATABASE=fafa
MYSQL_USERNAME=root
MYSQL_PASSWORD=password
```

---

## 七、完整文件清单

### 7.1 Java 服务（29 个文件）

**领域层**:
- `domain/model/photo/Photo.java` - 照片聚合根
- `domain/model/photo/PhotoId.java` - 照片 ID 值对象
- `domain/repository/PhotoRepository.java` - 照片仓储接口

**应用层**:
- `application/service/PhotoApplicationService.java` - 照片应用服务
- `application/dto/photo/UploadPhotoRequest.java` - 上传请求
- `application/dto/photo/UpdatePhotoDescriptionRequest.java` - 更新描述请求
- `application/dto/photo/SearchPhotoRequest.java` - 搜索请求
- `application/dto/photo/PhotoResponse.java` - 照片响应
- `application/dto/photo/PhotoSearchResult.java` - 搜索结果

**基础设施层**:
- `infrastructure/persistence/dataobject/PhotoDO.java` - 照片 DO
- `infrastructure/persistence/mapper/PhotoMapper.java` - MyBatis Mapper
- `infrastructure/persistence/mapper/PhotoMapper.xml` - SQL 映射
- `infrastructure/persistence/repository/PhotoRepositoryImpl.java` - 仓储实现
- `infrastructure/persistence/converter/PhotoConverter.java` - 转换器
- `infrastructure/mq/MqProducerService.java` - MQ 生产者
- `infrastructure/mq/PhotoAnalysisMessage.java` - 照片分析消息
- `infrastructure/client/PythonAiClient.java` - Python AI 客户端

**接口层**:
- `interfaces/controller/PhotoController.java` - 照片控制器

### 7.2 Python 服务（4 个文件）

- `app/service/photo_service.py` - 照片分析服务
- `app/consumer/photo_consumer.py` - RocketMQ 消费者
- `app/api/photo.py` - 照片搜索 API
- `app/repository/photo_repository.py` - 照片仓储

---

## 八、API 接口汇总

### 8.1 Java 服务接口（9 个）

| 方法 | 路径 | 说明 | 请求参数 |
|------|------|------|----------|
| POST | `/api/photos/upload` | 上传照片 | file, petId, takenAt, description |
| GET | `/api/photos` | 照片列表 | petId, page, size |
| GET | `/api/photos/date-range` | 按日期范围查询 | petId, startDate, endDate |
| GET | `/api/photos/{id}` | 照片详情 | id |
| PUT | `/api/photos/{id}/description` | 更新描述 | description |
| PUT | `/api/photos/{id}/cover` | 设置封面 | - |
| DELETE | `/api/photos/{id}` | 删除照片 | - |
| GET | `/api/photos/count` | 统计数量 | petId |
| POST | `/api/photos/search` | 语义搜索 | petId, query, limit |

### 8.2 Python 服务接口（2 个）

| 方法 | 路径 | 说明 | 请求参数 |
|------|------|------|----------|
| POST | `/ai/photos/search` | 语义搜索 | pet_id, query, limit |
| GET | `/ai/photos/list` | 照片列表 | pet_id, limit |

---

## 九、测试建议

### 9.1 功能测试

1. **照片上传测试**
   - 上传不同格式图片（JPG、PNG、WEBP）
   - 上传不同尺寸图片
   - 测试大文件上传（接近 10MB）
   - 测试元数据读取

2. **AI 分析测试**
   - 上传后检查 MQ 消息发送
   - 观察 Python 消费者日志
   - 验证 qwen-vl-plus 分析结果
   - 检查 Qdrant 向量存储
   - 验证 MySQL 更新

3. **语义搜索测试**
   - 测试不同查询语句
   - 测试相似度阈值
   - 测试 pet_id 过滤
   - 验证搜索结果准确性

4. **权限测试**
   - 测试跨用户访问
   - 测试 token 验证

### 9.2 性能测试

- 并发上传测试（10+ 用户同时上传）
- MQ 消费速率测试
- 向量检索性能测试（1000+ 照片）

### 9.3 异常测试

- OSS 上传失败处理
- qwen-vl-plus API 调用失败
- Qdrant 连接失败
- MQ 消费失败重试

---

## 十、后续优化建议

### 10.1 功能优化

1. **缩略图生成**
   - 自动生成多种尺寸缩略图
   - 减少流量和加载时间

2. **AI 分析优化**
   - 使用更强大的视觉模型
   - 增加情感识别（开心/生气/害怕）
   - 识别宠物品种和颜色

3. **搜索增强**
   - 混合检索（向量 + 关键词）
   - 多模态检索（图片搜图片）
   - 时间范围过滤

4. **照片聚合**
   - 按时间自动生成相册
   - 按事件分组（生日、体检等）

### 10.2 性能优化

1. **CDN 加速**
   - 照片 URL 使用 CDN
   - 缩略图优先加载

2. **缓存策略**
   - Redis 缓存热门照片元数据
   - 搜索结果缓存

3. **批量处理**
   - 批量上传接口
   - 批量分析优化

### 10.3 运维优化

1. **监控告警**
   - MQ 消费延迟监控
   - AI API 调用失败率
   - 向量库查询耗时

2. **成本控制**
   - OSS 存储成本统计
   - AI API 调用量统计
   - 设置调用上限

---

## 十一、技术栈总结

| 组件 | 技术 | 说明 |
|------|------|------|
| 后端框架 | Spring Boot 3.2.5 | Java 21 |
| ORM | MyBatis-Plus 3.5.6 | 持久化 |
| 认证 | Sa-Token 1.37.0 | 权限控制 |
| 消息队列 | RocketMQ 2.3.0 | 异步解耦 |
| 对象存储 | MinIO | 文件存储 |
| Python 框架 | FastAPI 0.110.0 | AI 服务 |
| AI 模型 | 通义千问 qwen-vl-plus | 视觉理解 |
| Embedding | text-embedding-v2 | 文本向量化 |
| 向量库 | Qdrant 1.8.0 | 向量检索 |
| 数据库 | MySQL 8.0 | 关系数据 |

---

## 十二、总结

阶段四成功实现了照片管理的完整功能链路：

1. ✅ **照片上传与存储**：支持文件上传、元数据读取、OSS 存储
2. ✅ **异步 AI 分析**：基于 RocketMQ 异步调用 qwen-vl-plus 进行视觉理解
3. ✅ **向量化存储**：使用 text-embedding-v2 生成向量，存入 Qdrant
4. ✅ **语义搜索**：支持自然语言搜索照片，相似度打分
5. ✅ **完整 CRUD**：照片列表、详情、更新、删除、设置封面

**技术亮点**：
- MQ 解耦上传和分析，提升用户体验
- 多模态 AI（视觉 + 文本）深度理解照片内容
- 向量检索实现语义搜索，突破关键词限制
- DDD 架构保持代码清晰，易于扩展

下一阶段可以继续开发健康管理、智能提醒等功能模块。
