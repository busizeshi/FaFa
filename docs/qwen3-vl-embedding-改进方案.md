# FaFa 宠物助手 - qwen3-vl-embedding 统一向量化方案

> **版本**: v2.0  
> **日期**: 2026-08-20  
> **目标**: 采用 qwen3-vl-embedding 统一处理图片、视频、文本的向量化，实现智能宠物识别和语义搜索

---

## 一、方案概述

### 1.1 核心变更

**从原方案迁移到新方案：**

| 维度 | 原方案 | 新方案 |
|------|--------|--------|
| **图片理解** | qwen-vl-plus 生成文本描述 | qwen3-vl-embedding 直接生成向量 |
| **文本向量化** | text-embedding-v2 | qwen3-vl-embedding (统一向量空间) |
| **视频支持** | ❌ 不支持 | ✅ 原生支持 (最大50MB) |
| **宠物识别** | ❌ 需依赖外部服务 | ✅ 图片到图片相似度搜索 |
| **语义搜索** | text-to-text | ✅ text-to-image/video 跨模态搜索 |
| **向量维度** | 1536 (text-embedding-v2) | 可配置 (Matryoshka, 建议1024) |
| **API调用成本** | ~870元/月 | ~163元/月 (降低81%) |

### 1.2 技术优势

1. **统一向量空间**: 文本、图片、视频映射到同一向量空间，天然支持跨模态检索
2. **简化架构**: 一个模型替代三个服务 (qwen-vl-plus + text-embedding-v2 + 图像搜索)
3. **原生视频支持**: 50MB视频限制完美匹配需求
4. **降低成本**: API调用次数减少，成本降低81%
5. **更好性能**: MMEB-V2 benchmark 排名第一

---

## 二、业务场景实现

### 2.1 场景一：宠物自动识别

**用户故事**: 用户创建宠物时上传三视图(正面、侧面、俯视)，之后每日上传照片/视频时，系统自动识别出是哪只宠物。

**技术实现**:

```
1. 创建宠物时:
   用户上传 3 张三视图 → qwen3-vl-embedding 生成向量 → 存入 Qdrant (标记为 pet_profile)
   
2. 日常上传照片/视频时:
   用户上传媒体 → qwen3-vl-embedding 生成向量 → Qdrant 相似度搜索 (过滤 pet_profile) 
   → 返回最相似的宠物 (confidence > 0.75) → 自动填充 petId 和 tags
   
3. 数据流:
   PhotoController.uploadPhoto (不再强制 petId)
   → Python AI Service: generate_embedding(image/video)
   → Qdrant: search(vector, filter={type: "pet_profile", user_id: xxx})
   → 返回识别结果: {pet_id, pet_name, confidence}
```

### 2.2 场景二：智能语义搜索

**用户故事**: 用户对AI说"帮我搜索去年12月,阿酷在阳台上趴着的图片和视频"

**技术实现**:

```
1. 解析用户查询:
   LLM 分析 → 提取结构化条件:
   {
     "pet_names": ["阿酷"],
     "time_range": "2025-12",
     "location": "阳台",
     "action": "趴着",
     "media_types": ["photo", "video"]
   }

2. 向量搜索:
   query_text = "阿酷在阳台上趴着" 
   → qwen3-vl-embedding(text) 
   → Qdrant 搜索 (filter: pet_id=xxx, time_range, media_type)
   → 返回匹配的照片/视频

3. 数据流:
   用户查询 → ChatService 解析 
   → qwen3-vl-embedding 生成 query 向量
   → Qdrant 混合搜索 (向量相似度 + 元数据过滤)
   → 返回结果列表
```

---

## 三、数据库设计变更

### 3.1 新增表: `user_tag` (用户标签表)

```sql
CREATE TABLE `user_tag` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '标签ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `tag_name` VARCHAR(50) NOT NULL COMMENT '标签名称',
  `category` VARCHAR(20) NULL COMMENT '分类: pet-宠物名, location-地点, action-动作, object-物品, mood-心情, other-其他',
  `usage_count` INT NOT NULL DEFAULT 0 COMMENT '使用次数',
  `last_used_at` DATETIME NULL COMMENT '最后使用时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_user_tag_user_id` (`user_id`),
  KEY `idx_user_tag_usage` (`user_id`, `usage_count` DESC),
  UNIQUE KEY `uk_user_tag_name` (`user_id`, `tag_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户标签表';
```

### 3.2 修改表: `pet` (宠物表)

**新增字段:**

```sql
ALTER TABLE `pet` ADD COLUMN `front_view_url` VARCHAR(500) NULL COMMENT '正面照URL' AFTER `avatar`;
ALTER TABLE `pet` ADD COLUMN `side_view_url` VARCHAR(500) NULL COMMENT '侧面照URL' AFTER `front_view_url`;
ALTER TABLE `pet` ADD COLUMN `top_view_url` VARCHAR(500) NULL COMMENT '俯视照URL' AFTER `side_view_url`;
ALTER TABLE `pet` ADD COLUMN `profile_embedding_ids` JSON NULL COMMENT '三视图向量ID数组 ["vec_1", "vec_2", "vec_3"]' AFTER `top_view_url`;
```

### 3.3 修改表: `photo` (照片/视频表)

**字段调整:**

```sql
-- 1. petId 改为可空
ALTER TABLE `photo` MODIFY COLUMN `pet_id` BIGINT NULL COMMENT '宠物ID(可为空,AI自动识别)';

-- 2. 新增媒体类型字段
ALTER TABLE `photo` ADD COLUMN `media_type` VARCHAR(20) NOT NULL DEFAULT 'image' COMMENT '媒体类型: image-图片, video-视频' AFTER `user_id`;

-- 3. 新增视频相关字段
ALTER TABLE `photo` ADD COLUMN `duration` INT NULL COMMENT '视频时长(秒)' AFTER `file_size`;
ALTER TABLE `photo` ADD COLUMN `video_cover_url` VARCHAR(500) NULL COMMENT '视频封面URL' AFTER `thumbnail_url`;

-- 4. 新增自动识别相关字段
ALTER TABLE `photo` ADD COLUMN `auto_recognized` TINYINT NOT NULL DEFAULT 0 COMMENT '是否自动识别宠物: 0-否, 1-是' AFTER `media_type`;
ALTER TABLE `photo` ADD COLUMN `recognition_confidence` DECIMAL(5, 4) NULL COMMENT '识别置信度(0-1)' AFTER `auto_recognized`;
ALTER TABLE `photo` ADD COLUMN `recognized_pet_ids` JSON NULL COMMENT '识别出的宠物ID数组' AFTER `recognition_confidence`;

-- 5. 修改表注释
ALTER TABLE `photo` COMMENT='照片/视频表';
```

---

## 四、后端代码修改清单

### 4.1 Java 后端 (fafa-java)

#### 4.1.1 Controller 层

**✅ [PetController.java]**
- 新增接口: `POST /pet/{id}/profile-photos` - 上传三视图
- 修改接口: `POST /pet` 和 `PUT /pet/{id}` - 支持上传三视图

**✅ [PhotoController.java]**
- 修改接口: `POST /api/photos/upload`
  - 新增参数: `List<String> tags` (用户标签)
  - 修改参数: `petId` 改为可选
  - 新增参数: `MultipartFile[] files` (支持批量上传)
  - 新增逻辑: 支持视频上传 (最大50MB)
- 新增接口: `POST /api/photos/batch-upload` - 批量上传

**🆕 [UserTagController.java]**
- `POST /api/user-tags` - 创建标签
- `GET /api/user-tags` - 查询标签列表
- `GET /api/user-tags/suggestions` - 标签建议(根据使用频率)
- `DELETE /api/user-tags/{id}` - 删除标签
- `DELETE /api/user-tags/batch` - 批量删除

#### 4.1.2 Application Service 层

**✅ [PetApplicationService.java]**
- 新增方法: `uploadProfilePhotos(petId, frontView, sideView, topView)` - 上传三视图
- 修改方法: `createPet()` - 支持同时上传三视图

**✅ [PhotoApplicationService.java]**
- 修改方法: `uploadPhoto()` 
  - petId 改为可选参数
  - 新增 tags 参数处理
  - 新增视频上传逻辑
  - 调用 Python AI 服务进行宠物识别
- 新增方法: `batchUploadPhotos()` - 批量上传

**🆕 [UserTagApplicationService.java]**
- CRUD 操作 + 批量操作
- 标签使用计数更新

#### 4.1.3 Domain 层

**✅ [Pet.java]**
- 新增字段: `frontViewUrl`, `sideViewUrl`, `topViewUrl`, `profileEmbeddingIds`
- 新增方法: `updateProfilePhotos()`

**✅ [Photo.java]**
- 修改字段: `petId` 改为可空
- 新增字段: `mediaType`, `duration`, `videoCoverUrl`, `autoRecognized`, `recognitionConfidence`, `recognizedPetIds`
- 新增方法: `markAsAutoRecognized(petId, confidence)`

**🆕 [UserTag.java]**
- 新建领域模型

#### 4.1.4 Infrastructure 层

**✅ [PhotoAnalysisMessage.java]**
- 新增字段: `mediaType`, `tags`, `isProfilePhoto`

**✅ [OssService.java]**
- 新增方法: `uploadVideo()` - 视频上传
- 新增方法: `generateVideoThumbnail()` - 生成视频缩略图

**🆕 [PythonAiClient.java]**
- 新增方法: `recognizePet(imageUrl/videoUrl)` - 宠物识别
- 修改方法: `searchPhotos()` - 支持视频搜索

---

### 4.2 Python AI 服务 (fafa-python)

#### 4.2.1 核心服务重写

**✅ [photo_service.py]** - **完全重写**

原逻辑:
```python
qwen-vl-plus 分析图片 → 生成文本描述 → text-embedding-v2 向量化 → 存入 Qdrant
```

新逻辑:
```python
qwen3-vl-embedding 直接向量化(图片/视频) → 存入 Qdrant
```

关键改动:
- ❌ 删除 `_call_qwen_vl()` 方法
- ❌ 删除 `_generate_embedding()` 方法  
- ❌ 删除文本描述生成逻辑
- ✅ 新增 `_generate_multimodal_embedding()` - 调用 qwen3-vl-embedding
- ✅ 新增 `_recognize_pet()` - 宠物识别逻辑
- ✅ 新增 `analyze_video()` - 视频分析

**✅ [vector_service.py]** - **完全重写**

新增功能:
- `search_by_image(image_url, filters)` - 图片相似度搜索
- `search_by_text(query_text, filters)` - 文本语义搜索
- `search_by_video(video_url, filters)` - 视频搜索
- `index_pet_profile(pet_id, image_urls)` - 索引宠物三视图
- `recognize_pet_from_media(user_id, media_url)` - 识别宠物

**🆕 [pet_service.py]** - **新建**

负责宠物相关AI处理:
- `process_profile_photos(pet_id, urls)` - 处理三视图
- `update_pet_profile_embeddings(pet_id)` - 更新向量库

**🆕 [user_tag_service.py]** - **新建**

标签智能管理:
- `extract_tags_from_media(media_url)` - 从媒体中提取标签
- `suggest_tags(user_id, context)` - 智能标签建议

#### 4.2.2 配置修改

**✅ [config.py]**

```python
# 修改模型配置
AI_VISION_MODEL: str = "qwen3-vl-embedding"  # 改为 qwen3-vl-embedding
AI_EMBEDDING_MODEL: str = "qwen3-vl-embedding"  # 统一使用同一模型

# 新增配置
QWEN3_VL_EMBEDDING_DIM: int = 1024  # 向量维度(Matryoshka)
VIDEO_MAX_SIZE_MB: int = 50  # 视频最大50MB
VIDEO_SUPPORTED_FORMATS: list = ["mp4", "mov", "avi"]

# Qdrant 配置调整
QDRANT_VECTOR_SIZE: int = 1024  # 改为 1024 维
QDRANT_COLLECTION_PHOTOS: str = "fafa_media"  # 改名更通用
QDRANT_COLLECTION_PET_PROFILES: str = "fafa_pet_profiles"  # 新增宠物档案集合
```

**✅ [dashscope_client.py]**

```python
# 新增方法
async def generate_multimodal_embedding(
    content: Union[str, dict],  # 文本 or {"image": url} or {"video": url}
    dimension: int = 1024
) -> List[float]:
    """
    调用 qwen3-vl-embedding 生成向量
    
    支持:
    - 文本: content = "一只猫在阳台"
    - 图片: content = {"image": "https://..."}
    - 视频: content = {"video": "https://..."}
    """
    from dashscope import MultiModalEmbedding
    
    response = MultiModalEmbedding.call(
        model='qwen3-vl-embedding',
        input=content,
        dimension=dimension  # Matryoshka 支持多种维度
    )
    
    if response.status_code == 200:
        return response.output.embeddings[0].embedding
    else:
        raise Exception(f"向量生成失败: {response.message}")
```

#### 4.2.3 API 接口

**✅ [photo.py]**
- 修改: `POST /api/v1/photo/analyze` - 支持视频分析
- 新增: `POST /api/v1/photo/recognize-pet` - 宠物识别

**✅ [vector.py]**  
- 修改: `POST /api/v1/vector/search` - 支持跨模态搜索

**🆕 [pet.py]** - **新建**
- `POST /api/v1/pet/profile` - 处理宠物三视图

#### 4.2.4 数据库 Repository

**✅ [photo_repository.py]**
- 新增查询: `find_by_tags()`, `find_by_media_type()`

**🆕 [user_tag_repository.py]** - **新建**
- 标签 CRUD 操作

---

## 五、Qdrant 向量库设计

### 5.1 Collection 设计

**Collection 1: `fafa_media`** (照片/视频)

```python
{
  "collection_name": "fafa_media",
  "vectors": {
    "size": 1024,  # qwen3-vl-embedding 维度
    "distance": "Cosine"
  },
  "payload_schema": {
    "media_id": "integer",       # 照片/视频ID
    "user_id": "integer",
    "pet_id": "integer|null",    # 可为空
    "media_type": "keyword",     # "image" | "video"
    "url": "text",
    "taken_at": "datetime",
    "tags": "keyword[]",         # 用户标签
    "ai_tags": "keyword[]",      # AI标签
    "auto_recognized": "boolean",
    "recognition_confidence": "float"
  }
}
```

**Collection 2: `fafa_pet_profiles`** (宠物三视图档案)

```python
{
  "collection_name": "fafa_pet_profiles",
  "vectors": {
    "size": 1024,
    "distance": "Cosine"
  },
  "payload_schema": {
    "pet_id": "integer",
    "user_id": "integer",
    "pet_name": "text",
    "view_type": "keyword",  # "front" | "side" | "top"
    "image_url": "text",
    "created_at": "datetime"
  }
}
```

### 5.2 搜索策略

**策略1: 宠物识别 (image-to-image)**

```python
# 1. 用户上传照片,生成向量
media_vector = generate_multimodal_embedding({"image": uploaded_url})

# 2. 在 pet_profiles collection 中搜索
results = qdrant.search(
    collection_name="fafa_pet_profiles",
    query_vector=media_vector,
    query_filter={
        "must": [{"key": "user_id", "match": {"value": user_id}}]
    },
    limit=1,
    score_threshold=0.75  # 相似度阈值
)

# 3. 返回识别结果
if results and results[0].score > 0.75:
    return {
        "pet_id": results[0].payload["pet_id"],
        "pet_name": results[0].payload["pet_name"],
        "confidence": results[0].score
    }
```

**策略2: 语义搜索 (text-to-image/video)**

```python
# 1. 用户查询文本向量化
query_vector = generate_multimodal_embedding("阿酷在阳台上趴着")

# 2. 在 media collection 中搜索
results = qdrant.search(
    collection_name="fafa_media",
    query_vector=query_vector,
    query_filter={
        "must": [
            {"key": "user_id", "match": {"value": user_id}},
            {"key": "pet_id", "match": {"value": pet_id}},
            {"key": "taken_at", "range": {"gte": "2025-12-01", "lte": "2025-12-31"}}
        ]
    },
    limit=20
)
```

---

## 六、API调用成本估算

### 6.1 成本对比

**假设**: 1000个活跃用户,每人每天上传5张照片

| 项目 | 原方案 | 新方案 |
|------|--------|--------|
| **图片理解** | qwen-vl-plus: 5000次/天 × 0.008元 = 40元/天 | ❌ 不需要 |
| **文本向量化** | text-embedding-v2: 5000次/天 × 0.0007元 = 3.5元/天 | ❌ 不需要 |
| **图片向量化** | ❌ 无 | qwen3-vl-embedding: 5000次/天 × 0.0035元 = 17.5元/天 |
| **宠物识别** | 阿里云图像搜索: ~12元/月 | ✅ 包含在向量化中 |
| **月度成本** | (40 + 3.5) × 30 + 12 = **1317元/月** | 17.5 × 30 = **525元/月** |
| **年度成本** | 15,804元 | 6,300元 |

**节省**: 60.2%

### 6.2 视频处理成本

- 视频向量化: 0.0035元/次 (与图片相同)
- 无额外成本增加

---

## 七、实施计划

### 7.1 开发阶段 (5天)

**Day 1**: 数据库设计 + 领域模型
- ✅ 新增 `user_tag` 表
- ✅ 修改 `pet` 表 (三视图字段)
- ✅ 修改 `photo` 表 (视频支持)
- ✅ 更新 Java Domain 模型

**Day 2-3**: Python AI 服务重构
- ✅ 重写 `photo_service.py` (qwen3-vl-embedding)
- ✅ 重写 `vector_service.py`
- ✅ 新建 `pet_service.py`
- ✅ 更新 Qdrant Collection
- ✅ 编写单元测试

**Day 4**: Java 后端接口
- ✅ PetController: 三视图上传
- ✅ PhotoController: tags参数 + 批量上传 + 视频支持
- ✅ UserTagController: 完整CRUD
- ✅ 调用 Python AI 服务

**Day 5**: 集成测试 + 文档
- ✅ 端到端测试
- ✅ 性能测试
- ✅ API 文档更新

### 7.2 数据迁移 (1天)

**迁移脚本**:
1. 为现有照片重新生成向量 (qwen3-vl-embedding)
2. 更新 Qdrant 数据
3. 验证数据一致性

### 7.3 上线部署 (0.5天)

- 灰度发布: 10% → 50% → 100%
- 监控 API 调用成功率
- 监控识别准确率

---

## 八、风险与应对

### 8.1 识别准确率风险

**风险**: 三视图可能无法100%识别所有照片

**应对**:
1. 设置置信度阈值 (0.75),低于阈值提示用户手动确认
2. 支持用户手动修正识别结果
3. 持续学习: 用户修正后更新宠物档案

### 8.2 视频处理性能

**风险**: 50MB视频上传和处理较慢

**应对**:
1. 异步处理: 上传后立即返回,后台分析
2. 进度通知: WebSocket 推送处理进度
3. 视频压缩: 客户端先压缩再上传

### 8.3 成本超预期

**风险**: 用户量激增导致API成本飙升

**应对**:
1. 设置每日调用上限
2. 缓存机制: 相同内容不重复向量化
3. 降级策略: 高峰期临时降低向量维度

---

## 九、监控指标

### 9.1 业务指标

- 宠物识别准确率: 目标 > 85%
- 用户修正率: < 20%
- 语义搜索召回率: > 80%

### 9.2 技术指标

- API 调用成功率: > 99%
- 向量化耗时: P95 < 2s
- 搜索响应时间: P95 < 500ms
- 日均 API 成本: < 20元

---

**方案状态**: ✅ 待开发  
**预计工期**: 6.5 天  
**成本节省**: 60.2%  
**性能提升**: 跨模态搜索 + 视频支持
