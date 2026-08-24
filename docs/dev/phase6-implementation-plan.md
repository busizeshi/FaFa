# Phase 6 实施文档：健康管理、用药闭环与规则预警

> 依据：《FaFa产品功能设计-最终版.md》第五章（健康管理）、第四章用药记录约定、第八章提醒系统、第十一章 AI 数据查询工具链；《分阶段开发计划.md》Phase 6 概要；《技术选型与项目框架搭建.md》第 2.2、4、5、7 章；《Codex开发交接文档.md》阶段进度与数据库约定。
>
> 状态：**核心代码已实施，进入联调与验收**。
>
> 前置假设：Phase 0～5 的核心代码已经完成，Phase 2 的 `daily_status`、`feeding_record`、`weight_record`，Phase 3 的 `reminder`、`reminder_completion_history`、`event_record`，以及 Phase 4 的 `media_asset` 已可作为本阶段数据来源；Phase 5 的 Java 鉴权、AI 会话和 Python Agent 链路继续复用。
>
> 本阶段目标：将健康管理从“记录分散、只能回忆”升级为“就医档案可追溯、用药计划可执行、异常变化可解释、就医前可带走摘要”的闭环。

---

## 1. 阶段目标

Phase 6 不做诊断系统，也不让 AI 替代兽医。它只负责把用户已经记录的事实组织起来，并基于明确、可审计的规则提示“数据出现了值得关注的变化”。

### 1.1 用户完成本阶段后得到的能力

| 能力 | 用户结果 | 事实来源 |
|---|---|---|
| 就医档案 | 记录医院、医生、日期、主诉、诊断、处方、费用、附件，并按时间查看 | `medical_visit`、`media_asset` |
| 医疗时间线 | 就诊、疫苗、驱虫、体检、用药记录统一按时间展示 | 就医档案 + 提醒完成历史 + 用药记录 |
| 用药计划 | 记录药品、规格、剂量、疗程、频次、饭前/饭后和每日执行时间 | `medication_plan` |
| 用药闭环 | 由计划生成提醒，点击完成后产生实际用药记录，可查询漏记/跳过 | `reminder`、`medication_record` |
| 健康预警 | 识别拒食、呕吐、饮水、腹泻等组合变化，展示红/黄级提醒 | `daily_status`、`weight_record`、用药数据 |
| 就诊摘要 | 选择近 7 天、近 30 天或自定义范围，生成结构化摘要和可复制文本 | 既有记录 + 就医档案 + 用药记录 |
| AI 健康问答扩展 | AI 可以查询就医和用药事实，但仍只描述记录，不输出诊断或处方 | Phase 5 Agent 工具链 |

### 1.2 阶段完成定义

以下链路在真实环境中可跑通，才认为 Phase 6 完成：

```text
小程序健康管理页
  → Java 登录态、宠物归属和参数校验
  → MySQL 保存就医档案 / 用药计划 / 实际用药记录
  → 用药计划创建或更新现有 medication 提醒
  → 定时任务执行纯规则预警并持久化去重结果
  → 首页读取未处理预警并支持确认
  → 就医摘要请求由 Java 组装权限范围
  → Python 基于只读事实生成结构化摘要
  → Java 返回结构化摘要和可复制文本
```

### 1.3 非目标

以下内容明确不属于 Phase 6 的核心交付：

- 不做疾病诊断、治疗方案、处方推荐、剂量调整建议或风险概率判断。
- 不实现病历/检查报告 OCR 结构化落库。产品设计中的该能力属于 P2 增强，本阶段只支持把已有 `media_asset` 作为就诊附件关联。
- 不实现食品管理、食品包装识别、换粮过渡和喂食计划，归入 Phase 7。
- 不实现支出分类、记账统计和医疗费用自动记账，归入 Phase 8；Phase 6 只保存就诊费用事实。
- 不实现周报、月报、季度报告、年度报告和分享笔记，归入 Phase 9。
- 不实现家庭协同的成员权限、临时授权和 operator 权限扩展，继续沿用当前用户拥有宠物的权限模型。
- 不引入第二套提醒系统。用药提醒必须复用 Phase 3 的 `reminder` 聚合和完成闭环。
- 不建立“健康画像”或长期记忆表，不把规则推断结果写入宠物档案。

---

## 2. 基线、依赖与关键决策

### 2.1 已有能力基线

| 基线 | 当前能力 | Phase 6 用法 |
|---|---|---|
| Phase 0 | Sa-Token 登录、用户与会员信息 | 所有公开健康接口复用登录态 |
| Phase 1 | 宠物 CRUD 和归属校验 | 所有健康数据均绑定 `pet_id`，写入前校验当前用户拥有该宠物 |
| Phase 2 | 每日状态、喂食、体重记录 | 规则预警和就诊摘要的主要事实源 |
| Phase 3 | 周期提醒、完成历史、事件生成 | 用药计划生成 `type=medication` 提醒；疫苗/驱虫/体检纳入医疗时间线 |
| Phase 4 | `media_asset`、MinIO、媒体详情接口 | 就诊票据、处方、报告照片先作为已有图片素材，再建立关联表 |
| Phase 5 | Java/Python Agent、内部只读查询、会话消息 | 新增医疗/用药只读工具和健康边界提示 |
| 基础设施 | MySQL、Flyway、Redis、Spring Scheduler、FastAPI、Qwen | V6 迁移、预警去重、定时评估、摘要生成 |

### 2.2 关键设计决策

1. **Java 是医疗事实唯一写入方**：就医档案、药品计划、用药记录、预警状态全部由 Java 鉴权并落 MySQL。
2. **预警规则是纯函数优先**：规则只接受已经加载的 `HealthRuleContext`，不在规则内部访问数据库、不调用模型，便于单元测试和审计。
3. **时间线不额外建聚合表**：就医、提醒完成、用药记录和事件记录按查询时合并，避免派生数据长期不一致。
4. **用药计划与提醒一对多**：一个计划可以包含多个每日执行时间，每个执行时间生成一条 `type=medication` 提醒；提醒完成时通过 `medication_id` 回写同一计划的用药记录。
5. **用药记录幂等**：同一计划、同一计划执行时间只能有一条实际记录；重复点击完成不重复产生用药记录。
6. **预警持久化但不重复轰炸**：同一宠物、同一规则、同一事实窗口生成稳定指纹，窗口内只更新 `last_detected_at`，不重复创建红色预警。
7. **摘要先结构化后自然语言**：Java 先限定数据范围和数据字段，Python 只对结构化事实做表达；摘要失败时返回可复制的确定性数据摘要，不伪造模型结果。
8. **服务端不生成 PNG/PDF**：Phase 6 返回结构化摘要和纯文本，小程序用现有页面渲染并支持复制/保存长图；避免引入服务端字体、渲染和文件存储链路。

### 2.3 权限与数据边界

每一条公开接口都必须满足：

```text
currentUserId = Sa-Token 登录用户
petId 属于 currentUserId
资源 userId / petId 与当前上下文一致
```

资源不存在和资源不属于当前用户统一返回“不存在”，避免通过错误码探测其他用户的医疗数据。Python 内部接口只接受 `X-Internal-Token`，且仍然要求 `user_id + pet_id`，不能把内部令牌当作业务授权。

---

## 3. 业务流程与状态机

### 3.1 就医档案流程

```text
创建就诊记录
  → 校验宠物归属、日期、费用和字段长度
  → 保存 medical_visit
  → 可选关联已有 media_asset
  → 出现在医疗时间线
```

就诊记录允许编辑和逻辑删除；删除就诊记录不删除关联媒体素材，不删除由提醒或用药产生的历史记录。

### 3.2 用药计划流程

```text
创建用药计划
  → 校验疗程、剂量、频次和执行时间
  → 保存 medication_plan
  → 为每个执行时间创建 type=medication 的 reminder
  → 用户点击提醒完成
  → reminder 完成事务内写入 medication_record
  → 周期提醒推进下一次时间
```

计划状态：

```text
ACTIVE  ──暂停──> PAUSED
PAUSED  ──恢复──> ACTIVE
ACTIVE  ──疗程结束/用户结束──> COMPLETED
ACTIVE  ──删除──> DELETED（逻辑删除）
```

计划暂停时，关联的 medication 提醒一并暂停；恢复时只恢复原计划生成的提醒。用户不能通过普通提醒接口伪造一个没有 `medication_plan` 的 medication 提醒。

### 3.3 健康预警流程

```text
定时扫描 / 用户手动评估
  → 读取限定时间窗口的结构化记录
  → 规则逐条评估
  → 生成稳定 fingerprint
  → 新预警插入 / 已存在预警更新
  → 首页读取 open / acknowledged
  → 用户确认后变为 acknowledged
```

预警状态：

| 状态 | 含义 | 是否继续出现在首页 |
|---|---|---|
| `open` | 新发现、用户尚未确认 | 是 |
| `acknowledged` | 用户已阅读，事实仍在窗口内 | 可配置，默认否 |
| `resolved` | 规则下一轮不再满足或用户主动关闭 | 否 |
| `expired` | 事实窗口自然过期 | 否 |

### 3.4 就诊摘要流程

```text
用户选择 petId + 日期范围
  → Java 校验范围 ≤ 配置上限
  → Java 查询就诊、状态、喂食、体重、用药事实
  → Java 调用 Python /api/medical/summaries
  → Python 生成结构化摘要 + 纯文本 + 安全声明
  → Java 校验响应字段并返回小程序
```

摘要不落库为“模型事实”。如用户需要保存，应由后续成长档案或报告模块另行创建记录。

---

## 4. 领域模型设计

### 4.1 就医档案聚合

建议路径：`domain/model/medical/MedicalVisit.java`

`MedicalVisit` 是就诊记录聚合根，负责：

- 创建和恢复就诊记录。
- 校验日期不能晚于今天，费用不能为负，文本长度符合约束。
- 更新就诊基础信息。
- 标记逻辑删除。
- 维护 `visitDate`、`petId`、`userId` 不可变。

附件关系单独建模为 `MedicalVisitAttachment`，由应用服务负责校验媒体归属和关联事务，不把媒体文件上传逻辑塞进医疗领域实体。

### 4.2 用药计划聚合

建议路径：`domain/model/medical/MedicationPlan.java`

计划字段：

| 字段 | 说明 |
|---|---|
| `medicationName` | 药品名称，必填 |
| `specification` | 规格，如 10mg/片，可空 |
| `purpose` | 处方用途或用户备注，不作诊断结论 |
| `dosageValue` / `dosageUnit` | 每次剂量及单位 |
| `frequencyType` / `frequencyValue` | 每日、每 N 天、每周等频次 |
| `administrationTimes` | 一天内执行时间列表，如 `["09:00", "21:00"]` |
| `mealRelation` | `before_meal/after_meal/any` |
| `startDate` / `endDate` | 疗程范围 |
| `sourceVisitId` | 可选的处方来源就诊记录 |
| `status` | `active/paused/completed/deleted` |

领域行为：

```java
create(...)
update(...)
pause()
resume()
complete()
delete()
isEffectiveOn(LocalDate date)
buildReminderSlots()
```

`buildReminderSlots()` 只负责把已经校验的执行时间转换为提醒创建命令，不直接访问仓储或发送通知。

### 4.3 实际用药记录实体

建议路径：`domain/model/medical/MedicationRecord.java`

状态：

| 状态 | 含义 |
|---|---|
| `taken` | 已实际用药 |
| `skipped` | 用户明确跳过 |
| `missed` | 到期后未完成，由扫描任务补记或查询时计算 |
| `cancelled` | 计划取消导致的历史计划项失效 |

领域规则：

- `takenAt` 不能晚于当前时间加配置容差。
- `dosageValue` 必须大于 0；补录时可以覆盖计划默认剂量，但必须记录备注。
- 同一 `medicationPlanId + scheduledAt` 只能存在一条记录。
- `taken` 和 `skipped` 一旦保存，不能由重复请求创建第二条记录。

### 4.4 健康预警领域模型

建议路径：`domain/model/health/HealthAlert.java`。

```text
HealthAlert
├── ruleCode
├── severity: red / yellow
├── title
├── message
├── evidence: 事实引用列表
├── observedFrom / observedTo
├── fingerprint
├── status
└── firstDetectedAt / lastDetectedAt / acknowledgedAt / resolvedAt
```

领域模型只保存“规则发现了什么事实变化”，不保存“宠物患有什么疾病”。规则消息必须使用中性表述，例如：

- “记录显示今日出现拒食与呕吐组合，建议尽快联系兽医。”
- “最近 2 天的记录中食欲下降且精神低迷，建议持续观察并咨询兽医。”

禁止文案：

- “已经确诊胃肠炎”。
- “需要服用某药，每次多少毫克”。
- “一定是肾病/糖尿病”。

---

## 5. 数据库设计与 Flyway 迁移

### 5.1 迁移文件

新增：

```text
fafa-java/src/main/resources/db/migration/V6__create_medical_health_tables.sql
```

同时更新：

```text
docs/MySQL数据表记录.md
```

迁移要求：

- 所有表使用 `utf8mb4`，金额使用 `DECIMAL`，不使用 MySQL `ENUM`。
- `user_id + pet_id` 作为高频权限过滤条件，索引必须覆盖。
- 业务记录默认逻辑删除；追加型用药记录不物理删除，使用 `cancelled` 表示计划取消后的历史状态。
- JSON 只存展示性列表和规则证据快照，不把可查询的核心业务字段塞入 JSON。

### 5.2 `medical_visit` 就诊档案表

```sql
CREATE TABLE IF NOT EXISTS `medical_visit` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '就诊记录ID',
  `user_id` BIGINT NOT NULL COMMENT '所属用户ID',
  `pet_id` BIGINT NOT NULL COMMENT '宠物ID',
  `visit_date` DATE NOT NULL COMMENT '就诊日期',
  `hospital_name` VARCHAR(200) NOT NULL COMMENT '医院名称',
  `doctor_name` VARCHAR(100) DEFAULT NULL COMMENT '医生姓名',
  `chief_complaint` VARCHAR(1000) DEFAULT NULL COMMENT '主诉',
  `diagnosis` VARCHAR(2000) DEFAULT NULL COMMENT '就诊诊断原文，仅作病历记录',
  `prescription` VARCHAR(3000) DEFAULT NULL COMMENT '处方原文，仅作病历记录',
  `treatment` VARCHAR(2000) DEFAULT NULL COMMENT '治疗/检查项目',
  `total_cost` DECIMAL(10,2) DEFAULT NULL COMMENT '本次就诊费用',
  `notes` VARCHAR(2000) DEFAULT NULL COMMENT '补充备注',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_medical_visit_pet_date` (`pet_id`, `visit_date`, `is_deleted`),
  KEY `idx_medical_visit_user_date` (`user_id`, `visit_date`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='宠物就医档案';
```

### 5.3 `medical_visit_attachment` 就诊附件关系表

复用 Phase 4 的 `media_asset`，不在医疗模块重复上传文件。

```sql
CREATE TABLE IF NOT EXISTS `medical_visit_attachment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '关联ID',
  `user_id` BIGINT NOT NULL COMMENT '所属用户ID',
  `visit_id` BIGINT NOT NULL COMMENT '就诊记录ID',
  `media_id` BIGINT NOT NULL COMMENT 'media_asset ID',
  `attachment_type` VARCHAR(16) NOT NULL DEFAULT 'other' COMMENT 'receipt/report/prescription/other',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '展示顺序',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_medical_visit_media` (`visit_id`, `media_id`),
  KEY `idx_medical_attachment_visit` (`visit_id`, `sort_order`),
  KEY `idx_medical_attachment_media` (`media_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='就诊记录与媒体素材关联';
```

应用层必须同时校验 `medical_visit.user_id` 和 `media_asset.user_id`，禁止把其他用户的媒体挂到当前就诊记录。

### 5.4 `medication_plan` 用药计划表

```sql
CREATE TABLE IF NOT EXISTS `medication_plan` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用药计划ID',
  `user_id` BIGINT NOT NULL COMMENT '所属用户ID',
  `pet_id` BIGINT NOT NULL COMMENT '宠物ID',
  `source_visit_id` BIGINT DEFAULT NULL COMMENT '处方来源就诊记录ID',
  `medication_name` VARCHAR(200) NOT NULL COMMENT '药品名称',
  `specification` VARCHAR(100) DEFAULT NULL COMMENT '药品规格',
  `purpose` VARCHAR(500) DEFAULT NULL COMMENT '处方用途/用户备注',
  `dosage_value` DECIMAL(10,3) NOT NULL COMMENT '每次默认剂量',
  `dosage_unit` VARCHAR(16) NOT NULL COMMENT '剂量单位',
  `frequency_type` VARCHAR(16) NOT NULL COMMENT 'daily/every_days/weekly/as_needed',
  `frequency_value` INT DEFAULT NULL COMMENT '频次值，按 frequency_type 解释',
  `administration_times` JSON NOT NULL COMMENT '执行时间列表，如 ["09:00","21:00"]',
  `meal_relation` VARCHAR(16) NOT NULL DEFAULT 'any' COMMENT 'before_meal/after_meal/any',
  `start_date` DATE NOT NULL COMMENT '疗程开始日期',
  `end_date` DATE DEFAULT NULL COMMENT '疗程结束日期',
  `status` VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT 'active/paused/completed/deleted',
  `notes` VARCHAR(1000) DEFAULT NULL COMMENT '计划备注',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_medication_plan_pet_status` (`pet_id`, `status`, `is_deleted`),
  KEY `idx_medication_plan_user_date` (`user_id`, `start_date`, `end_date`, `is_deleted`),
  KEY `idx_medication_plan_source_visit` (`source_visit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='宠物用药计划';
```

### 5.5 `medication_record` 实际用药记录表

```sql
CREATE TABLE IF NOT EXISTS `medication_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '实际用药记录ID',
  `user_id` BIGINT NOT NULL COMMENT '所属用户ID',
  `pet_id` BIGINT NOT NULL COMMENT '宠物ID',
  `medication_plan_id` BIGINT NOT NULL COMMENT '用药计划ID',
  `reminder_id` BIGINT DEFAULT NULL COMMENT '产生该记录的提醒ID',
  `scheduled_at` DATETIME NOT NULL COMMENT '计划执行时间',
  `taken_at` DATETIME DEFAULT NULL COMMENT '实际用药时间',
  `dosage_value` DECIMAL(10,3) NOT NULL COMMENT '本次剂量',
  `dosage_unit` VARCHAR(16) NOT NULL COMMENT '剂量单位',
  `status` VARCHAR(16) NOT NULL COMMENT 'taken/skipped/missed/cancelled',
  `notes` VARCHAR(500) DEFAULT NULL COMMENT '实际执行备注',
  `operator_id` BIGINT DEFAULT NULL COMMENT '操作人，Phase 10 扩展',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_medication_record_slot` (`medication_plan_id`, `scheduled_at`),
  KEY `idx_medication_record_pet_time` (`pet_id`, `scheduled_at`),
  KEY `idx_medication_record_user_time` (`user_id`, `scheduled_at`),
  KEY `idx_medication_record_reminder` (`reminder_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='实际用药记录';
```

### 5.6 `health_alert` 健康预警表

```sql
CREATE TABLE IF NOT EXISTS `health_alert` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '预警ID',
  `user_id` BIGINT NOT NULL COMMENT '所属用户ID',
  `pet_id` BIGINT NOT NULL COMMENT '宠物ID',
  `rule_code` VARCHAR(64) NOT NULL COMMENT '规则编码',
  `severity` VARCHAR(16) NOT NULL COMMENT 'red/yellow',
  `title` VARCHAR(200) NOT NULL COMMENT '预警标题',
  `message` VARCHAR(1000) NOT NULL COMMENT '中性提示文案',
  `evidence_json` JSON NOT NULL COMMENT '触发事实快照',
  `observed_from` DATE NOT NULL COMMENT '事实窗口开始日期',
  `observed_to` DATE NOT NULL COMMENT '事实窗口结束日期',
  `fingerprint` CHAR(64) NOT NULL COMMENT '规则+窗口+事实摘要指纹',
  `status` VARCHAR(16) NOT NULL DEFAULT 'open' COMMENT 'open/acknowledged/resolved/expired',
  `first_detected_at` DATETIME NOT NULL COMMENT '首次发现时间',
  `last_detected_at` DATETIME NOT NULL COMMENT '最近发现时间',
  `acknowledged_at` DATETIME DEFAULT NULL COMMENT '用户确认时间',
  `resolved_at` DATETIME DEFAULT NULL COMMENT '解除时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_health_alert_fingerprint` (`pet_id`, `rule_code`, `fingerprint`),
  KEY `idx_health_alert_user_status` (`user_id`, `status`, `last_detected_at`),
  KEY `idx_health_alert_pet_status` (`pet_id`, `status`, `last_detected_at`),
  KEY `idx_health_alert_severity` (`severity`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='规则健康预警';
```

### 5.7 与已有表的整合约束

| 关联 | 规则 |
|---|---|
| `medication_plan.source_visit_id` | 只允许关联同用户、同宠物的就诊记录 |
| `reminder.medication_id` | Phase 6 约定指向 `medication_plan.id`；普通 `medication` 提醒必须被拦截 |
| `medication_record.reminder_id` | 只允许关联同计划生成的提醒 |
| `medical_visit_attachment.media_id` | 媒体必须属于当前用户，且不能已删除 |
| `health_alert.evidence_json` | 只保存记录 ID、日期、字段值等最小证据，不保存模型 Prompt |

---

## 6. Java 分层与代码结构

### 6.1 领域层

```text
fafa-java/src/main/java/com/fafa/
├── domain/
│   ├── model/
│   │   ├── medical/
│   │   │   ├── MedicalVisit.java
│   │   │   ├── MedicalVisitAttachment.java
│   │   │   ├── MedicationPlan.java
│   │   │   ├── MedicationRecord.java
│   │   │   ├── MedicationPlanStatus.java
│   │   │   ├── MedicationRecordStatus.java
│   │   │   ├── FrequencyType.java
│   │   │   └── MealRelation.java
│   │   └── health/
│   │       ├── HealthAlert.java
│   │       ├── HealthAlertSeverity.java
│   │       ├── HealthAlertStatus.java
│   │       └── HealthEvidence.java
│   ├── repository/
│   │   ├── MedicalVisitRepository.java
│   │   ├── MedicalVisitAttachmentRepository.java
│   │   ├── MedicationPlanRepository.java
│   │   ├── MedicationRecordRepository.java
│   │   └── HealthAlertRepository.java
│   └── service/
│       └── health/
│           ├── HealthRule.java
│           ├── HealthRuleContext.java
│           ├── HealthRuleEngine.java
│           ├── RefusalVomitingRule.java
│           ├── AppetiteEnergyDeclineRule.java
│           ├── IncreasedWaterIntakeRule.java
│           └── DiarrheaVomitingRule.java
├── application/
│   ├── dto/
│   │   ├── medical/
│   │   └── health/
│   ├── port/
│   │   └── MedicalSummaryGateway.java
│   └── service/
│       ├── MedicalVisitApplicationService.java
│       ├── MedicationApplicationService.java
│       ├── HealthAlertApplicationService.java
│       └── MedicalSummaryApplicationService.java
├── infrastructure/
│   ├── ai/impl/HttpMedicalSummaryGateway.java
│   ├── persistence/
│   │   ├── po/medical/
│   │   ├── mapper/medical/
│   │   └── repository/
│   └── schedule/HealthAlertScheduler.java
└── interfaces/
    ├── controller/MedicalController.java
    ├── controller/HealthAlertController.java
    └── dto/medical/
```

### 6.2 应用服务职责

| 服务 | 负责 | 不负责 |
|---|---|---|
| `MedicalVisitApplicationService` | 就诊 CRUD、媒体关联、时间线聚合 | 文件上传、AI OCR、规则计算 |
| `MedicationApplicationService` | 计划 CRUD、提醒同步、实际用药记录幂等 | 直接发送通知、健康诊断 |
| `HealthAlertApplicationService` | 加载事实、运行规则、预警去重、确认/解除 | 规则内部查库、模型调用 |
| `MedicalSummaryApplicationService` | 日期范围、权限、事实组装、Python 响应校验 | Prompt 编排、直接访问数据库外部连接 |
| `HealthRuleEngine` | 顺序执行规则并合并结果 | 保存预警、发送通知 |

### 6.3 事务边界

- 创建/编辑/删除就诊记录：单事务。
- 关联附件：校验就诊和素材归属后单事务保存关联。
- 创建用药计划：保存计划 + 创建对应提醒必须在同一事务中完成；任一步失败整体回滚。
- 完成 medication 提醒：提醒状态推进、完成历史、`medication_record` 写入必须在同一事务中完成。
- 预警评估：每条规则的 upsert 可在一个事务内完成；单条规则失败不能阻塞其他规则，失败必须记录 `ERROR` 日志并保留下次重试入口。
- 就诊摘要：不写业务数据，Java 读取事实和调用 Python 不放在数据库写事务中。

---

## 7. 公开接口设计（Java）

所有接口前缀为 `/api`，使用现有 `Result<T>`，要求登录态。日期使用 `yyyy-MM-dd`，时间使用 ISO-8601。公开接口必须在 Knife4j 中可见。

### 7.1 就诊档案接口

| 方法 | 地址 | 用途 |
|---|---|---|
| `POST` | `/api/medical/visits` | 创建就诊记录 |
| `GET` | `/api/medical/visits?petId=&startDate=&endDate=` | 分页查询就诊记录 |
| `GET` | `/api/medical/visits/{visitId}` | 就诊详情及附件 |
| `PUT` | `/api/medical/visits/{visitId}` | 编辑就诊记录 |
| `DELETE` | `/api/medical/visits/{visitId}` | 逻辑删除就诊记录 |
| `POST` | `/api/medical/visits/{visitId}/attachments` | 关联已有媒体素材 |
| `DELETE` | `/api/medical/visits/{visitId}/attachments/{mediaId}` | 解除媒体关联 |
| `GET` | `/api/medical/timeline?petId=&startDate=&endDate=` | 查询医疗时间线 |

创建请求：

```json
{
  "petId": 1,
  "visitDate": "2026-08-20",
  "hospitalName": "和平宠物医院",
  "doctorName": "李医生",
  "chiefComplaint": "近两天食欲下降，偶尔呕吐",
  "diagnosis": "医生填写的诊断原文",
  "prescription": "处方原文，供后续核对",
  "treatment": "血常规、补液",
  "totalCost": 380.00,
  "notes": "下周复诊"
}
```

响应示例：

```json
{
  "code": 0,
  "message": "成功",
  "data": {
    "id": 10001,
    "petId": 1,
    "visitDate": "2026-08-20",
    "hospitalName": "和平宠物医院",
    "doctorName": "李医生",
    "chiefComplaint": "近两天食欲下降，偶尔呕吐",
    "diagnosis": "医生填写的诊断原文",
    "prescription": "处方原文，供后续核对",
    "treatment": "血常规、补液",
    "totalCost": 380.00,
    "attachments": [],
    "createdAt": "2026-08-23T15:00:00"
  }
}
```

附件关联请求：

```json
{
  "mediaIds": [301, 302],
  "attachmentType": "receipt"
}
```

时间线响应中的 `type` 固定为：

```text
medical_visit / vaccination / deworming / checkup / medication
```

时间线只返回事实快照，不把健康规则推断混入历史事件。

### 7.2 用药计划接口

| 方法 | 地址 | 用途 |
|---|---|---|
| `POST` | `/api/medical/medication-plans` | 创建用药计划并生成提醒 |
| `GET` | `/api/medical/medication-plans?petId=&status=` | 查询计划 |
| `GET` | `/api/medical/medication-plans/{planId}` | 查询计划详情及关联提醒 |
| `PUT` | `/api/medical/medication-plans/{planId}` | 编辑未完成计划 |
| `POST` | `/api/medical/medication-plans/{planId}/pause` | 暂停计划及提醒 |
| `POST` | `/api/medical/medication-plans/{planId}/resume` | 恢复计划及提醒 |
| `POST` | `/api/medical/medication-plans/{planId}/complete` | 提前结束疗程 |
| `DELETE` | `/api/medical/medication-plans/{planId}` | 逻辑删除计划 |

创建请求：

```json
{
  "petId": 1,
  "sourceVisitId": 10001,
  "medicationName": "阿莫西林",
  "specification": "50mg/片",
  "purpose": "按就诊记录执行",
  "dosageValue": 0.5,
  "dosageUnit": "片",
  "frequencyType": "daily",
  "frequencyValue": 2,
  "administrationTimes": ["09:00", "21:00"],
  "mealRelation": "after_meal",
  "startDate": "2026-08-20",
  "endDate": "2026-08-27",
  "notes": "如有异常按医嘱处理"
}
```

响应示例：

```json
{
  "code": 0,
  "message": "成功",
  "data": {
    "id": 20001,
    "petId": 1,
    "medicationName": "阿莫西林",
    "dosageValue": 0.5,
    "dosageUnit": "片",
    "frequencyType": "daily",
    "frequencyValue": 2,
    "administrationTimes": ["09:00", "21:00"],
    "mealRelation": "after_meal",
    "startDate": "2026-08-20",
    "endDate": "2026-08-27",
    "status": "active",
    "reminders": [
      {"id": 50001, "nextRemindTime": "2026-08-23T21:00:00", "status": "pending"}
    ]
  }
}
```

### 7.3 实际用药记录接口

| 方法 | 地址 | 用途 |
|---|---|---|
| `POST` | `/api/medical/medication-records` | 手动补记/标记实际用药 |
| `GET` | `/api/medical/medication-records?petId=&startDate=&endDate=` | 查询用药日历和历史 |
| `GET` | `/api/medical/medication-records/{recordId}` | 查询记录详情 |

补记请求：

```json
{
  "petId": 1,
  "medicationPlanId": 20001,
  "scheduledAt": "2026-08-23T09:00:00",
  "takenAt": "2026-08-23T09:15:00",
  "status": "taken",
  "dosageValue": 0.5,
  "dosageUnit": "片",
  "notes": "已完成"
}
```

跳过请求使用同一接口，将 `status` 设为 `skipped`，`takenAt` 可空。重复提交同一 `medicationPlanId + scheduledAt` 必须返回已有记录或幂等成功，不得插入第二条。

### 7.4 健康预警接口

| 方法 | 地址 | 用途 |
|---|---|---|
| `GET` | `/api/health-alerts?petId=&status=` | 查询预警卡片 |
| `GET` | `/api/health-alerts/{alertId}` | 预警详情和事实证据 |
| `POST` | `/api/health-alerts/evaluate` | 手动触发指定宠物评估，便于测试和用户刷新 |
| `POST` | `/api/health-alerts/{alertId}/acknowledge` | 用户确认已读 |
| `POST` | `/api/health-alerts/{alertId}/resolve` | 用户主动关闭 |

手动评估请求：

```json
{
  "petId": 1,
  "asOfDate": "2026-08-23"
}
```

预警响应示例：

```json
{
  "code": 0,
  "message": "成功",
  "data": {
    "id": 70001,
    "petId": 1,
    "ruleCode": "REFUSAL_WITH_VOMITING",
    "severity": "red",
    "title": "今日出现需要关注的组合变化",
    "message": "记录显示今日出现拒食与呕吐组合，建议尽快联系兽医。",
    "status": "open",
    "observedFrom": "2026-08-23",
    "observedTo": "2026-08-23",
    "evidence": [
      {"sourceType": "daily_status", "sourceId": "90001", "field": "appetite", "value": "refused"},
      {"sourceType": "daily_status", "sourceId": "90001", "field": "vomiting", "value": "once"}
    ],
    "lastDetectedAt": "2026-08-23T15:30:00"
  }
}
```

### 7.5 就医摘要接口

| 方法 | 地址 | 用途 |
|---|---|---|
| `POST` | `/api/medical/summaries` | 生成就诊前摘要 |

请求：

```json
{
  "petId": 1,
  "startDate": "2026-08-01",
  "endDate": "2026-08-23",
  "includeMedia": false
}
```

响应：

```json
{
  "code": 0,
  "message": "成功",
  "data": {
    "pet": {"id": 1, "name": "豆包", "type": "cat"},
    "range": {"startDate": "2026-08-01", "endDate": "2026-08-23"},
    "overview": "本时间范围内记录到 2 次就诊、3 次异常状态和 12 次用药执行记录。",
    "symptomTimeline": [
      {"date": "2026-08-20", "items": ["食欲下降", "呕吐 1 次"]}
    ],
    "medicalVisits": [
      {"date": "2026-08-20", "hospitalName": "和平宠物医院", "diagnosis": "医生填写的诊断原文"}
    ],
    "medications": [
      {"name": "阿莫西林", "plan": "每日 2 次", "takenCount": 6, "missedCount": 1}
    ],
    "attentionItems": ["请将原始处方和检查报告一并带给医生"],
    "plainText": "豆包就诊摘要（2026-08-01 至 2026-08-23）...",
    "disclaimer": "本摘要仅整理 FaFa 中的用户记录，不构成诊断或治疗建议。"
  }
}
```

范围约束：`endDate` 不得晚于今天，范围不超过配置的 `medical.summary-max-range-days`，默认 90 天；缺少记录时返回空数组和明确说明，不以模型常识补全。

---

## 8. Python Agent 与摘要服务改造

### 8.1 新增 Python 路由

```text
fafa-python/app/api/medical.py
fafa-python/app/service/medical_summary_service.py
fafa-python/app/schemas/medical.py
```

新增接口：

```http
POST /api/medical/summaries
```

Python 只接受 Java 传入的 `user_id`、`pet_id`、日期范围和请求标识，随后通过 `JavaQueryClient` 调用内部只读接口获取事实。Python 不接收客户端直传的“已经整理好的诊断结论”，避免绕过 Java 的数据权限和事实来源。

### 8.2 Phase 5 Agent 工具扩展

在现有静态工具注册表中增加：

| 工具 | Java 内部接口 | 只读 |
|---|---|---|
| `getMedicalHistory` | `GET /internal/ai/pets/{petId}/medical-visits` | 是 |
| `getMedicationPlans` | `GET /internal/ai/pets/{petId}/medication-plans` | 是 |
| `getMedicationHistory` | `GET /internal/ai/pets/{petId}/medication-records` | 是 |
| `getHealthAlerts` | `GET /internal/ai/pets/{petId}/health-alerts` | 是 |

新增意图：

```text
medical_history_query
medication_query
health_alert_query
medical_summary_request
```

工具返回统一证据格式：

```json
{
  "tool": "getMedicationHistory",
  "data": {
    "petId": 1,
    "records": [],
    "range": {"startDate": "2026-08-01", "endDate": "2026-08-23"}
  },
  "source": "mysql",
  "traceId": "..."
}
```

### 8.3 医疗安全 Prompt 与输出校验

Python 摘要和 Agent 回答都必须满足：

- 用“记录显示、记录中出现、用户填写、截至某日”等事实性表达。
- 原样保留用户填写的诊断/处方文本，并标注“病历原文”，不由模型改写成新的诊断。
- 将规则预警与模型摘要分开返回，模型不能覆盖规则严重级别。
- 统一附加安全声明：`本内容仅整理已有记录，不构成诊断或治疗建议。`
- 命中“确诊、处方、剂量调整、停药、换药”等生成内容时，返回安全降级文案并记录 traceId。

### 8.4 摘要失败降级

| 失败 | 处理 |
|---|---|
| Java 内部查询超时 | Python 返回 `AgentError`，Java 返回可重试错误 |
| 模型调用超时 | 返回确定性模板摘要，不生成推断句 |
| 模型返回 JSON 不合法 | 丢弃模型结构，使用 Java 传入的统计数字和原始记录摘要 |
| 某一个数据源不可用 | 在 `warnings` 中说明缺失数据，不把缺失当作正常 |
| 内容触发医疗边界 | 不返回原始内容，固定安全提示 |

---

## 9. 健康预警规则设计

### 9.1 规则接口

建议接口：

```java
public interface HealthRule {

    String code();

    Optional<HealthAlertDraft> evaluate(HealthRuleContext context);
}
```

`HealthRuleContext` 由应用服务按配置加载：

```text
petProfile
dailyStatuses
feedingRecords
weightRecords
medicationRecords
asOfDate
windowStart
windowEnd
```

规则不查询数据库；规则引擎不调用 Python；所有日期计算使用注入的 `Clock`，避免测试依赖系统当前时间。

### 9.2 首批规则

| 编码 | 触发条件 | 等级 | 文案边界 |
|---|---|---|---|
| `REFUSAL_WITH_VOMITING` | 同一天 `appetite=refused` 且 `vomiting=once/multiple` | red | 建议尽快联系兽医，不写疾病名称 |
| `APPETITE_ENERGY_DECLINE_2D` | 连续至少 2 个有记录日期同时出现食欲下降/拒食和精神低迷 | yellow | 描述连续记录，不说已经患病 |
| `INCREASED_WATER_3D` | 连续至少 3 个有记录日期 `water_intake=more` | yellow | 提示关注并咨询兽医，不下肾脏/血糖判断 |
| `DIARRHEA_2D` | 连续至少 2 个有记录日期 `defecation=diarrhea` | yellow | 描述排便记录变化 |
| `VOMITING_MULTIPLE` | 单日 `vomiting=multiple` | yellow | 提示尽快关注和咨询兽医 |
| `MEDICATION_MISSED_2` | 同一有效计划在观察窗口内至少 2 次 `missed/skipped` | yellow | 这是执行提醒，不推断病情 |

规则实现要求：

- 没有记录的日期不能当作“正常”，只参与“连续有记录日期”的判断。
- 同一日的每日状态最多一条，沿用 Phase 2 唯一键语义。
- 规则证据必须包含源表、源记录 ID、字段、值和日期。
- 规则阈值全部来自 `HealthRuleProperties`，不能散落在规则类中。
- 规则只生成预警草稿，保存和去重由 `HealthAlertApplicationService` 负责。

### 9.3 预警生成与去重

建议 fingerprint 输入：

```text
petId + ruleCode + observedFrom + observedTo + sorted(evidence sourceId/value)
```

对输入做 SHA-256 得到 64 位十六进制指纹。相同指纹重复扫描时只更新 `last_detected_at`；新的事实窗口或事实值变化时生成新预警。

旧预警处理：

- 规则本轮不再满足，且状态为 `open`/`acknowledged`：更新为 `resolved`。
- 超过 `alert-retention-days` 未再发现：可由归档任务转为 `expired`，Phase 6 不物理删除。
- 红色预警在同一 fingerprint 下默认不重复推送，重复通知间隔由配置控制。

### 9.4 调度与手动评估

新增 `HealthAlertScheduler`：

- 默认每 30 分钟扫描一次当前有效宠物。
- 每次只加载配置窗口内的数据，避免无界查询。
- 单个宠物失败记录错误并继续下一个宠物。
- 通过 `fafa.health-alert.enabled` 可关闭调度，接口手动评估仍可用于测试。
- 开发环境通知先复用现有日志推送边界；生产接入微信订阅消息前必须完成模板、用户授权和 access token 配置，不在规则代码中硬编码。

---

## 10. 就医摘要设计

### 10.1 数据范围

Java 端按 `petId + startDate + endDate` 查询：

1. 宠物基础资料：名称、物种、年龄/生日、当前体重。
2. `daily_status`：异常维度和备注，不把没有记录日期当作正常。
3. `feeding_record`：次数、总量（有单位时统计）。
4. `weight_record`：首条、末条、变化值和日期。
5. `medical_visit`：日期、医院、主诉、诊断原文、处方原文、治疗、费用。
6. `medication_plan`：药名、剂量、疗程和计划状态。
7. `medication_record`：已完成、跳过、漏记数量。
8. 已有 `health_alert`：只带预警标题和事实窗口，不让模型重算等级。

### 10.2 输出结构

```text
MedicalSummaryResponse
├── pet
├── range
├── overview
├── symptomTimeline[]
├── medicalVisits[]
├── medications[]
├── healthChanges[]
├── attentionItems[]
├── warnings[]
├── plainText
└── disclaimer
```

`plainText` 用于复制给医生；小程序展示结构化分区，并提供复制和 Canvas 保存长图按钮。生成摘要不自动写入成长档案，避免把模型表达当作原始医疗记录。

### 10.3 AI 生成约束

Prompt 只允许模型完成：

- 将事实按时间排序。
- 将多个记录压缩成清晰的就医交接语言。
- 标记缺失数据和记录冲突。
- 复述用户已填写的诊断/处方原文。

Prompt 禁止模型完成：

- 判断疾病、病因、严重程度或预后。
- 修改、补充或纠正医生原始诊断。
- 推荐药物、改变剂量、建议停药。
- 使用没有证据的时间、数字或症状。

---

## 11. 小程序页面与交互

### 11.1 页面清单

```text
pages/medical/index        健康首页：预警卡片、近期就诊、今日用药
pages/medical/visit-form   新建/编辑就诊档案
pages/medical/visit-detail 就诊详情与附件
pages/medical/timeline     医疗时间线
pages/medical/medication   用药计划列表
pages/medical/med-form      新建/编辑用药计划
pages/medical/calendar      用药日历与完成状态
pages/medical/summary       就诊摘要预览、复制、保存长图
```

### 11.2 健康首页

排序：

1. 红色 `open` 预警置顶。
2. 黄色 `open` 预警按最近发现时间排序。
3. 今日用药待办。
4. 最近一次就诊记录。
5. 进入就医摘要的快捷入口。

预警卡片必须显示：宠物名称、严重级别、事实日期、提示文案、查看证据和“我知道了”按钮。不能只显示一个没有证据来源的“健康风险”标签。

### 11.3 用药计划交互

- 创建计划时可从就诊档案选择处方来源，也可手动创建。
- `administrationTimes` 支持一天多个时间，保存前显示“将生成几条提醒”。
- 计划页区分“计划剂量”和“实际剂量”，实际剂量默认为计划剂量但允许补录修改。
- 点击提醒完成后展示“已记录用药”，再次点击不重复提交。
- 跳过需要可选原因，不能悄悄将未操作状态标记为已完成。
- 结束疗程后，关联提醒进入完成/结束态，历史实际用药记录保留。

### 11.4 就诊摘要交互

- 提供“近 7 天”“近 30 天”“自定义”三个入口。
- 生成前显示范围和数据来源数量。
- 结果页分为概览、症状时间线、就诊、用药、需要带给医生的信息。
- “复制文本”复制 `plainText`；“保存长图”只保存当前结构化页面，不发到外部平台。
- 明确显示安全声明，避免用户把摘要误认为诊断结果。

### 11.5 页面与接口错误处理

| 错误 | 用户反馈 |
|---|---|
| 宠物无权限/记录不存在 | 返回统一“内容不存在或已删除” |
| 摘要服务超时 | 保留已查询的结构化数据，提示稍后重试 |
| 预警评估失败 | 不覆盖已有预警，显示“刷新失败” |
| 重复完成用药 | 展示已有完成记录，不弹出错误打断用户 |
| 附件已删除 | 从就诊详情隐藏并提示素材不可用 |

---

## 12. 配置设计

### 12.1 Java 配置

在 `FaFaProperties` 增加 `Medical`、`HealthAlert` 配置对象，建议配置项：

```yaml
fafa:
  medical:
    visit-max-range-days: 366
    summary-max-range-days: 90
    summary-timeout-seconds: 45
    medication-future-tolerance-minutes: 5
    max-administration-times: 6
  health-alert:
    enabled: true
    scan-cron: "0 */30 * * * *"
    daily-window-days: 7
    increased-water-days: 3
    appetite-energy-decline-days: 2
    diarrhea-days: 2
    missed-medication-count: 2
    red-repeat-hours: 24
    retention-days: 180
```

规则文案和规则编码作为领域常量/枚举管理；阈值、扫描周期、范围和通知去重间隔进入配置，测试通过构造配置覆盖边界。

### 12.2 Python 配置

在 `Settings` 增加：

```text
medical_summary_timeout_seconds
medical_summary_max_output_tokens
medical_summary_max_records
```

模型名继续通过 `app.core.llm.get_chat_model(Purpose.CHAT_DEEP)` 获取，不能在摘要服务中直接写模型名称。

### 12.3 敏感配置

- 微信订阅消息模板 ID、access token 等只放 `.env` 或部署环境变量。
- 医疗原文不能进入普通 INFO 日志；日志只记录 `userId`、`petId`、`visitId`、`traceId` 和数量。
- Python Prompt 和完整病历内容不写入应用日志。

---

## 13. 实施顺序与任务拆分

### Phase 6.0：前置验收与契约冻结

目标：不改业务代码，先确认 Phase 0～5 环境和数据契约。

任务：

1. 验证当前 Flyway 版本为 V5，确认 V6 不与现有迁移冲突。
2. 验证 `reminder.medication_id` 已存在且当前 medication 完成不会错误生成成长事件。
3. 固定医疗接口字段、日期范围、错误码和脱敏日志规则。
4. 确认小程序上传图片先走既有媒体接口，再关联就诊记录。

产物：接口契约表、迁移评审记录、测试数据脚本。

### Phase 6.1：数据库与就医档案

任务：

1. 编写 V6 迁移和回滚检查清单。
2. 实现 `MedicalVisit` 聚合和 Repository 接口。
3. 实现 PO、Mapper、Repository 实现和应用服务。
4. 实现就诊 CRUD、附件关联和医疗时间线。
5. 更新 `ErrorCode`、DTO、Knife4j 注解和接口文档。

验收：可以创建、编辑、删除、查询就诊记录；越权访问失败；附件归属正确。

### Phase 6.2：用药计划与提醒闭环

任务：

1. 实现 `MedicationPlan` 和 `MedicationRecord` 领域模型。
2. 实现频次、执行时间、疗程范围和用药状态校验。
3. 通过 `ReminderApplicationService` 或窄接口创建/暂停/恢复 medication 提醒。
4. 修改提醒完成事务：`type=medication` 时写入 `medication_record`。
5. 加入实际用药记录幂等约束和补录接口。
6. 增加计划完成、提前结束和提醒清理逻辑。

验收：一条计划生成多个执行时间提醒；完成提醒只产生一条用药记录；暂停/恢复状态一致。

### Phase 6.3：纯规则健康预警

任务：

1. 定义 `HealthRuleContext`、`HealthRule` 和证据结构。
2. 实现首批六条规则和阈值配置。
3. 实现 `HealthAlert` 持久化、指纹去重和状态转移。
4. 实现手动评估接口和定时扫描任务。
5. 接入首页查询和确认接口。
6. 复用现有提醒推送边界，补齐红色预警去重日志。

验收：同一事实重复扫描不重复插入；规则只消费有记录的日期；红/黄级文案通过安全审查。

### Phase 6.4：就诊摘要与 Agent 扩展

任务：

1. 实现 Java 事实聚合和摘要 Gateway。
2. 实现 Python medical schema、service、route 和安全 Prompt。
3. 扩展 Java 内部只读医疗工具。
4. 扩展 Phase 5 Agent 意图和工具注册表。
5. 增加模型超时、JSON 校验、确定性降级摘要。

验收：摘要中的日期、药品、用药数量均能追溯到事实；模型不可输出诊断/处方；服务不可用时不影响既有医疗 CRUD。

### Phase 6.5：小程序页面与联调

任务：

1. 健康首页接入预警、今日用药、最近就诊。
2. 完成就诊档案表单、附件关联和时间线。
3. 完成用药计划和用药日历。
4. 完成就诊摘要预览、复制和保存长图。
5. 使用真实 MySQL、Redis、MinIO、Java、Python 完成端到端验证。

验收：主流程可从小程序操作完成，接口重试、重复点击和服务降级均符合预期。

### Phase 6.6：文档、回归和发布准备

任务：

1. 更新 `docs/接口文档.md`、`docs/MySQL数据表记录.md`、`docs/Codex开发交接文档.md`。
2. 更新 `docs/技术选型与项目框架搭建.md` 的 medical 内部契约。
3. 运行 Java/Python 全量测试和迁移验证。
4. 完成安全、日志、权限、性能和回滚检查。
5. 输出 Phase 6 完成报告和已知限制。

---

## 14. 测试与验收标准

### 14.1 Java 领域单元测试

必须覆盖：

- `MedicalVisit`：未来日期、负费用、字段长度、更新和逻辑删除。
- `MedicationPlan`：每日多次执行时间、空时间列表、重复时间、结束日期早于开始日期、暂停/恢复/结束。
- `MedicationRecord`：实际时间容差、剂量校验、taken/skipped/missed 状态和重复槽位。
- 每条 `HealthRule`：正例、边界值、缺少记录、跨日期窗口、重复事实。
- `HealthAlert`：确认、解决、过期和同指纹去重。

### 14.2 Java 应用服务测试

- 就诊记录只能由宠物所有者创建和读取。
- 就诊附件不能关联其他用户素材。
- 处方来源就诊记录必须与计划属于同一宠物。
- 创建计划与创建提醒保持事务一致。
- medication 提醒完成后只写一条用药记录。
- 重复请求返回幂等结果，不重复插入。
- 规则引擎单个规则失败不会吞掉其他规则结果。
- 摘要日期范围超过上限时不调用 Python。

### 14.3 Python 测试

- Schema 拒绝非法 ID、空消息、超长日期范围。
- `JavaQueryClient` 正确传递内部令牌和 traceId。
- 医疗摘要 Prompt 不包含允许诊断的指令。
- 模型返回非法 JSON 时进入确定性降级。
- 记录原文中的诊断/处方不会被模型扩展成新结论。
- 医疗问答触发“停药/换药/改剂量/确诊”时返回安全边界文案。
- Java 查询超时、404、500 的错误映射清晰且不泄露病历原文。

### 14.4 接口契约测试

使用 Mock Java 内部接口和 Python 摘要接口验证：

| 契约 | 必测内容 |
|---|---|
| Java → Python 摘要 | snake_case 字段、日期、trace_id、响应结构 |
| Python → Java 医疗查询 | `user_id`、`pet_id`、日期范围、内部令牌 |
| Reminder → Medication | medication_id、scheduled_at、幂等键 |
| Media → Medical | media_id 归属、删除状态、附件类型 |

### 14.5 端到端验收场景

#### 场景 A：创建就诊档案

1. 上传一张票据图片得到 `mediaId`。
2. 创建就诊记录。
3. 关联票据图片。
4. 查询详情和时间线，确认医院、费用、附件均展示。
5. 删除就诊记录后，媒体素材仍可在素材库查询。

#### 场景 B：用药计划多次执行

1. 创建每日 09:00 和 21:00 的 7 天计划。
2. 确认生成两个 medication 提醒。
3. 完成 09:00 提醒，确认生成一条 `taken` 记录。
4. 重复点击完成，确认不新增记录。
5. 暂停计划，确认两个提醒均暂停。

#### 场景 C：健康预警

1. 写入同日拒食和呕吐记录。
2. 手动评估，确认产生一条 red 预警。
3. 再次评估，确认不重复插入。
4. 确认预警，首页默认不再显示未读卡片。
5. 事实窗口变化后，旧预警转 resolved 或生成新指纹预警。

#### 场景 D：就诊摘要

1. 选择近 30 天。
2. 确认摘要包含状态、就诊、用药和预警事实。
3. 复制纯文本，确认包含日期和安全声明。
4. 关闭 Python，确认 Java 返回可理解的降级错误或确定性摘要。

#### 场景 E：越权

1. 用户 A 使用用户 B 的 `petId` 查询就诊、用药和预警。
2. 用户 A 使用用户 B 的 `visitId`、`planId`、`alertId` 操作资源。
3. 所有请求均不能读取、修改或确认用户 B 的数据。

#### 场景 F：医疗边界

1. 在 AI 对话中询问“是不是肾病”。
2. 在摘要中出现医生填写的诊断原文。
3. 要求 AI 修改剂量或建议停药。
4. 系统只能描述事实并建议咨询兽医，不能给出诊断/处方结论。

---

## 15. 观测性、性能与安全

### 15.1 日志字段

关键日志统一包含：

```text
traceId, userId, petId, visitId/planId/alertId, operation, result, durationMs
```

不得记录：

- 处方全文、诊断全文、病历图片 URL 的签名参数。
- Python 完整 Prompt 和模型原始返回。
- access token、内部令牌和用户敏感身份信息。

### 15.2 指标

建议增加 Micrometer 指标：

```text
fafa_medical_visit_create_total
fafa_medication_plan_create_total
fafa_medication_record_status_total{status}
fafa_health_alert_evaluate_total{rule_code,result}
fafa_health_alert_open_total{severity}
fafa_medical_summary_total{result}
fafa_medical_summary_duration_ms
```

### 15.3 性能约束

- 预警评估按宠物批量读取数据，禁止每条规则重复查询数据库。
- 医疗时间线使用有限日期范围和分页，禁止无限制返回全部历史。
- 摘要默认最多 90 天；每类事实数量通过配置限制，并在响应中返回截断警告。
- 用药日历按月份或日期范围查询，禁止一次加载全部疗程历史。
- 规则指纹和唯一索引保证重复扫描不会产生数据膨胀。

### 15.4 并发与幂等

- 用药计划更新与提醒同步必须在同一事务中完成，必要时按计划 ID加锁。
- 用药完成使用数据库唯一键作为最终幂等保障，应用层先查只是优化。
- 健康预警采用唯一指纹约束；并发扫描遇到唯一键冲突时重新读取已有记录，不返回系统错误。
- 摘要接口不做结果持久化缓存，先保证事实新鲜度；后续若增加缓存，缓存键必须包含用户、宠物和日期范围。

---

## 16. 风险与应对

| 风险 | 影响 | 应对 |
|---|---|---|
| 现有提醒完成逻辑未接入 medication_record | 用药闭环断裂 | 先写 ReminderApplicationService 集成测试，再开放计划创建 |
| 每日多次用药与单提醒模型不一致 | 重复提醒或漏提醒 | 一个计划按执行时间拆成多条 reminder，并统一由 plan ID 管理 |
| 健康规则阈值缺少兽医确认 | 误报或用户不信任 | 文案只描述数据变化；阈值配置化；上线前完成产品/兽医审核 |
| 缺失记录被误判为正常 | 产生错误预警 | 规则区分“未记录”和“正常”，连续规则只统计有记录日期 |
| 医疗原文被 AI 改写 | 用户误解病历 | 原文单独展示；摘要只做排序压缩；增加输出安全校验 |
| Python 服务不可用 | 摘要无法生成 | 医疗 CRUD 和规则预警不依赖 Python；摘要提供重试和确定性降级 |
| 微信订阅消息未配置 | 红色预警无法真正推送 | 首页红色置顶先可用；生产发布前把推送配置作为上线门槛 |
| Phase 8 记账尚未完成 | 就诊费用无法自动进入账单 | Phase 6 保存费用事实，Phase 8 再定义跨模块记账策略 |
| 迁移数据量大或索引不合理 | 查询变慢 | 先在测试库执行 explain 和基准数据压测，再发布 V6 |

---

## 17. 交付清单

### 17.1 Java

- [ ] V6 Flyway 迁移脚本。
- [ ] 就医档案领域模型、仓储接口、PO、Mapper、Repository 实现。
- [ ] 用药计划、实际用药记录领域模型和应用服务。
- [ ] 健康预警模型、规则接口、首批规则和规则引擎。
- [ ] 就医档案、用药、预警、摘要公开 Controller。
- [ ] Java → Python 医疗摘要 Gateway。
- [ ] Reminder 与 medication_record 事务集成。
- [ ] 错误码、配置、日志和 OpenAPI 分组。

### 17.2 Python

- [ ] `medical.py` 路由和 Pydantic schema。
- [x] `MedicalSummaryService` 和安全 Prompt。
- [x] 医疗内部 Java 查询客户端方法。
- [x] 模型超时、非法响应和确定性降级。
- [x] 医疗摘要基础测试与 Python 回归测试。

### 17.3 小程序

- [x] 健康首页与预警卡片。
- [ ] 就诊档案 CRUD 和附件关联页面。
- [ ] 医疗时间线独立页面。
- [ ] 用药计划、提醒完成和用药日历页面。
- [x] 医疗摘要预览基础页面。

### 17.4 文档

- [x] `docs/接口文档.md` 增加 Phase 6 公开接口和 Python/Java 内部接口。
- [x] `docs/MySQL数据表记录.md` 增加 V6 表结构和索引说明。
- [x] `docs/技术选型与项目框架搭建.md` 更新 medical 服务间契约。
- [x] `docs/Codex开发交接文档.md` 更新阶段进度、运行配置和已知限制。
- [ ] Phase 6 真实环境联调记录和验收结果。

---

## 18. Phase 6 完成标准

1. **数据库**
   - [ ] V6 迁移在空库和现有 V5 数据库均成功执行。
   - [ ] 就诊、附件、用药计划、用药记录、健康预警表索引通过评审。
   - [ ] 唯一键和逻辑删除语义符合本文档。

2. **就医档案**
   - [ ] 就诊 CRUD、附件关联、医疗时间线完成。
   - [ ] 疫苗/驱虫/体检提醒完成历史能出现在时间线。
   - [ ] 删除就诊不会删除媒体和其他历史事实。

3. **用药管理**
   - [ ] 计划支持每日多次执行时间、疗程、饭前/饭后和暂停/恢复。
   - [ ] 计划能生成并管理 medication 提醒。
   - [ ] 完成提醒能幂等写入实际用药记录。
   - [ ] 用药日历能区分已完成、跳过、漏记和未到期。

4. **健康预警**
   - [ ] 首批六条规则全部有单元测试和真实数据验收。
   - [ ] 预警有证据、等级、状态和去重指纹。
   - [ ] 红色预警首页置顶，规则文案通过医疗边界审查。
   - [ ] 定时扫描支持关闭、重试和失败隔离。

5. **就诊摘要与 AI**
   - [x] 自定义范围可生成摘要；近 7 天、近 30 天由请求日期范围覆盖。
   - [ ] 摘要内容可追溯到既有数据，不生成无证据结论。
   - [x] Python 不可用或模型失败时有明确错误/事实摘要降级。
   - [x] Agent 能查询就医/用药事实，但不能写入医疗表。

6. **安全与质量**
   - [ ] 全部公开接口完成登录、宠物归属、资源归属测试。
   - [ ] 病历和处方原文不出现在普通日志。
   - [ ] Java 全量测试、Python 全量测试、契约测试和核心端到端场景通过。
   - [ ] Knife4j、接口文档、MySQL 记录和交接文档已同步。

---

## 19. 实施自检清单

### Architecture Delineation

- [x] Java `interfaces → application → domain ← infrastructure` 依赖方向清晰。
- [x] 规则类只处理内存上下文，数据库查询和保存由应用服务负责。
- [x] Python 只做摘要表达和 Agent 编排，不能直接连接 MySQL/Qdrant 或写医疗业务表。
- [x] 医疗时间线采用查询时聚合，避免为派生数据增加不必要的同步表。

### Production-Ready Code

- [ ] 所有阈值、周期、超时、范围和文案策略可配置或由领域常量集中管理。
- [ ] 所有跨模块写入均有事务边界、幂等键和失败日志。
- [ ] 所有外部调用均有超时、错误映射、traceId 和降级路径。
- [ ] 所有用户输入经过 Bean Validation 与领域层二次校验。

### Self-Review Report

- **Hard-coding**：预警阈值、时间窗口、摘要范围、通知去重间隔和模型超时放入配置；规则编码和状态使用枚举/常量集中管理。
- **Coupling/Cohesion**：Java 保持权限、事实和事务中心；Python 仅依赖窄的内部查询契约；规则不直接访问仓储；医疗时间线不新增派生同步表。
- **Type Safety**：Java 使用领域枚举、不可变命令 DTO 和明确的 Repository 接口；Python 使用 Pydantic schema、TypedDict/类型注解和明确的外部服务异常。

---

## 20. 待产品确认项

这些问题不影响先完成基础代码，但在 Phase 6.0 契约冻结前必须确认：

1. 健康预警红色/黄色阈值是否由兽医或可信资料源审核，并确认最终文案。
2. “确认预警”后是否立即从首页隐藏，还是保留为已读卡片。
3. 用药计划是否允许 `as_needed` 按需用药；首版建议只允许用户手动记录，不自动生成周期提醒。
4. 处方原文中的药名和剂量是否允许用户编辑；首版建议允许编辑计划，但保留来源就诊记录原文不变。
5. 微信订阅消息模板、接收授权和红色预警推送策略是否在本阶段完成生产接入。
6. 就诊摘要的默认范围是近 7 天还是近 30 天；本文档接口同时支持两者。
7. 就诊费用在 Phase 8 记账中的归属策略：自动创建账单，还是由用户确认后创建。

---

**文档版本**：v1.0  
**计划日期**：2026-08-23  
**适用阶段**：Phase 6  
**维护者**：FaFa 开发组
