# FaFa MySQL 数据表记录

> 本文档记录各阶段数据表的建表语句和索引说明。

---

## Phase 0 用户系统

### user - 用户表

**建表语句**：
```sql
CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `open_id` VARCHAR(64) NOT NULL COMMENT '微信 openId',
  `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `gender` VARCHAR(10) DEFAULT NULL COMMENT '性别',
  `age` INT DEFAULT NULL COMMENT '年龄',
  `avatar_url` VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
  `city` VARCHAR(50) DEFAULT NULL COMMENT '城市',
  `is_vip` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否会员',
  `vip_expire_time` DATETIME DEFAULT NULL COMMENT '会员到期时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_open_id` (`open_id`),
  KEY `idx_phone` (`phone`),
  KEY `idx_vip` (`is_vip`, `vip_expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';
```

**索引**：
- `uk_open_id`：微信openId唯一索引，用于登录查找
- `idx_phone`：手机号索引，用于手机号查询
- `idx_vip`：会员状态和到期时间复合索引，用于会员管理

### user_quota_usage - 用户配额使用记录表

**建表语句**：
```sql
CREATE TABLE IF NOT EXISTS `user_quota_usage` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `quota_type` VARCHAR(50) NOT NULL COMMENT '配额类型: PET_COUNT, STORAGE_GB, AI_QUERY_DAILY',
  `used_amount` BIGINT NOT NULL DEFAULT 0 COMMENT '已使用量',
  `last_reset_time` DATETIME DEFAULT NULL COMMENT '上次重置时间(仅周期性配额)',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_quota` (`user_id`, `quota_type`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户配额使用记录表';
```

**索引**：
- `uk_user_quota`：用户ID和配额类型联合唯一索引，确保每个用户每种配额类型只有一条记录
- `idx_user_id`：用户ID索引，用于查询用户所有配额

---

## Phase 1 宠物档案

### pet - 宠物档案表

**建表语句**：
```sql
CREATE TABLE `pet` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '所属用户',
  `name` VARCHAR(32) NOT NULL COMMENT '宠物名称',
  `type` VARCHAR(16) NOT NULL COMMENT '宠物类型: cat/dog/other',
  `breed` VARCHAR(64) NULL COMMENT '品种',
  `gender` VARCHAR(16) NOT NULL COMMENT '性别: male/female/unknown',
  `birth_date` DATE NULL COMMENT '出生日期（外推）',
  `is_neutered` TINYINT NOT NULL DEFAULT 0 COMMENT '是否绝育',
  `adopt_date` DATE NULL COMMENT '到家日期',
  `weight` DECIMAL(5,2) NULL COMMENT '当前体重（kg）冗余',
  `avatar_url` VARCHAR(512) NULL COMMENT '头像URL',
  `front_photo_url` VARCHAR(512) NULL COMMENT '正面照URL',
  `side_photo_url` VARCHAR(512) NULL COMMENT '侧面照URL',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `active_name` VARCHAR(32) GENERATED ALWAYS AS (IF(`is_deleted` = 0, `name`, NULL)) STORED COMMENT '有效宠物名称唯一约束辅助列',
  PRIMARY KEY (`id`),
  INDEX `idx_user_id` (`user_id`, `is_deleted`),
  INDEX `idx_sort_order` (`user_id`, `sort_order`),
  UNIQUE KEY `uk_pet_user_active_name` (`user_id`, `active_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宠物档案表';
```

**索引**：
- `idx_user_id`：查询用户宠物列表，过滤逻辑删除
- `idx_sort_order`：手动排序
- `uk_pet_user_active_name`：同一用户有效宠物名称唯一；逻辑删除后名称可复用

---

## Phase 2 日常记录系统

> 设计说明：① `daily_status` 为一天一格 upsert 语义，删除即当天回到「未记录」，故物理删除、无逻辑删除列，唯一键不含 deleted；② 枚举维度用 VARCHAR + Java 领域枚举校验（与 pet.type 约定一致），不用 MySQL ENUM；③ `feeding_record` 的 `unit` 区分 g/ml，保证 AI 喂养分析数据正确性；④ 物种维度配置不建表，由领域层 SpeciesRecordPolicy 提供。

### daily_status - 每日状态记录表（状态观察）

**建表语句**：
```sql
CREATE TABLE IF NOT EXISTS `daily_status` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `pet_id` BIGINT NOT NULL COMMENT '宠物ID',
  `record_date` DATE NOT NULL COMMENT '记录日期（一天一格）',
  `appetite` VARCHAR(16) DEFAULT NULL COMMENT '食欲: excellent/normal/decreased/refused',
  `energy` VARCHAR(16) DEFAULT NULL COMMENT '精神活动: active/normal/lethargic',
  `water_intake` VARCHAR(16) DEFAULT NULL COMMENT '饮水程度: normal/more/less/not_noticed',
  `defecation` VARCHAR(16) DEFAULT NULL COMMENT '排便: normal/soft/diarrhea/constipation/not_seen',
  `vomiting` VARCHAR(16) DEFAULT 'none' COMMENT '呕吐: none/once/multiple',
  `urination` VARCHAR(16) DEFAULT NULL COMMENT '排尿: normal/abnormal/not_seen',
  `appetite_amount` DECIMAL(10,2) DEFAULT NULL COMMENT '进食克数（选填）',
  `water_amount` DECIMAL(10,2) DEFAULT NULL COMMENT '饮水毫升数（选填）',
  `notes` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `operator_id` BIGINT DEFAULT NULL COMMENT '记录人ID（家庭协同预留）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pet_date` (`pet_id`, `record_date`),
  KEY `idx_record_date` (`record_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='每日状态记录表（状态观察，一天一格）';
```

**索引**：
- `uk_pet_date`：宠物+日期唯一索引，保证一天一格 upsert 语义，同时覆盖按宠物查日期范围/最近一条
- `idx_record_date`：按日期跨宠物查询（后续报告统计用）

### feeding_record - 喂食记录表（照护动作）

**建表语句**：
```sql
CREATE TABLE IF NOT EXISTS `feeding_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `pet_id` BIGINT NOT NULL COMMENT '宠物ID',
  `feeding_time` DATETIME NOT NULL COMMENT '喂食时间',
  `food_type` VARCHAR(16) NOT NULL COMMENT '食物类型: main_food/snack/can/other',
  `amount` DECIMAL(10,2) DEFAULT NULL COMMENT '喂食量数值',
  `unit` VARCHAR(8) NOT NULL DEFAULT 'g' COMMENT '单位: g/ml',
  `food_id` BIGINT DEFAULT NULL COMMENT '关联食品管理卡ID（Phase 7 预留）',
  `notes` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `operator_id` BIGINT DEFAULT NULL COMMENT '记录人ID（家庭协同预留）',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_pet_time` (`pet_id`, `feeding_time`),
  KEY `idx_food` (`food_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='喂食记录表（照护动作，事件流）';
```

**索引**：
- `idx_pet_time`：按宠物查时间范围喂食记录（当天列表/区间统计）
- `idx_food`：按食品统计喂食量（Phase 7 食品管理用）

### weight_record - 体重记录表（周期测量）

**建表语句**：
```sql
CREATE TABLE IF NOT EXISTS `weight_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `pet_id` BIGINT NOT NULL COMMENT '宠物ID',
  `record_date` DATE NOT NULL COMMENT '记录日期',
  `weight` DECIMAL(5,2) NOT NULL COMMENT '体重（kg），与 pet.weight 保持一致',
  `notes` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `operator_id` BIGINT DEFAULT NULL COMMENT '记录人ID（家庭协同预留）',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_pet_date` (`pet_id`, `record_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='体重记录表（周期测量）';
```

**索引**：
- `idx_pet_date`：按宠物查体重趋势（同日可多条，无唯一约束）

---

## Phase 3 提醒系统

> 设计说明：① `reminder` 行 = 提醒定义 + 当前周期状态，**无持久 completed 态**——周期提醒完成当期后原地推进 `next_remind_time` 并保持 pending，一次性提醒完成即 finished，完成历史独立表 `reminder_completion_history` 承载（避免同一定义多行导致列表/编辑语义复杂化）；② 枚举列用 VARCHAR + Java 领域枚举校验（与 Phase 2 约定一致），不用 MySQL ENUM；③ 新增 `last_push_time` 列：定时扫描同一提醒每日至多推送一次（计划文档未含，属增强）；④ 完成历史冗余 `reminder_title`/`reminder_type` 快照：提醒被删除后历史与「今日已完成」列表仍可独立展示；⑤ `event_record` 为成长档案数据底座，Phase 3 仅落地提醒完成的写入侧，手动创建/时间轴读接口 Phase 4+ 扩展。

### reminder - 提醒表（周期任务定义+当前状态）

**建表语句**：
```sql
CREATE TABLE IF NOT EXISTS `reminder` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `pet_id` BIGINT NOT NULL COMMENT '宠物ID',
  `user_id` BIGINT NOT NULL COMMENT '所属用户ID',
  `type` VARCHAR(16) NOT NULL COMMENT '提醒类型: vaccine/deworming/bath/nail_trim/checkup/medication/birthday/daily_record/custom',
  `title` VARCHAR(200) NOT NULL COMMENT '提醒标题',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '提醒描述',
  `interval_type` VARCHAR(16) NOT NULL COMMENT '周期类型: once/days/weekly/monthly/months/yearly',
  `interval_value` INT DEFAULT NULL COMMENT '周期值: days/months=N(≥1); weekly=ISO星期(1周一~7周日); monthly=每月几号(1-31); once/yearly不使用',
  `next_remind_time` DATETIME NOT NULL COMMENT '下次提醒时间（当前周期基准，完成以其为基准推算下期）',
  `status` VARCHAR(16) NOT NULL DEFAULT 'pending' COMMENT '状态: pending生效中/paused已暂停/finished已结束(一次性提醒完成后)',
  `last_completion_time` DATETIME DEFAULT NULL COMMENT '最近一次完成时间',
  `last_completion_notes` VARCHAR(500) DEFAULT NULL COMMENT '最近一次完成备注',
  `last_push_time` DATETIME DEFAULT NULL COMMENT '最近一次推送时间（同一提醒每日至多推送一次）',
  `medication_id` BIGINT DEFAULT NULL COMMENT '关联用药ID（Phase 6 用药管理预留）',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_status` (`user_id`, `status`),
  KEY `idx_pet` (`pet_id`),
  KEY `idx_scan` (`status`, `next_remind_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='提醒表（周期任务定义+当前状态）';
```

**索引**：
- `idx_user_status`：首页今日提醒/提醒管理页按用户+状态查询
- `idx_pet`：按宠物查提醒列表
- `idx_scan`：定时扫描任务（status=pending AND next_remind_time 到期区间），is_deleted=0 由 MyBatis-Plus 逻辑删除自动过滤

### reminder_completion_history - 提醒完成历史表（追加只写）

**建表语句**：
```sql
CREATE TABLE IF NOT EXISTS `reminder_completion_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `reminder_id` BIGINT NOT NULL COMMENT '提醒ID',
  `pet_id` BIGINT NOT NULL COMMENT '宠物ID',
  `user_id` BIGINT NOT NULL COMMENT '完成人ID（家庭协同预留，现阶段=宠物主人）',
  `reminder_title` VARCHAR(200) NOT NULL COMMENT '提醒标题快照（提醒删除后历史仍可独立展示）',
  `reminder_type` VARCHAR(16) NOT NULL COMMENT '提醒类型快照',
  `completion_time` DATETIME NOT NULL COMMENT '完成时间（可晚于计划提醒时间）',
  `notes` VARCHAR(500) DEFAULT NULL COMMENT '完成备注',
  `photo_urls` JSON DEFAULT NULL COMMENT '完成照片URL列表（如疫苗本照片）',
  `event_record_id` BIGINT DEFAULT NULL COMMENT '关联生成的事件记录ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_reminder` (`reminder_id`, `completion_time`),
  KEY `idx_user_time` (`user_id`, `completion_time`),
  KEY `idx_pet_time` (`pet_id`, `completion_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='提醒完成历史表（追加只写）';
```

**索引**：
- `idx_reminder`：单个提醒的完成历史（时间倒序）
- `idx_user_time`：首页「今日已完成」按完成人+完成时间查询（家庭协同后按宠物空间聚合）
- `idx_pet_time`：按宠物聚合完成历史（宠物主页时间轴）

### event_record - 事件记录表（成长档案）

**建表语句**：
```sql
CREATE TABLE IF NOT EXISTS `event_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `pet_id` BIGINT NOT NULL COMMENT '宠物ID',
  `user_id` BIGINT NOT NULL COMMENT '所属用户ID',
  `source` VARCHAR(16) NOT NULL DEFAULT 'manual' COMMENT '来源: manual手动创建(Phase 4+)/reminder提醒完成生成',
  `source_id` BIGINT DEFAULT NULL COMMENT '来源对象ID（source=reminder 时为 reminder_id）',
  `event_type` VARCHAR(32) NOT NULL COMMENT '事件类型: vaccine/deworming/bath/nail_trim/checkup/custom',
  `title` VARCHAR(200) NOT NULL COMMENT '事件标题',
  `content` VARCHAR(2000) DEFAULT NULL COMMENT '事件详情',
  `event_date` DATE NOT NULL COMMENT '事件日期',
  `material_urls` JSON DEFAULT NULL COMMENT '素材URL列表（成长档案扩展）',
  `tags` JSON DEFAULT NULL COMMENT '标签列表（成长档案扩展，Phase 4 打通 user_tag）',
  `operator_id` BIGINT DEFAULT NULL COMMENT '记录人ID（家庭协同预留）',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_pet_date` (`pet_id`, `event_date`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='事件记录表（成长档案，Phase 3 提醒完成写入侧先行）';
```

**索引**：
- `idx_pet_date`：按宠物查时间轴事件（成长档案时间轴聚合核心查询）
- `idx_user`：按用户聚合事件（跨宠物统计）

---

## Phase 4 素材管理与成长档案

> 设计说明：① `user_tag` 为早期已建表（无代码引用、无数据），Phase 4 正式启用，结构含 `category` 分类与 `usage_count` 热度计数（支撑「选择已有标签」列表按使用频次排序），本阶段补齐使用逻辑；② `media_asset.tags` 与 `event_record.tags` 均为标签名快照（JSON 数组），标签定义删除不影响已打标数据；③ `media_asset.status` 承载 embedding 异步处理状态机（pending→processing→completed/failed；照片和视频均进入链路）；④ 宠物归属以 `media_asset.tags` 中的宠物名称为业务事实，支持多宠物同时命中；`pet_id` 与 `recognition_confirmed` 保留用于单宠物历史兼容，不产生新的归属表；⑤ 存储配额复用 Phase 0 的 `user_quota_usage` 表（新增 quota_type=`STORAGE_USED`，单位字节），AI 语义检索月配额走 Redis 计数（技术文档 6.2），不建新表。

### user_tag - 用户标签表（早期已建，Phase 4 启用）

**建表语句**：
```sql
CREATE TABLE IF NOT EXISTS `user_tag` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '标签ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `tag_name` VARCHAR(50) NOT NULL COMMENT '标签名称',
  `category` VARCHAR(20) DEFAULT NULL COMMENT '分类: pet-宠物名, location-地点, action-动作, object-物品, mood-心情, other-其他',
  `usage_count` INT NOT NULL DEFAULT 0 COMMENT '使用次数（打标时+1，取消打标不回退，表示历史使用热度）',
  `last_used_at` DATETIME DEFAULT NULL COMMENT '最后使用时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_tag_name` (`user_id`, `tag_name`),
  KEY `idx_user_tag_user_id` (`user_id`),
  KEY `idx_user_tag_usage` (`user_id`, `usage_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户标签表';
```

**索引**：
- `uk_user_tag_name`：用户级标签名唯一（同一用户不能建同名标签）
- `idx_user_tag_usage`：标签选择列表按使用热度排序
- 字典数据物理删除（无 is_deleted）：删除标签定义不影响素材/事件上的 tags 快照

### media_asset - 媒体素材表（照片/视频）

**建表语句**：
```sql
CREATE TABLE IF NOT EXISTS `media_asset` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '上传用户ID',
  `pet_id` BIGINT DEFAULT NULL COMMENT '单宠物兼容归属ID，多宠物归属以 tags 中的宠物名称为准',
  `media_type` VARCHAR(16) NOT NULL COMMENT '媒体类型: photo图片/video视频',
  `file_url` VARCHAR(512) NOT NULL COMMENT '对象存储URL',
  `file_name` VARCHAR(255) DEFAULT NULL COMMENT '原始文件名',
  `file_size` BIGINT NOT NULL COMMENT '文件大小（字节，存储配额计量）',
  `mime_type` VARCHAR(64) DEFAULT NULL COMMENT 'MIME类型',
  `tags` JSON DEFAULT NULL COMMENT '标签名数组（快照语义，标签定义删除不影响已有素材）',
  `status` VARCHAR(16) NOT NULL DEFAULT 'pending' COMMENT 'AI分析状态: pending待分析/processing分析中/completed已完成/failed失败',
  `ai_description` VARCHAR(1000) DEFAULT NULL COMMENT '历史 AI 描述字段，embedding 链路不生成',
  `ai_tags` JSON DEFAULT NULL COMMENT '历史 AI 标签字段，embedding 链路不生成',
  `recognition_score` DECIMAL(5,4) DEFAULT NULL COMMENT '宠物识别相似度（cosine 0~1；用户手动指定归属时为NULL）',
  `recognition_confirmed` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '单宠物历史兼容确认标志，新自动匹配不进入待确认流程',
  `taken_at` DATETIME NOT NULL COMMENT '拍摄/发生时间（用户指定，缺省为上传时刻）',
  `error_message` VARCHAR(500) DEFAULT NULL COMMENT '分析失败原因（status=failed时记录）',
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`user_id`, `taken_at`),
  KEY `idx_pet_time` (`pet_id`, `taken_at`),
  KEY `idx_user_confirm` (`user_id`, `recognition_confirmed`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='媒体素材表（照片/视频，Phase 4 素材管理）';
```

**索引**：
- `idx_user_time`：素材库按用户+时间分页检索（主路径）
- `idx_pet_time`：按宠物查素材（宠物主页/个体识别结果查看）
- `idx_user_confirm`：识别结果待确认列表（user_id + recognition_confirmed=0 AND pet_id IS NOT NULL）

---

## Phase 5 AI 对话 Agent

### ai_chat_session - AI 会话表

**建表语句**：
```sql
CREATE TABLE IF NOT EXISTS `ai_chat_session` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  `user_id` BIGINT NOT NULL COMMENT '所属用户ID',
  `pet_id` BIGINT DEFAULT NULL COMMENT '当前宠物上下文',
  `title` VARCHAR(80) DEFAULT NULL COMMENT '会话标题',
  `status` VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT 'active/deleted',
  `last_message_at` DATETIME DEFAULT NULL COMMENT '最近消息时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (`id`),
  KEY `idx_chat_session_user_time` (`user_id`, `status`, `last_message_at`),
  KEY `idx_chat_session_user_pet` (`user_id`, `pet_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI对话会话';
```

### ai_chat_message - AI 消息表

**建表语句**：
```sql
CREATE TABLE IF NOT EXISTS `ai_chat_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `session_id` BIGINT NOT NULL COMMENT '会话ID',
  `user_id` BIGINT NOT NULL COMMENT '所属用户ID',
  `pet_id` BIGINT DEFAULT NULL COMMENT '本条消息宠物上下文',
  `role` VARCHAR(16) NOT NULL COMMENT 'user/assistant/tool/system',
  `content` TEXT NOT NULL COMMENT '消息正文',
  `client_message_id` VARCHAR(64) DEFAULT NULL COMMENT '客户端幂等键',
  `blocks_json` JSON DEFAULT NULL COMMENT '结构化回答卡片',
  `citations_json` JSON DEFAULT NULL COMMENT '数据证据引用',
  `tool_calls_json` JSON DEFAULT NULL COMMENT '工具调用摘要',
  `record_draft_json` JSON DEFAULT NULL COMMENT '自然语言记录草稿',
  `model` VARCHAR(64) DEFAULT NULL COMMENT '模型标识',
  `prompt_tokens` INT DEFAULT NULL COMMENT '输入 token 数',
  `completion_tokens` INT DEFAULT NULL COMMENT '输出 token 数',
  `assistant_status` VARCHAR(16) NOT NULL DEFAULT 'answered' COMMENT 'answered/failed',
  `trace_id` VARCHAR(64) DEFAULT NULL COMMENT '链路追踪ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_chat_message_client` (`session_id`, `client_message_id`),
  KEY `idx_chat_message_session_time` (`session_id`, `id`),
  KEY `idx_chat_message_user_time` (`user_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI对话消息';
```

**设计说明**：Python Agent 不直接连接 MySQL；Java 按 `user_id + session_id + pet_id` 做权限校验后保存消息。`blocks_json`、`citations_json`、`record_draft_json` 只保存经过 Java 白名单过滤的结构化结果。

---

## V6 医疗健康表

V6 的可执行建表脚本位于：
`fafa-java/src/main/resources/db/migration/V6__create_medical_health_tables.sql`。
Java 通过 Flyway 自动执行，以下为表职责和索引核对清单。

### medical_visit

保存用户录入的就诊事实：宠物、就诊日期、医院/医生、主诉、诊断原文、处方原文、治疗记录、费用和备注。使用 `is_deleted` 逻辑删除。

关键索引：`idx_medical_visit_pet_date (pet_id, visit_date)`、`idx_medical_visit_user_date (user_id, visit_date)`。

### medical_visit_attachment

保存就医档案和已有 `media_asset` 的关联，不复制文件；`attachment_type` 支持 `receipt/report/prescription/other`。

关键约束：`uk_medical_visit_media (visit_id, media_id)`，并使用 `idx_medical_attachment_visit (visit_id, sort_order)`、`idx_medical_attachment_media (media_id)` 查询关联。

### medication_plan

保存药品名称、规格、用途、剂量、频次、执行时间 JSON、饭前/饭后、疗程、计划状态和来源就诊记录。`status` 支持 `active/paused/completed/deleted`。

关键索引：`idx_medication_plan_pet_status (pet_id, status)`、`idx_medication_plan_user_status (user_id, status)`。

### medication_record

保存每次计划执行事实，区分 `taken/skipped/missed/cancelled`，关联 `medication_plan` 和可选的 `reminder`。不物理删除。

关键约束：`uk_medication_record_slot (medication_plan_id, scheduled_at)`，保证提醒完成和手工补记幂等；另有 `idx_medication_record_pet_time (pet_id, scheduled_at)`。

### health_alert

保存纯规则健康预警的等级、标题、事实证据快照、观察窗口、指纹和生命周期状态。证据只用于展示和审计，规则评估仍读取业务事实表。

关键约束：`uk_health_alert_fingerprint (pet_id, rule_code, fingerprint)`，关键索引：`idx_health_alert_pet_status_time (pet_id, status, last_detected_at)`、`idx_health_alert_user_status (user_id, status)`。

### V6 数据边界

- Java 是以上五张表的唯一写入方；Python 只通过 `/internal/ai/**` 查询，不能直接连接 MySQL。
- 就医档案删除不删除 `media_asset`；用药计划删除保留实际用药历史。
- 规则预警不执行诊断，不替代兽医；`evidence_json` 必须包含来源类型、来源 ID、字段、值和日期。

---

## V7 食品与饮食表

V7 的可执行迁移脚本位于：
`fafa-java/src/main/resources/db/migration/V7__create_food_and_feeding_plan_tables.sql`。

### food_card

食品管理卡，保存用户确认后的食品事实，包括类型、规格、开封/保质期、营养标签、配料、来源媒体和来源识别任务。`status` 支持 `active/archived/deleted`，`is_deleted` 用于逻辑删除；`source_recognition_task_id` 唯一，避免同一识别任务并发确认生成重复食品卡。

关键索引：`idx_food_card_user_pet_status (user_id, pet_id, status, is_deleted)`、`idx_food_card_pet_updated (pet_id, updated_at)`、`idx_food_card_source_media (source_media_id)`。

### food_recognition_task

包装图片识别任务，记录幂等键、任务状态、模型/提示词版本、候选草稿、受限原始响应、失败信息和确认生成的食品卡。`(user_id, idempotency_key)` 唯一；AI 失败时 Java 释放 Redis 月度识别配额。

### food_transition_plan

换粮过渡计划，保存旧粮/新粮、起止日期、每日比例 JSON、每日目标量和最近一次反应。`status` 支持 `draft/active/completed/cancelled`，`idx_transition_pet_date` 用于宠物时间范围查询。

### feeding_plan / feeding_plan_item

喂食计划主表和计划项表。主表记录计划版本、来源、策略版本和生命周期；计划项记录稳定 `item_key`、食品、时段、数量、比例、周期和创建时营养快照。`(feeding_plan_id, item_key, is_deleted)` 唯一，避免计划项重复。

### feeding_record V7 扩展

在既有 `feeding_record` 增加 `feeding_plan_id`、`feeding_plan_item_id`、`planned_at`、`food_name_snapshot`。历史记录保持可读，食品卡逻辑删除不级联删除喂食事实；计划项完成通过 `feedingPlanId + feedingPlanItemId` 幂等。

### V7 数据边界

- Java 是食品卡、识别任务、换粮计划和喂食计划的唯一写入方；Python 只读取图片并返回结构化候选字段。
- 识别草稿不等于食品事实，必须经过用户确认后才能写入 `food_card`。
- 食品卡被 active 喂食计划或 active 换粮计划引用时，不允许归档或删除，返回 `FOOD_IN_ACTIVE_PLAN`。
- 迁移执行后请检查 `feeding_record` 原有数据、JSON 列、唯一索引和新增列默认值；生产环境先备份再执行 Flyway。

---

## V8 养宠记账表

V8 的可执行迁移脚本位于：
`fafa-java/src/main/resources/db/migration/V8__create_expense_record_table.sql`。

### expense_record

保存用户确认的宠物开支事实。`category` 支持 `food/medical/supplies/grooming/insurance/other`；金额使用 `DECIMAL(12,2)`，当前仅允许正数人民币；`source_type=manual` 表示用户录入，`source_type=medical_visit` 表示由就医档案自动同步。

医疗来源通过 `medical_visit_id` 唯一约束保证幂等：同一个就医档案最多对应一条自动开支。医疗费用清空或就医档案删除时只逻辑删除开支；后续重新填写费用可恢复同一行，避免唯一键冲突和重复账单。自动生成记录禁止从记账接口修改或删除。

食品开支可以记录 `food_card_id`，同时保存 `food_name_snapshot`，只做历史关联和展示；食品卡的 `price` 不会自动生成消费记录。`receipt_media_id` 只保存已有媒体素材 ID，不复制文件。

关键索引：`idx_expense_user_pet_date (user_id, pet_id, expense_date, is_deleted)`、`idx_expense_user_category_date (user_id, category, expense_date, is_deleted)`、`idx_expense_food_card (food_card_id, expense_date)`、`idx_expense_receipt_media (receipt_media_id)`；自动账单幂等约束为 `uk_expense_medical_visit (medical_visit_id)`。

### V8 数据边界

- Java 是 `expense_record` 的唯一写入方；Python Agent 只能通过 `/internal/ai/**` 读取开支事实和统计。
- 所有公开查询和关联资源都校验 `user_id + pet_id`；不建立物理外键，避免历史媒体/食品卡逻辑删除阻断账单事实。
- 统计由 Java `BigDecimal` 确定性计算，不使用 AI 推断、不进行货币换算；趋势补零，百分比两位小数并做残差修正。

---

## V9 周期报告与内容草稿表

V9 的可执行迁移脚本位于：
`fafa-java/src/main/resources/db/migration/V9__create_report_and_content_tables.sql`。

### pet_report

保存用户、宠物和周期唯一对应的报告。`period_type` 支持 `week/month/quarter/year`；`fact_snapshot_json` 是 Java 依据记录表组装的事实快照，`content_json` 是通过结构校验的 AI 或确定性表达层；报告重新生成更新同一行并递增 `generation_version`。

关键索引：`uk_pet_report_period` 保证同一用户、宠物、周期只有一份报告；`idx_pet_report_user_pet_time` 支持报告列表；`idx_pet_report_status_updated` 支持状态扫描。

### content_draft

保存分享笔记和小红书文案草稿。`selected_media_json` 保存用户选定的素材 ID 顺序；`fact_snapshot_json` 永不被用户编辑覆盖；用户编辑只更新 `draft_json`。`idempotency_key` 保证手动重试和周期任务不重复创建草稿。

### content_schedule

保存内容周期配置和下次运行时间，`interval_days` 仅支持 7、15、30。调度器到期后调用内容草稿生成服务并推进 `next_run_at`，不保存外部平台发布状态，也不直接发布内容。

### V9 数据边界

- Java 是三张表的唯一写入方，负责所有权、周期、素材归属、结构校验和逻辑删除。
- Python 只接收事实快照并返回结构化文案，不查询 Java 数据库，不写入报告事实，不提供发布能力。
- 报告与内容草稿共用 Redis 月配额；AI 失败时释放本次额度并保留确定性降级结果。
- 报告事实和内容表达分离，删除报告不会删除原始每日状态、医疗、费用、素材记录。

---

> 本文档最后更新：2026-08-23  
> 维护者：FaFa 开发组

---

## Phase 10 家庭协同与应急信息卡

V10 的可执行迁移脚本位于：
`fafa-java/src/main/resources/db/migration/V10__create_collaboration_and_emergency_tables.sql`。

### pet_member

保存宠物空间协作者。`pet.user_id` 仍然是 owner；`role` 支持 `member/caregiver`，`status` 支持 `active/revoked/expired`。`(pet_id,user_id)` 唯一，caregiver 使用 `expires_at` 自动失效，撤销和过期只改变关系状态，不删除历史审计。

### pet_invitation

保存一次性邀请元数据和 SHA-256 token 摘要，不保存 raw token。`max_uses=1`，`token_hash` 唯一；caregiver 邀请额外保存 `member_expires_at`，用于接受后生成临时成员关系。

### pet_emergency_profile

保存用户确认的应急资料，`pet_id` 唯一。接口使用 `version` 乐观锁，首次保存提交 0；空字段保持 NULL，不从 AI 或诊断文本推断。

### access_audit_log

追加记录权限拒绝、邀请、接受、撤销和业务操作。`operator_id` 表示实际操作人，历史记录允许为空；查询接口仅主人可用，并且只返回脱敏字段。

### reminder_notification_delivery

按 `(reminder_id, recipient_user_id, delivery_window)` 唯一幂等，记录 `pending/sent/failed`、失败摘要和 `retry_count`。共享提醒的 owner、active member、未过期 caregiver 分别记录投递状态，单个收件人失败不阻断其他收件人。

### V10 operator_id 扩展

V10 对 `medical_visit`、`medication_plan`、`media_asset`、`food_card`、`food_transition_plan`、`feeding_plan`、`expense_record`、`pet_report`、`content_draft` 增加可空 `operator_id`，不改写已有 `user_id` owner 语义；历史数据保持 NULL。
