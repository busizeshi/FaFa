# Phase 9 实施文档：报告与内容创作

> 依据：《FaFa产品功能设计-最终版.md》第十章“报告与内容创作”、第十一章 AI 助手、第十八章版本规划；《分阶段开发计划.md》Phase 9；《.agent/development-guidelines.md》；Phase 4～8 已落地代码和数据契约。
>
> 计划日期：2026-08-23
>
> 状态：**核心代码已实施，待联调与验收**
>
> 前置假设：Phase 0～8 的核心事实链路已经完成。Java 是用户数据、报告快照和内容草稿的唯一写入方；Python 只负责受控的 AI 文案生成；小程序只负责选择、展示、编辑、复制和用户确认，不负责自行计算报告结论。

Phase 9 的核心不是把几张表拼成一篇文章，而是把已有的每日状态、体重、喂食、提醒、医疗、健康预警、食品、开支、事件和媒体事实，组织成可追溯的周期快照，再基于快照生成高密度报告和可编辑分享草稿。所有 AI 文案都必须能回指输入事实，不能把模型推测写回宠物事实表。

---

## 1. 阶段目标与完成定义

### 1.1 用户目标

| 能力 | 用户结果 | 主要事实来源 |
|---|---|---|
| 周期报告 | 查看某只宠物一周、一个月、一个季度或一年的记录摘要 | Phase 2～8 事实表 |
| 月报 | 看到本月概览、变化、关注项、下月提醒和代表照片 | 事实快照 + 受控 AI 总结 |
| 年报 | 看到全年数据价值、费用结构、成长记忆和年度变化 | 事实快照 + 受控 AI 总结 |
| 分享笔记 | 选择主题和照片，生成可编辑的图文草稿 | 事件、媒体、宠物档案、报告事实 |
| 小红书文案 | 生成标题、正文、Emoji、推荐话题和图片顺序 | 用户主题 + 已选事实/媒体 |
| 周期生成 | 按用户设置的周期生成待确认草稿 | `content_schedule` + 配额 |
| 数据追溯 | 报告中的数字和结论能看到来源日期或事实项 | `sourceKeys` + 事实快照 |

### 1.2 本阶段完成定义

以下链路在开发环境跑通，并通过自动化测试、迁移检查、会员配额验证和小程序手工验收后，才认为 Phase 9 核心完成：

```text
用户登录
  → 选择宠物和报告周期
  → Java 按固定日期边界读取已有事实
  → 生成不可变 ReportFactSnapshot
  → 保存报告事实快照
  → 校验并获取“报告/分享内容”月度配额
  → Python 基于快照生成结构化 AI 内容
  → Java 校验 sourceKeys、字段长度、免责声明和敏感内容
  → 用户查看报告、选择照片、编辑分享草稿
  → 用户复制/保存到本地，FaFa 不代为发布
```

### 1.3 报告周期规则

首版统一使用 `Asia/Shanghai` 业务时区和 `LocalDate`，不使用 UTC 截断日期：

| 周期 | `periodType` | 日期边界 | 默认使用场景 |
|---|---|---|---|
| 周报 | `week` | 周一至周日 | 看最近发生了什么 |
| 月报 | `month` | 自然月 1 日至月末 | 主推报告 |
| 季度报告 | `quarter` | 1～3、4～6、7～9、10～12 月 | 看长期趋势 |
| 年报 | `year` | 1 月 1 日至 12 月 31 日 | 数据价值、情绪价值和分享 |

请求以 `anchorDate` 指定所属周期。当前周期的结束日期必须截断到今天，报告返回 `dataStatus=partial`；历史完整周期返回 `dataStatus=complete`。未来日期不进入事实快照。

### 1.4 本阶段不做

- 不接入小红书、抖音或其他平台的自动发布 API。
- 不生成公开链接、不做社区、点赞、评论、关注和内容分发。
- 不做 PDF、长图海报、视频剪辑、配乐和字幕渲染；只返回结构化文本、媒体顺序和基础展示数据。
- 不实现预算建议、消费预测、医疗诊断、处方建议或健康评分替代兽医判断。
- 不让 AI 直接修改每日状态、体重、喂食、医疗、费用或媒体归属。
- 不让模型从原始账单、诊断文本或用户备注中自行推断新的事实；所有数字必须来自 Java 快照。
- 不实现多语言内容生成；首版固定简体中文 `zh-CN`。
- 不引入新的图表库；报告图表复用小程序原生 Canvas 或现有统计展示组件。

---

## 2. 产品规则落地

### 2.1 报告内容结构

报告分为“事实层”和“表达层”，两层必须分别保存：

#### 事实层 `ReportFactSnapshot`

事实层由 Java 生成，保存报告生成时的可审计快照：

```json
{
  "snapshotVersion": "v1",
  "pet": {
    "petId": 1,
    "name": "豆包",
    "type": "cat",
    "breed": "英短"
  },
  "period": {
    "type": "month",
    "startDate": "2026-08-01",
    "endDate": "2026-08-23",
    "dataStatus": "partial"
  },
  "coverage": {
    "calendarDays": 23,
    "dailyStatusRecordedDays": 18,
    "weightRecordCount": 4,
    "feedingRecordCount": 42,
    "reminderCompletedCount": 6,
    "mediaCount": 12,
    "eventCount": 3,
    "medicalVisitCount": 1,
    "expenseCount": 4
  },
  "weight": {
    "firstKg": 4.20,
    "lastKg": 4.28,
    "changeKg": 0.08,
    "sampleCount": 4
  },
  "dailyStatus": {
    "appetite": {"normalDays": 16, "refusalDays": 1},
    "energy": {"normalDays": 17, "lowDays": 1},
    "waterIntake": {"normalDays": 15, "abnormalDays": 2},
    "defecation": {"normalDays": 16, "abnormalDays": 2}
  },
  "feeding": {
    "days": 20,
    "recordCount": 42,
    "averageDailyAmount": 76.40,
    "unit": "g"
  },
  "reminders": {
    "completedCount": 6,
    "missedCount": 1,
    "completionRate": 85.71
  },
  "medical": {
    "visitCount": 1,
    "medicationRecordCount": 8,
    "openAlertCount": 1
  },
  "expenses": {
    "currency": "CNY",
    "totalAmount": 499.90,
    "categoryBreakdown": [
      {"category":"food","amount":299.90,"percentage":60.00},
      {"category":"medical","amount":200.00,"percentage":40.00}
    ]
  },
  "events": [
    {"key":"event:9001","date":"2026-08-12","title":"第一次去公园","type":"milestone"}
  ],
  "media": [
    {"key":"media:201","mediaId":201,"takenAt":"2026-08-08T10:20:00","tags":["阳台","晒太阳"]}
  ],
  "limitations": [
    "每日状态有5天未记录",
    "当前月份尚未结束"
  ]
}
```

事实快照只保存生成报告所需的聚合值、来源 ID、日期和展示安全字段，不复制完整诊断原文、处方原文、票据 URL、对象存储签名 URL 或用户 token。

#### 表达层 `ReportContent`

表达层由 Python 生成、Java 校验后保存：

```json
{
  "language": "zh-CN",
  "headline": "豆包的八月记录",
  "overview": "本周期共记录18天，整体记录以正常状态为主。",
  "dataChanges": [
    {
      "text": "体重从4.20kg变化到4.28kg，增加0.08kg。",
      "sourceKeys": ["weight.firstKg", "weight.lastKg", "weight.changeKg"]
    }
  ],
  "attentionItems": [
    {
      "text": "本周期有1次拒食记录，建议结合原始记录继续观察。",
      "sourceKeys": ["dailyStatus.appetite.refusalDays"],
      "severity": "info"
    }
  ],
  "nextPeriodReminders": [
    {
      "text": "下个周期可以继续记录饮水和排便情况。",
      "sourceKeys": ["dailyStatus.waterIntake", "dailyStatus.defecation"]
    }
  ],
  "limitations": ["以上内容只基于 FaFa 中已记录的数据，不代表未记录的日期没有发生相关事件。"],
  "disclaimer": "以上内容仅整理已记录事实，不构成诊断、处方或治疗建议。"
}
```

### 2.2 月报内容口径

月报必须覆盖产品设计中规定的内容，但数据不足时必须明确“未记录”，不能补齐或猜测：

1. **本月概览**：记录天数、照片/视频数、喂食次数、体重记录数、提醒完成数。
2. **体重变化**：首条、末条、变化值、采样数；采样数小于 2 时不生成趋势结论。
3. **饮食**：喂食天数、次数、平均每日量、食品卡/计划事实；不从食品卡价格推导消费。
4. **饮水**：每日状态中已记录的正常/异常天数；不做饮水疾病判断。
5. **排便**：每日状态中已记录的正常/异常天数；不做诊断判断。
6. **健康事件**：医疗就诊、实际用药、健康预警和用户事件的事实摘要；不复制诊断和处方原文。
7. **成长记忆**：最多 9 个代表媒体和最多 3 个代表事件，均带来源 ID 和日期。
8. **本月总体状态**：一句话，不得超过 120 个汉字。
9. **数据变化**：2～4 条，每条必须携带 `sourceKeys`。
10. **值得关注**：最多 3 条，只能描述记录到的变化或缺失，不得输出诊断结论。
11. **下月提醒**：最多 3 条，只能是记录建议或已存在提醒，不得创建新提醒。

### 2.3 分享笔记与内容格式

首版统一使用 `contentType` 区分内容格式：

| `contentType` | 用途 | 必须返回字段 |
|---|---|---|
| `share_note` | FaFa 内部可复制的图文笔记 | 标题、正文、Emoji、图片顺序、免责声明 |
| `xiaohongshu` | 小红书发布前的文案草稿 | 标题、正文、Emoji、推荐话题、图片顺序、免责声明 |
| `short_video` | 短视频发布前的文字脚本 | 标题、开场、正文/字幕、结尾、话题、图片/视频顺序 |

Phase 9 页面优先开放 `share_note` 和 `xiaohongshu`；`short_video` 只完成后端结构化契约和基础生成，视频编辑、字幕渲染留到后续阶段。

支持的 `topicType`：

```text
growth_record / birthday / first_home / monthly_summary / travel /
daily_funny / memorial_day / custom
```

用户自定义主题最多 100 个汉字；用户备注、事件内容和自定义主题全部视为不可信数据，进入 prompt 时必须包裹为“资料内容”，不能当作模型指令执行。

### 2.4 会员配额

产品设计规定普通用户每月 3 次、会员每月 10 次“报告/分享笔记生成”。Phase 9 采用共享配额，报告生成、重新生成和分享笔记生成共用一个 Redis 月度计数：

```text
fafa:quota:report-content:{userId}:{yyyy-MM}
```

规则：

- 查询已经完成的同周期报告不消耗配额。
- 首次生成、用户主动重新生成、每一次新的分享笔记生成消耗 1 次。
- Python 超时、返回格式无效或 Java 校验失败时释放本次配额。
- 周期任务命中已完成报告或已生成草稿时不重复消耗。
- 普通用户上限 3，会员上限 10；会员状态沿用 `User.isVipValid()`。
- 页面展示本月已使用次数和剩余次数；不足时给出温和提示，不改变已保存事实。

---

## 3. 领域模型与边界

### 3.1 领域枚举

建议新增：

```text
com.fafa.domain.model.report.ReportPeriodType
  WEEK / MONTH / QUARTER / YEAR

com.fafa.domain.model.report.ReportStatus
  FACTS_READY / GENERATING / COMPLETED / DEGRADED / FAILED / DELETED

com.fafa.domain.model.report.DataStatus
  COMPLETE / PARTIAL / EMPTY

com.fafa.domain.model.content.ContentType
  SHARE_NOTE / XIAOHONGSHU / SHORT_VIDEO

com.fafa.domain.model.content.ContentDraftStatus
  GENERATING / COMPLETED / DEGRADED / FAILED / DELETED

com.fafa.domain.model.content.TopicType
  GROWTH_RECORD / BIRTHDAY / FIRST_HOME / MONTHLY_SUMMARY /
  TRAVEL / DAILY_FUNNY / MEMORIAL_DAY / CUSTOM

com.fafa.domain.model.content.GenerationTrigger
  MANUAL / SCHEDULED / REGENERATE
```

枚举 code 是数据库和接口契约，禁止在 Controller、Python 路由和小程序页面中散落字符串判断。

### 3.2 `ReportPeriod` 值对象

建议新增：`com.fafa.domain.model.report.ReportPeriod`。

职责：

- 根据 `periodType + anchorDate + ZoneId` 计算闭区间 `startDate/endDate`。
- 当前周期将 `endDate` 截断到 `today`，并计算 `DataStatus`。
- 拒绝未来锚点、开始日期晚于结束日期和超过配置最大天数的查询。
- 提供 `periodKey`，例如 `month:2026-08`、`quarter:2026-Q3`，用于幂等键和日志。
- 不依赖 Spring、MyBatis 或当前登录态，可单独测试周/月/季度/年边界。

默认规则：周一为周起点；季度按自然季度；年份按自然年；业务时区由 `fafa.report.time-zone` 配置，默认 `Asia/Shanghai`。

### 3.3 `PetReport` 聚合根

建议新增：`com.fafa.domain.model.report.PetReport`。

聚合字段：

- `id/userId/petId`
- `period: ReportPeriod`
- `status/dataStatus`
- `snapshotVersion/calculationVersion`
- `factSnapshotJson`（持久化边界字段，领域层通过不可变快照对象访问）
- `contentJson`
- `contentMode`：`ai` 或 `deterministic`
- `modelName/promptVersion`
- `generationTrigger/generationVersion`
- `lastError`
- `generatedAt/createdAt/updatedAt`
- `deleted`

领域行为：

- `markFactsReady(snapshot)`：事实快照保存后进入可生成状态。
- `startGeneration()`：只允许从 `FACTS_READY`、`FAILED` 或 `DEGRADED` 进入生成中。
- `completeWithAiContent(content, metadata)`：保存已通过 Java 校验的结构化内容。
- `completeWithDeterministicContent(content)`：AI 不可用时保存事实型降级内容。
- `fail(errorCode)`：保存安全错误摘要，不把 Python 原始响应写入用户响应。
- `requestRegeneration()`：增加生成版本，保留同一周期唯一记录，不重复创建报告行。
- `delete()`：逻辑删除报告，不能删除源事实和媒体。

报告聚合不拥有每日状态、体重、医疗、账单和媒体实体，只保存生成时的快照和来源 ID。

### 3.4 `ContentDraft` 聚合根

建议新增：`com.fafa.domain.model.content.ContentDraft`。

聚合字段：

- `id/userId/petId`
- `contentType/topicType/customTopic`
- `period: ReportPeriod?`（可选；自定义主题可以没有周期）
- `selectedMediaIds`
- `factSnapshotJson`
- `draftJson`
- `status/contentMode`
- `generationTrigger/idempotencyKey/generationVersion`
- `modelName/promptVersion/lastError`
- `createdAt/updatedAt/deleted`

领域行为：

- `startGeneration()`、`complete(draft)`、`completeDegraded(draft)`、`fail(error)`。
- `editByUser(title, body, hashtags, mediaOrder)`：只允许编辑展示层字段，不能改事实快照。
- `regenerate()`：保留草稿 ID 和来源主题，递增版本。
- `delete()`：逻辑删除草稿，不影响报告和媒体。

### 3.5 周期任务边界

产品支持分享笔记周期生成，首版只做轻量调度，不做复杂工作流：

- `ContentSchedule` 保存某用户某宠物的内容格式、主题、间隔天数、下次执行时间和启用状态。
- 默认 `intervalDays=15`，用户可修改为 7、15、30 天；范围由配置限制。
- 定时器只创建或更新待生成草稿，不发布、不直接修改事实。
- 同一 `userId + petId + contentType` 只保留一条启用中的周期配置。
- 调度任务按批次扫描到期配置，单次批量上限配置化，避免一次生成压垮 Python。
- 配额不足或 AI 失败时记录原因并推进下次执行时间，不能每分钟重复重试。

---

## 4. 事实快照组装方案

### 4.1 Java 事实组装职责

新增应用/领域服务建议：

```text
ReportPeriodService
  - 计算周期边界

ReportFactAssembler
  - 按 userId + petId + period 读取所有事实
  - 生成固定结构 ReportFactSnapshot

ReportFactPolicy
  - 限制最大媒体数、事件数、每日明细数
  - 脱敏医疗、票据和私密文本
  - 确定缺失数据和 dataStatus

ReportContentValidator
  - 校验 AI 字段、长度、枚举、sourceKeys 和免责声明

ReportApplicationService
  - 编排归属校验、快照、配额、网关、保存和重试
```

应用服务只协调，不把跨表统计规则散落到 Controller；可复用的计数、周期、百分比和趋势口径放在 domain service。

### 4.2 数据来源映射

| 报告事实 | 来源 | 读取方式 | 注意事项 |
|---|---|---|---|
| 宠物资料 | `pet` | 归属校验后读取 | 只返回名称、类型、品种等展示安全字段 |
| 每日状态 | `daily_status` | 日期范围查询 | 统计已记录天数，不把缺失当正常 |
| 体重 | `weight_record` | 日期范围首末排序 | 样本不足时不生成趋势结论 |
| 喂食 | `feeding_record` | 日期范围聚合 | 沿用 Phase 7 食品名称快照和计划关联 |
| 提醒 | `reminder_completion_history` | user/pet/时间范围 | 完成和漏记分开表达 |
| 医疗 | `medical_visit`、`medication_record` | 只读事实汇总 | 不复制诊断原文、处方原文 |
| 健康预警 | `health_alert` | 读取状态和规则标题 | 明确是规则观察，不写成诊断 |
| 食品/喂养 | Phase 7 表和分析服务 | 复用既有事实统计 | 不把食品卡价格当消费 |
| 开支 | Phase 8 `/internal/ai/**` 或 domain repository | 复用 `calculationVersion` | 不在 Phase 9 重写费用统计 |
| 事件 | `event_record` | 日期范围查询 | 只保留标题、日期、类型和来源 ID |
| 媒体 | `media_asset` | 归属、日期、状态过滤 | 最多选 9 张代表图，不保存签名 URL |

### 4.3 报告数据口径

- 所有计数按闭区间 `[startDate, endDate]`。
- 逻辑删除、跨用户和跨宠物数据不进入快照。
- 事件和媒体按 `eventDate/takenAt` 排序，使用稳定 ID 作为第二排序键。
- 代表媒体选择规则：用户手动选择优先；没有手选时按 `takenAt`、AI 标签丰富度、确认归属和 ID 稳定排序。
- 每份报告最多保存 9 个媒体引用、20 个事件引用、366 个每日趋势点；年报趋势使用月聚合，不保存全年每条原始记录。
- 统计数字全部由 Java 计算，Python 不能重新计算总额、百分比、变化值和完成率。
- 事实快照保存 `calculationVersion`，Phase 8 费用统计版本变更后，新报告使用新版本，旧报告仍按旧快照展示。

### 4.4 事实缺失和降级

| 场景 | Java 行为 | 用户展示 |
|---|---|---|
| 无任何记录 | 保存空快照 | “这个周期还没有已记录数据” |
| 有记录但某维度为空 | 快照保留 `null`/0 和 coverage | “该周期暂无饮水记录” |
| Python 不可用 | 事实快照仍保存，生成确定性报告 | 显示“AI 总结暂不可用，以下为已记录数据” |
| AI 输出非法 | 丢弃非法表达层，保留快照 | 可重试，不能展示未经校验的内容 |
| 统计源服务失败 | 报告进入 `FAILED`，不生成猜测 | 显示失败原因和重试按钮 |
| 当前周期未结束 | `dataStatus=partial` | 明确“当前周期进行中” |

---

## 5. V9 数据库设计

### 5.1 迁移文件

新增：

```text
fafa-java/src/main/resources/db/migration/V9__create_report_and_content_tables.sql
```

建议迁移创建三张表：`pet_report`、`content_draft`、`content_schedule`。不建立跨表物理外键，沿用项目现有逻辑删除和应用层归属校验策略。

### 5.2 `pet_report`

```sql
CREATE TABLE IF NOT EXISTS `pet_report` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '宠物报告ID',
  `user_id` BIGINT NOT NULL COMMENT '所属用户ID',
  `pet_id` BIGINT NOT NULL COMMENT '所属宠物ID',
  `period_type` VARCHAR(16) NOT NULL COMMENT 'week/month/quarter/year',
  `period_start` DATE NOT NULL COMMENT '报告周期开始',
  `period_end` DATE NOT NULL COMMENT '报告周期结束',
  `data_status` VARCHAR(16) NOT NULL DEFAULT 'complete' COMMENT 'complete/partial/empty',
  `status` VARCHAR(16) NOT NULL DEFAULT 'facts_ready' COMMENT 'facts_ready/generating/completed/degraded/failed/deleted',
  `snapshot_version` VARCHAR(32) NOT NULL COMMENT '事实快照版本',
  `calculation_version` VARCHAR(32) DEFAULT NULL COMMENT '统计口径版本',
  `fact_snapshot_json` JSON NOT NULL COMMENT 'Java 生成的事实快照',
  `content_json` JSON DEFAULT NULL COMMENT '通过校验的报告表达层',
  `content_mode` VARCHAR(16) DEFAULT NULL COMMENT 'ai/deterministic',
  `model_name` VARCHAR(100) DEFAULT NULL COMMENT '生成模型',
  `prompt_version` VARCHAR(64) DEFAULT NULL COMMENT '提示词版本',
  `generation_trigger` VARCHAR(16) NOT NULL DEFAULT 'manual' COMMENT 'manual/scheduled/regenerate',
  `generation_version` INT NOT NULL DEFAULT 1 COMMENT '同周期重新生成次数',
  `last_error_code` VARCHAR(64) DEFAULT NULL COMMENT '安全错误码',
  `last_error_message` VARCHAR(500) DEFAULT NULL COMMENT '用户可读错误摘要',
  `generated_at` DATETIME DEFAULT NULL COMMENT '完成时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pet_report_period` (`user_id`, `pet_id`, `period_type`, `period_start`, `period_end`),
  KEY `idx_pet_report_user_pet_time` (`user_id`, `pet_id`, `period_end`, `is_deleted`),
  KEY `idx_pet_report_status_updated` (`status`, `updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='宠物周期报告';
```

唯一键保证同一用户、宠物和周期只有一份报告；重新生成更新同一行并递增 `generation_version`，避免历史页面出现重复报告。

### 5.3 `content_draft`

```sql
CREATE TABLE IF NOT EXISTS `content_draft` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '内容草稿ID',
  `user_id` BIGINT NOT NULL COMMENT '所属用户ID',
  `pet_id` BIGINT NOT NULL COMMENT '所属宠物ID',
  `content_type` VARCHAR(24) NOT NULL COMMENT 'share_note/xiaohongshu/short_video',
  `topic_type` VARCHAR(32) NOT NULL COMMENT 'growth_record/birthday/...',
  `custom_topic` VARCHAR(100) DEFAULT NULL COMMENT '用户自定义主题',
  `period_type` VARCHAR(16) DEFAULT NULL COMMENT '可选报告周期',
  `period_start` DATE DEFAULT NULL,
  `period_end` DATE DEFAULT NULL,
  `selected_media_json` JSON DEFAULT NULL COMMENT '用户选择的媒体ID和顺序',
  `fact_snapshot_json` JSON NOT NULL COMMENT '生成时的事实快照',
  `draft_json` JSON DEFAULT NULL COMMENT '标题/正文/话题等结构化草稿',
  `status` VARCHAR(16) NOT NULL DEFAULT 'generating' COMMENT 'generating/completed/degraded/failed/deleted',
  `content_mode` VARCHAR(16) DEFAULT NULL COMMENT 'ai/deterministic',
  `generation_trigger` VARCHAR(16) NOT NULL DEFAULT 'manual',
  `idempotency_key` VARCHAR(100) NOT NULL COMMENT '客户端/调度幂等键',
  `generation_version` INT NOT NULL DEFAULT 1,
  `model_name` VARCHAR(100) DEFAULT NULL,
  `prompt_version` VARCHAR(64) DEFAULT NULL,
  `last_error_code` VARCHAR(64) DEFAULT NULL,
  `last_error_message` VARCHAR(500) DEFAULT NULL,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_content_draft_idempotency` (`user_id`, `idempotency_key`),
  KEY `idx_content_draft_user_pet_time` (`user_id`, `pet_id`, `updated_at`, `is_deleted`),
  KEY `idx_content_draft_pet_type` (`pet_id`, `content_type`, `status`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='宠物内容创作草稿';
```

用户编辑只修改 `draft_json`，不覆盖 `fact_snapshot_json`；这样可以区分“原始事实”“AI 初稿”和“用户最终编辑稿”。

### 5.4 `content_schedule`

```sql
CREATE TABLE IF NOT EXISTS `content_schedule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '内容周期配置ID',
  `user_id` BIGINT NOT NULL,
  `pet_id` BIGINT NOT NULL,
  `content_type` VARCHAR(24) NOT NULL COMMENT 'share_note/xiaohongshu',
  `topic_type` VARCHAR(32) NOT NULL DEFAULT 'monthly_summary',
  `interval_days` INT NOT NULL DEFAULT 15 COMMENT '7/15/30',
  `next_run_at` DATETIME NOT NULL,
  `last_run_at` DATETIME DEFAULT NULL,
  `enabled` TINYINT NOT NULL DEFAULT 1,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_content_schedule_user_pet_type` (`user_id`, `pet_id`, `content_type`, `is_deleted`),
  KEY `idx_content_schedule_due` (`enabled`, `next_run_at`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='宠物内容周期生成配置';
```

调度表只保存配置和运行时间，不保存 AI 内容；内容统一写入 `content_draft`。

### 5.5 数据清理与保留

- 报告、草稿和周期配置均逻辑删除，默认查询带 `is_deleted=0`。
- 用户删除宠物时，报告和草稿逻辑删除，媒体和原始事实沿用各自模块的删除策略。
- 报告快照默认长期保留，便于用户回看；后续如需清理，必须按用户可见规则和审计策略执行。
- 失败记录只保存安全错误码和摘要，不保存完整 prompt、完整模型响应或签名 URL。

---

## 6. Java 后端实现方案

### 6.1 领域层目录

```text
fafa-java/src/main/java/com/fafa/domain/
├── model/report/
│   ├── ReportPeriod.java
│   ├── ReportPeriodType.java
│   ├── ReportStatus.java
│   ├── DataStatus.java
│   └── PetReport.java
├── model/content/
│   ├── ContentType.java
│   ├── ContentDraftStatus.java
│   ├── TopicType.java
│   ├── GenerationTrigger.java
│   └── ContentDraft.java
├── repository/
│   ├── PetReportRepository.java
│   ├── ContentDraftRepository.java
│   └── ContentScheduleRepository.java
└── service/report/
    ├── ReportPeriodService.java
    ├── ReportFactAssembler.java
    ├── ReportContentValidator.java
    └── ContentSelectionPolicy.java
```

领域层不依赖 Spring、MyBatis、Jackson、HTTP、Python 和小程序。

### 6.2 应用层目录

```text
fafa-java/src/main/java/com/fafa/application/
├── dto/report/
│   ├── GenerateReportCommand.java
│   ├── ReportDTO.java
│   ├── ReportFactDTO.java
│   └── ReportPageDTO.java
├── dto/content/
│   ├── CreateContentDraftCommand.java
│   ├── EditContentDraftCommand.java
│   ├── ContentDraftDTO.java
│   └── ContentScheduleDTO.java
├── port/
│   ├── ReportGenerationGateway.java
│   └── ContentGenerationGateway.java
└── service/
    ├── ReportApplicationService.java
    ├── ContentDraftApplicationService.java
    ├── ContentScheduleApplicationService.java
    └── ReportQuotaApplicationService.java
```

职责边界：

- `ReportApplicationService`：周期、归属、快照、配额、AI 网关、报告查询和重试。
- `ContentDraftApplicationService`：主题、媒体选择、内容生成、用户编辑和草稿查询。
- `ContentScheduleApplicationService`：周期设置、启停、下次执行时间和调度幂等。
- `ReportQuotaApplicationService`：封装报告/分享内容共享配额，不把 Redis key 散落到各业务服务。
- `ReportGenerationGateway`：只表达“用事实快照生成报告”，不读取数据库。
- `ContentGenerationGateway`：只表达“用事实快照和主题生成草稿”，不具备写 Java 数据的能力。

### 6.3 基础设施层目录

```text
fafa-java/src/main/java/com/fafa/infrastructure/
├── persistence/
│   ├── mapper/PetReportMapper.java
│   ├── mapper/ContentDraftMapper.java
│   ├── mapper/ContentScheduleMapper.java
│   ├── po/PetReportPO.java
│   ├── po/ContentDraftPO.java
│   ├── po/ContentSchedulePO.java
│   └── repository/*RepositoryImpl.java
├── ai/impl/
│   ├── HttpReportGenerationGateway.java
│   └── HttpContentGenerationGateway.java
└── scheduler/
    └── ContentScheduleScanner.java
```

开发环境复用现有 HTTP 内网链路；生产环境先保持 HTTP + 超时/重试/熔断边界，待任务量证实后再接 RocketMQ，不在 Phase 9 预先引入通用工作流引擎。

### 6.4 错误码规划

在 `ErrorCode` 增加 `71xxx` 报告与创作段：

```text
REPORT_NOT_FOUND(71001, "报告不存在")
REPORT_PERIOD_INVALID(71002, "报告周期无效")
REPORT_GENERATION_QUOTA_EXCEEDED(71003, "本月报告与分享内容生成次数已用完")
REPORT_GENERATION_IN_PROGRESS(71004, "报告正在生成，请稍后查看")
REPORT_GENERATION_FAILED(71005, "报告生成失败，请稍后重试")
REPORT_CONTENT_INVALID(71006, "报告内容校验失败")
REPORT_SOURCE_UNAVAILABLE(71007, "报告所需事实暂时不可用")
CONTENT_DRAFT_NOT_FOUND(71008, "内容草稿不存在")
CONTENT_DRAFT_FIELD_INVALID(71009, "内容草稿参数无效")
CONTENT_DRAFT_IMMUTABLE(71010, "当前草稿状态不可编辑")
CONTENT_SCHEDULE_NOT_FOUND(71011, "周期生成配置不存在")
CONTENT_SCHEDULE_INVALID(71012, "周期生成配置无效")
CONTENT_MEDIA_INVALID(71013, "选中的媒体无效或无权访问")
```

AI 原始异常统一映射到安全错误码；日志保留 `traceId/userId/petId/reportId/draftId`，不记录完整 prompt 和完整模型响应。

### 6.5 配置规划

在 `FaFaProperties` 增加：

```java
@Data
public static class Report {
    private String timeZone = "Asia/Shanghai";
    private int maxRangeDays = 3660;
    private int maxMediaCount = 9;
    private int maxEventCount = 20;
    private int maxDailyPoints = 366;
    private int generationTimeoutSeconds = 60;
    private int maxTopicLength = 100;
    private int maxContentLength = 12000;
    private String snapshotVersion = "v1";
    private String promptVersion = "report-v1";
    private boolean scheduleEnabled = true;
    private int scheduleBatchSize = 20;
}
```

在 `FaFaProperties.Quota` 增加：

```java
@Data
public static class ReportContentQuota {
    private int monthlyNormal = 3;
    private int monthlyVip = 10;
}
```

配置进入 `application-dev.yml`、`application-test.yml` 和 `application-prod.yml`，生产值通过环境变量注入。Java 业务代码不得读取环境变量，也不得根据 profile 写分支。

---

## 7. 公开接口设计

所有公开接口默认地址为 `http://localhost:8080`，统一携带登录态 `Authorization`，返回既有 `Result<T>` 结构。接口只接受 `petId`，不接受请求体中的 `userId`；用户归属从 Sa-Token 登录态读取。

### 7.1 生成或查询报告

#### 生成报告

- 定义：`POST`
- 地址：`/api/reports/generate`
- 请求：

```json
{
  "petId": 1,
  "periodType": "month",
  "anchorDate": "2026-08-23",
  "includeMedia": true,
  "forceRegenerate": false,
  "idempotencyKey": "miniapp-report-2026-08-pet-1"
}
```

规则：

- 同一周期已有 `completed/degraded` 报告且 `forceRegenerate=false` 时直接返回，不消耗配额。
- 已有 `generating` 报告时返回 `REPORT_GENERATION_IN_PROGRESS` 和报告 ID。
- 首次生成或强制重新生成时先保存事实快照，再获取共享配额。
- 当前周期允许生成 `partial` 报告；未来日期不纳入。

响应：

```json
{
  "code": 0,
  "message": "成功",
  "data": {
    "id": 901,
    "petId": 1,
    "periodType": "month",
    "periodStart": "2026-08-01",
    "periodEnd": "2026-08-23",
    "dataStatus": "partial",
    "status": "completed",
    "contentMode": "ai",
    "headline": "豆包的八月记录",
    "overview": "本周期共记录18天，整体记录以正常状态为主。",
    "dataChanges": [],
    "attentionItems": [],
    "nextPeriodReminders": [],
    "limitations": ["当前月份尚未结束"],
    "media": [{"mediaId": 201, "takenAt": "2026-08-08T10:20:00", "url": null}],
    "calculationVersion": "v1",
    "snapshotVersion": "v1",
    "generationVersion": 1,
    "generatedAt": "2026-08-23T16:00:00"
  }
}
```

`url` 不写入数据库；需要展示时由 Java 依据当前用户权限生成短期访问地址或复用现有媒体 DTO。

#### 报告列表

- 定义：`GET`
- 地址：`/api/reports?petId=1&periodType=month&page=1&size=20`
- 响应：分页返回报告摘要、周期、状态、数据完整度、生成时间和是否存在可用内容。

#### 报告详情

- 定义：`GET`
- 地址：`/api/reports/901`
- 响应：返回报告内容、事实概览、来源引用、媒体和 limitations。
- 详情接口每次校验报告归属；不能通过 ID 读取其他用户报告。

#### 重新生成

- 定义：`POST`
- 地址：`/api/reports/901/regenerate`
- 请求：`{"idempotencyKey":"regenerate-report-901-v2"}`
- 行为：递增 `generationVersion`，更新同一周期报告；成功前保留旧内容，失败时旧内容仍可查看。

#### 删除报告

- 定义：`DELETE`
- 地址：`/api/reports/901`
- 行为：逻辑删除报告，不删除源记录、事件、账单和媒体；删除后不进入默认列表。

### 7.2 内容草稿接口

#### 生成分享笔记/小红书文案

- 定义：`POST`
- 地址：`/api/content-drafts`
- 请求：

```json
{
  "petId": 1,
  "contentType": "xiaohongshu",
  "topicType": "birthday",
  "customTopic": "豆包三岁生日的一天",
  "periodType": "year",
  "anchorDate": "2026-08-23",
  "selectedMediaIds": [201, 202, 203],
  "tone": "warm",
  "idempotencyKey": "birthday-pet-1-2026-v1"
}
```

响应：

```json
{
  "code": 0,
  "message": "成功",
  "data": {
    "id": 902,
    "petId": 1,
    "contentType": "xiaohongshu",
    "topicType": "birthday",
    "status": "completed",
    "contentMode": "ai",
    "title": "豆包三岁啦：把平凡日子过成了纪念日",
    "body": "今天想记录一下豆包三岁生日这一天……",
    "emojis": ["🐾", "🎂"],
    "hashtags": ["#养宠日常", "#猫咪生日"],
    "mediaOrder": [201, 202, 203],
    "disclaimer": "内容由 FaFa 根据已记录资料生成，请发布前自行确认。",
    "sourceKeys": ["pet.name", "media:201", "event:9001"],
    "generationVersion": 1
  }
}
```

#### 草稿列表和详情

```text
GET /api/content-drafts?petId=1&contentType=xiaohongshu&page=1&size=20
GET /api/content-drafts/902
```

列表默认只返回未删除草稿，按 `updatedAt` 倒序；详情返回事实引用和用户当前编辑内容。

#### 编辑草稿

- 定义：`PUT`
- 地址：`/api/content-drafts/902`
- 请求：

```json
{
  "title": "豆包三岁生日记录",
  "body": "这是我和豆包一起度过的第三个生日……",
  "emojis": ["🐾", "🎂"],
  "hashtags": ["#养宠日常", "#猫咪生日"],
  "mediaOrder": [202, 201, 203]
}
```

只更新草稿展示层，不修改 `sourceKeys`、事实快照、宠物资料和媒体归属。标题、正文、话题和媒体数量必须再次经过 Java 长度与归属校验。

#### 重新生成和删除草稿

```text
POST   /api/content-drafts/902/regenerate
DELETE /api/content-drafts/902
```

重新生成消耗一次共享配额；删除为逻辑删除且不删除媒体。

### 7.3 周期生成配置

#### 查询配置

```text
GET /api/content-schedules?petId=1
```

#### 创建或更新配置

- 定义：`PUT`
- 地址：`/api/content-schedules`
- 请求：

```json
{
  "petId": 1,
  "contentType": "share_note",
  "topicType": "monthly_summary",
  "intervalDays": 15,
  "enabled": true
}
```

`intervalDays` 首版只支持 7、15、30；周期任务生成的是待确认草稿，不自动发布。

#### 启停和删除

```text
POST   /api/content-schedules/701/enable
POST   /api/content-schedules/701/disable
DELETE /api/content-schedules/701
```

### 7.4 会员配额接口扩展

扩展既有 `/api/quota` 响应：

```json
{
  "reportContentUsed": 1,
  "reportContentLimit": 3,
  "isVip": false
}
```

不新增前端自算逻辑，所有剩余次数由 Java 配额服务返回。

---

## 8. Java 与 Python 服务契约

### 8.1 Java → Python 报告生成

- 地址：`POST /api/reports/generate`
- 请求头：`X-Internal-Token`、`X-Trace-Id`
- Java 传递事实快照，不传 `userId` 作为 Python 授权依据；Python 只把 `userId` 当审计字段。

```json
{
  "requestId": "report-901-v1",
  "petId": 1,
  "periodType": "month",
  "language": "zh-CN",
  "promptVersion": "report-v1",
  "factSnapshot": {"snapshotVersion":"v1","pet":{"name":"豆包"},"coverage":{"weightRecordCount":4}},
  "traceId": "trace-report-901"
}
```

Python 返回：

```json
{
  "requestId": "report-901-v1",
  "contentMode": "ai",
  "modelName": "qwen-plus",
  "promptVersion": "report-v1",
  "content": {
    "headline": "豆包的八月记录",
    "overview": "本周期共记录18天，整体记录以正常状态为主。",
    "dataChanges": [],
    "attentionItems": [],
    "nextPeriodReminders": [],
    "limitations": [],
    "disclaimer": "以上内容仅整理已记录事实，不构成诊断、处方或治疗建议。"
  }
}
```

### 8.2 Java → Python 内容生成

- 地址：`POST /api/content/generate`
- 请求头：`X-Internal-Token`、`X-Trace-Id`
- Python 只依据 Java 传入的主题、事实快照和媒体元数据生成，不访问 MySQL。
- 原始用户主题与备注使用明确的数据边界标记，防止 prompt injection。

### 8.3 Python 端文件规划

```text
fafa-python/app/
├── api/
│   ├── reports.py
│   └── content.py
├── schemas/
│   ├── reports.py
│   └── content.py
├── service/
│   ├── report_generation_service.py
│   └── content_generation_service.py
└── agent/
    ├── intent.py          # 增加 report/content 意图
    ├── graph.py           # 固定工具/生成路由
    └── prompts.py         # report-v1/content-v1
```

Python 生成服务职责：

1. Pydantic 校验 Java 请求结构和字段上限。
2. 读取已传入的 `factSnapshot`，不查询数据库。
3. 通过 `Purpose.CHAT_DEEP` 获取模型，模型名来自配置。
4. 使用结构化 prompt 请求 JSON；响应解析失败时返回明确错误，不返回自由文本让 Java 猜结构。
5. 输出 Pydantic response，限制字段数量、长度、枚举和数组大小。
6. 记录 `traceId/requestId/modelName/promptVersion`，不记录完整敏感快照。

### 8.4 AI 输出硬校验

Java 在保存前必须检查：

- `sourceKeys` 必须存在于当前事实快照允许的 key 白名单。
- 文案中的数字只能来自事实快照；至少对金额、体重、次数、百分比做格式化后比对。
- 不允许出现“诊断为、确诊、处方、建议用药剂量、保证健康”等医疗越界表达。
- 不允许出现其他宠物名称、用户 token、对象存储 URL、内部接口地址和模型原始错误。
- `headline/overview/body/hashtags` 长度和数组数量必须符合配置。
- `mediaOrder` 必须是用户已选媒体 ID 的子集，且顺序不重复。
- 缺少数据时必须使用受控短语，例如“本周期暂无已记录数据”，不能使用“应该、可能是、推测”。
- 免责声明由 Java 统一追加或覆盖，不完全信任模型返回。

### 8.5 确定性降级内容

报告 AI 失败时仍生成事实型报告：

- `headline`：`{宠物名}的{周期}记录`
- `overview`：根据 `coverage` 拼接记录天数和可用维度，不写趋势推断。
- `dataChanges`：只展示 Java 已计算的首末值、总数和百分比。
- `attentionItems`：只展示数据缺失和已有健康预警标题，不生成新建议。
- `nextPeriodReminders`：只展示既有提醒或“继续记录”，不创建新提醒。
- `contentMode=deterministic`、`status=degraded`，前端展示“AI 总结暂不可用”。

分享笔记和小红书文案没有事实型降级时，返回 `REPORT_GENERATION_FAILED`，保留草稿记录和重试入口，不伪造创作内容。

---

## 9. Agent 对话接入

### 9.1 新增只读工具

在 Python 固定工具注册表中增加：

```text
getPetReports
getReportDetail
```

Agent 可读取已完成报告，但不能直接触发报告生成、重新生成、编辑草稿或修改事实。生成报告和分享笔记使用明确的公开页面/接口，不从普通问答路径隐式消耗会员配额。

### 9.2 意图路由

新增确定性意图：

```text
REPORT_QUERY       “看看豆包这个月的报告”“今年发生了什么”
CONTENT_DRAFT      “帮我写一篇豆包生日的小红书”
```

规则优先级：

1. 医疗安全边界和不支持能力。
2. 明确的报告查询。
3. 明确的内容创作请求，返回创建草稿页面或待确认草稿，不直接写库。
4. 现有数据查询、媒体检索和记录草稿规则。

AI 对话返回：

```json
{
  "answer": "我可以根据已生成的八月报告整理给你，也可以打开报告页面重新生成。",
  "blocks": [
    {"type":"report","reportId":901,"title":"豆包的八月记录"}
  ],
  "citations": [
    {"source_type":"pet_report","source_id":"901"}
  ]
}
```

如果没有报告，Agent 只返回“当前没有已生成报告”，并提供打开报告生成页的 link block；是否消耗配额必须由用户明确确认。

---

## 10. 小程序实施方案

### 10.1 页面规划

```text
pages/report/index       报告列表、周期切换、生成入口
pages/report/detail      报告详情、事实引用、代表媒体
pages/content/create     主题/格式/周期/媒体选择
pages/content/edit       草稿编辑、复制、保存和重新生成
pages/content/schedule   周期生成设置
```

首页增加：

- “查看本月报告”卡片。
- “写一篇分享笔记”快捷入口。
- 当前宠物没有报告时显示“生成第一份报告”，不显示虚假摘要。

### 10.2 报告列表体验

- 周报/月报/季度/年报使用周期 chips，默认月报。
- 状态展示：生成中、已完成、数据降级、生成失败、已删除。
- 当前周期显示“进行中”，历史周期显示完整日期。
- 同一周期只展示一条报告；重新生成后刷新同一卡片。
- 生成前显示本月剩余配额和预计读取范围；无配额时提供会员权益说明。

### 10.3 报告详情体验

页面顺序：

1. 宠物名 + 报告周期 + 数据完整度。
2. 一句话总体状态。
3. 本周期概览卡片。
4. 体重、饮食、饮水、排便和提醒数据卡片。
5. 健康事件和已有预警，带“仅为记录事实”的说明。
6. 分类开支和成长记忆。
7. 数据变化、值得关注、下期记录提示。
8. 代表照片/视频横向列表。
9. “生成分享笔记”按钮。

图表只展示 Java 已返回的点，不在小程序重新计算金额、百分比或趋势。点击数字可跳转对应已有模块的日期范围页面。

### 10.4 内容创作体验

1. 选择格式：图文笔记 / 小红书文案。
2. 选择主题：成长记录、生日、第一次到家、月度总结、旅行、搞笑日常、纪念日、自定义。
3. 默认带入当前报告周期和最多 9 张代表媒体。
4. 用户可取消或调整图片顺序；媒体选择只提交 ID。
5. 生成后进入可编辑页面，不直接调用外部平台。
6. 提供“复制正文”“复制标题”“复制话题”“保存草稿”。
7. 页面固定展示免责声明和“发布前请自行确认”。
8. 生成失败保留主题和选图，点击重试时再次确认配额。

### 10.5 周期设置体验

- 默认关闭自动生成，用户主动开启后默认每 15 天。
- 可选 7/15/30 天，展示下次生成日期。
- 关闭后不删除历史草稿。
- 周期任务只生成待确认草稿；在草稿列表标记“周期生成”。
- 配额不足、用户删除宠物或媒体失效时显示可读原因。

---

## 11. 测试计划

### 11.1 Java 领域单元测试

新增：

```text
ReportPeriodTest
  - 周一/周日边界
  - 月末/年末边界
  - 季度 Q1/Q2/Q3/Q4
  - 当前周期截断
  - 未来日期和超范围拒绝

PetReportTest
  - 状态机
  - 同周期重生成版本递增
  - 失败后重试
  - 已删除报告不可编辑

ContentDraftTest
  - 内容格式枚举
  - 主题长度
  - mediaOrder 子集和去重
  - 用户编辑只改变表达层

ReportContentValidatorTest
  - sourceKeys 白名单
  - 数字越界
  - 医疗越界词
  - URL/token 泄漏
  - 字段超长和免责声明补齐
```

### 11.2 Java 应用服务测试

- 宠物不属于当前用户时，报告、草稿、周期配置全部拒绝。
- 同周期已完成报告不重复消耗配额。
- 强制重新生成消耗一次配额并递增版本。
- Python 成功后保存 AI 内容；失败后释放配额并保留事实快照。
- AI 返回非法 `sourceKeys` 或超长内容时拒绝保存表达层。
- 用户编辑草稿不能修改事实快照、来源 ID 和媒体归属。
- 选中的媒体必须属于当前用户和当前宠物，最多 9 个。
- 周期任务扫描幂等，不重复创建同一内容草稿。
- 配额并发请求不超过普通/VIP上限。
- 删除宠物后的报告、草稿和周期配置均不可再通过公开接口读取。

### 11.3 Repository/迁移测试

- V9 可在已有 V8 数据库执行。
- 同一周期唯一约束生效。
- `is_deleted=1` 的报告、草稿和周期配置不出现在默认查询。
- JSON 字段可保存中文、Emoji、数组和结构化内容。
- 报告列表索引命中 `user_id + pet_id + period_end`。
- 周期扫描索引命中 `enabled + next_run_at + is_deleted`。

### 11.4 Python 测试

- Pydantic 请求/响应字段和长度约束。
- mock 模型返回合法 JSON 时正确映射。
- 模型返回 markdown、空字符串、缺少字段、未知枚举时拒绝。
- prompt 中的用户自定义主题被视为资料而不是指令。
- `sourceKeys` 和免责声明被保留。
- 无模型 key 时报告服务返回确定性降级结构。
- Agent 报告查询只调用只读工具，不触发生成接口。

### 11.5 小程序测试

- `node --check` 检查所有新增页面 JS。
- 无宠物、无数据、配额耗尽、生成中、失败重试、草稿编辑和复制。
- 报告数字不被前端重新计算。
- 重新生成返回同一报告 ID，不出现重复卡片。
- 媒体失效时报告仍可查看文字内容。
- 微信开发者工具验证图片预览、文本复制、页面返回刷新和登录过期处理。

### 11.6 契约与联调测试

1. Java 生成事实快照 → Python 返回报告 JSON → Java 校验并保存。
2. Java 生成分享草稿 → Python 返回内容 → 用户编辑 → Java 保存。
3. Python 不可用 → 报告降级、分享草稿失败、配额正确释放。
4. 关闭 Python → 已有报告和草稿仍可查询。
5. Phase 8 费用统计版本进入报告快照，后续费用数据更新不改变旧报告。
6. 同一请求重复提交 `idempotencyKey`，响应和数据库只保留一条。

---

## 12. 性能、并发和可观测性

### 12.1 查询性能

- 事实组装按模块批量查询，不在循环内逐条查数据库。
- 报告只保存聚合数据和有限来源 ID，不能把全年原始记录全部放进 prompt。
- 年报按月聚合；每日明细最多 366 点。
- 媒体默认最多 9 个，选图和预览不拉取全量媒体表。
- 首版不缓存报告事实；完成后直接读取已保存快照，避免统计口径变化。

### 12.2 并发与幂等

- 报告唯一键 + `status` 防止同周期并发生成。
- 草稿使用 `userId + idempotencyKey` 唯一键。
- 生成期间重复请求返回已有 ID，不重复获取配额。
- 周期扫描使用分布式锁或数据库条件更新时间，避免多实例重复执行。
- 配额获取、外部调用失败释放和报告状态更新必须有明确事务边界；Redis 配额不放进数据库事务假设中。

### 12.3 日志与指标

INFO：报告/草稿请求开始、成功、重生成、用户编辑和周期配置变化。

WARN：配额不足、AI 降级、数据缺失、媒体失效和用户输入被截断。

ERROR：数据库失败、网关不可用、响应解析失败和数据不一致。

日志字段：

```text
traceId, userId, petId, reportId, draftId, periodType,
contentType, generationVersion, modelName, promptVersion, durationMs
```

禁止日志内容：完整事实快照、完整诊断/处方原文、票据 URL、对象存储签名、Authorization、内部 token、完整用户自定义主题。

建议指标：

- `fafa_report_generation_total{status,content_mode}`
- `fafa_report_generation_duration_ms`
- `fafa_content_generation_total{content_type,status}`
- `fafa_report_quota_rejected_total`
- `fafa_report_fact_coverage{dimension}`
- `fafa_schedule_scan_total{status}`

---

## 13. 隐私、安全和合规边界

- 报告和草稿默认私有，所有详情、编辑、删除和媒体预览接口校验 `userId + petId`。
- Python 只收到生成所需的最小快照，不直接访问 MySQL，不保存长期原始事实。
- 医疗报告只展示事实计数、已记录事件和已有预警；不生成诊断、病因、处方和剂量建议。
- 内容生成不公开用户昵称、手机号、城市、token 和对象存储内部地址。
- 生成文案中的金额、体重、次数、日期和百分比必须来自 Java 快照；模型不能增加新的数值。
- 代表媒体只传 ID 和展示元数据，URL 在 Java 响应阶段按权限短期生成。
- 用户自定义主题、备注和事件文本要做长度限制、控制字符处理和 prompt injection 隔离。
- 用户删除草稿只删除内容草稿，不删除源照片；用户删除报告不影响原始事实。
- 分享页面明确“仅生成草稿，不负责发布”，用户复制到外部平台前自行确认。

---

## 14. 分阶段实施顺序

### Phase 9.0：基线冻结与契约确认

- 读取 Phase 8 `expense` 统计接口和 `calculationVersion`。
- 确认医疗摘要、Agent 内部查询、媒体访问和会员配额现有实现。
- 冻结周期边界、快照结构、AI 输出 JSON 和错误码。
- 更新 `docs/接口文档.md` 的 Phase 9 契约草案，不先改业务代码。

### Phase 9.1：领域模型与 V9 迁移

- 实现 `ReportPeriod`、报告/草稿状态枚举和聚合行为。
- 实现 `V9__create_report_and_content_tables.sql`。
- 编写 PO、Mapper、Repository 接口和实现。
- 完成唯一键、逻辑删除、索引和 JSON 字段测试。

### Phase 9.2：事实快照与报告 CRUD

- 实现 `ReportFactAssembler` 和各数据模块读取适配。
- 实现报告生成请求、列表、详情、删除和同周期幂等。
- 完成确定性报告内容和 `dataStatus`。
- 先不接 Python，使用固定测试网关验证 Java 端状态机和快照。

### Phase 9.3：AI 报告生成与会员配额

- 增加报告/分享内容共享配额。
- 增加 Java `ReportGenerationGateway` 和 Python `/api/reports/generate`。
- 实现 Pydantic 结构化响应和 Java `ReportContentValidator`。
- 完成 AI 成功、失败、超时、非法 JSON、配额回滚和降级测试。

### Phase 9.4：分享笔记与小红书文案

- 实现 `ContentDraft` 聚合和草稿 CRUD。
- 实现主题、媒体选择、内容格式、用户编辑和重新生成。
- 接入 Python `/api/content/generate`。
- 完成 no-publish 边界、媒体归属和来源引用校验。

### Phase 9.5：周期生成与 Agent

- 实现 `ContentSchedule`、周期配置接口和扫描任务。
- 使用批次、锁和幂等键控制生成压力。
- 增加 Agent 报告只读工具和生成页面 link block。
- Agent 不直接触发配额消费和写入接口。

### Phase 9.6：小程序页面

- 增加报告列表/详情页、内容创建/编辑页和周期设置页。
- 首页增加月报和分享入口。
- 接入配额展示、空状态、生成中、失败重试、复制和保存。
- 使用现有请求封装和媒体访问能力，不引入图表依赖。

### Phase 9.7：联调、验收与发布

- 执行 Java 全量测试、Python 测试和小程序检查。
- 在真实 MySQL 执行 V9，确认历史 Phase 8 费用和医疗数据可组成报告。
- 启动 Java、Redis、MinIO、Python，完成报告和草稿端到端验证。
- 进行配额并发、越权访问、AI 失败、敏感信息和 prompt injection 验收。
- 同步接口、数据库、交接文档和阶段计划，准备 Phase 10。

---

## 15. 配置示例

### 15.1 Java 开发环境

```yaml
fafa:
  report:
    time-zone: Asia/Shanghai
    max-range-days: 3660
    max-media-count: 9
    max-event-count: 20
    max-daily-points: 366
    generation-timeout-seconds: 60
    max-topic-length: 100
    max-content-length: 12000
    snapshot-version: v1
    prompt-version: report-v1
    schedule-enabled: true
    schedule-batch-size: 20
  quota:
    report-content:
      monthly-normal: 3
      monthly-vip: 10
```

### 15.2 Python 环境变量

```text
REPORT_PROMPT_VERSION=report-v1
CONTENT_PROMPT_VERSION=content-v1
REPORT_MAX_CONTENT_LENGTH=12000
REPORT_MAX_MEDIA_COUNT=9
REPORT_GENERATION_TIMEOUT_SECONDS=60
```

模型仍通过现有 `Purpose.CHAT_DEEP` 和 `model_chat_deep` 配置选择，禁止在报告服务中硬编码模型名。

---

## 16. 风险与处理方案

| 风险 | 影响 | 处理方案 |
|---|---|---|
| AI 编造数字或事件 | 报告失真 | Java 事实快照、sourceKeys、数字白名单和结构化输出校验 |
| 医疗内容越界 | 合规风险 | 聚合医疗事实、统一免责声明、敏感词拦截、禁止处方/诊断 |
| 报告与当前数据不一致 | 用户困惑 | 保存不可变事实快照并展示生成时间和口径版本 |
| 同一周期重复生成 | 配额浪费、页面重复 | 周期唯一键、幂等键和生成状态锁 |
| AI 失败阻塞报告 | 用户无法查看数据 | 先保存事实快照，报告支持 deterministic 降级 |
| 分享草稿自动发布 | 外部不可控风险 | 明确 no-publish，只提供复制/保存 |
| 会员配额并发超发 | 商业权益错误 | Redis 原子计数、失败回滚、幂等请求不重复计数 |
| 全年原始记录塞入 prompt | 超时、费用和隐私 | Java 聚合、按月趋势、限制媒体/事件数量 |
| 用户备注触发 prompt injection | AI 越权输出 | 资料与指令分区、模型输出白名单和 Java 最终校验 |
| 周期任务集中触发 | Python/Redis/数据库压力 | 扫描批次、分布式锁、下次执行时间推进和限流 |
| 媒体已删除或无权访问 | 报告图片加载失败 | 报告文字仍可用，媒体引用按当前权限过滤和降级 |
| 旧统计口径变化 | 历史报告漂移 | 保存 `calculationVersion` 和完整事实快照 |

---

## 17. 验收清单

### 17.1 功能验收

- [ ] 用户可以为当前宠物生成周报、月报、季度报告和年报。
- [ ] 当前周期正确显示 `partial`，历史完整周期显示 `complete`。
- [ ] 月报包含概览、体重、饮食、饮水、排便、健康事件和成长记忆。
- [ ] 报告中的数据变化、关注项和下期提醒符合产品数量限制。
- [ ] 无数据和部分数据时明确显示“暂无已记录数据”，不虚构结论。
- [ ] 同一周期重复打开不重复消耗配额；主动重新生成消耗一次配额。
- [ ] 普通用户每月 3 次、会员每月 10 次共享配额正确生效。
- [ ] AI 失败时报告仍可查看事实型降级内容。
- [ ] 用户可以生成 share note 和 xiaohongshu 草稿。
- [ ] 用户可以选择、排序、编辑和复制媒体与文案。
- [ ] 草稿删除、编辑和重新生成状态正确。
- [ ] 周期生成可启用、停用并按 7/15/30 天运行。
- [ ] FaFa 不调用任何外部平台发布接口。
- [ ] Agent 可以读取已有报告，但不会无确认触发生成或写入。

### 17.2 技术验收

- [ ] V9 Flyway 可在已有 Phase 8 数据库执行。
- [ ] `ReportPeriod` 领域逻辑无框架依赖，周期边界测试完整。
- [ ] 报告、草稿和周期配置所有读写接口执行用户/宠物归属校验。
- [ ] 事实快照和表达层分离，用户编辑不覆盖事实快照。
- [ ] AI 输出必须通过 sourceKeys、数字、长度、敏感词和免责声明校验。
- [ ] Java 是报告与草稿唯一写入方，Python 不连接 MySQL。
- [ ] 配额获取、幂等、失败回滚和并发上限验证通过。
- [ ] 旧报告使用保存时的 `calculationVersion`，不会因新数据自动漂移。
- [ ] 关键日志不泄漏快照、诊断、票据 URL 和 token。
- [ ] Java 编译/测试、Python 测试、小程序 JS 检查通过。

### 17.3 产品边界验收

- [ ] 页面明确报告只基于已记录数据，不代表未记录的日期没有发生事件。
- [ ] 页面明确 AI 文案需要用户自行确认，FaFa 不负责发布。
- [ ] 健康内容不出现诊断、处方和具体治疗剂量。
- [ ] 费用内容只引用 Phase 8 已保存金额和统计版本。
- [ ] 用户删除报告/草稿不会误删原始照片、账单、医疗档案和事件。
- [ ] 报告生成失败时用户仍能查看可用事实或明确重试原因。

---

## 18. 文档同步清单

Phase 9 实施完成后必须同步：

1. `docs/接口文档.md`：增加报告、内容草稿、周期设置、配额和 Agent 只读接口。
2. `docs/MySQL数据表记录.md`：增加 V9 `pet_report`、`content_draft`、`content_schedule` 表和索引。
3. `docs/Codex开发交接文档.md`：记录报告快照、AI 契约、配额、无发布边界和联调顺序。
4. `docs/分阶段开发计划.md`：将 Phase 9 状态更新为“核心代码完成，待联调”或“已完成”。
5. 本文档：实施后更新状态、勾选验收项并记录真实环境验证结果。
6. Phase 10 规划文档：引用报告/草稿私有数据边界，不重复实现内容发布能力。

---

## 19. 开发规范自检

### Architecture Delineation

报告以 `PetReport` 保存周期事实快照，内容以 `ContentDraft` 保存 AI 初稿和用户编辑稿，`ReportFactAssembler` 负责跨模块读取，`ReportGenerationGateway`/`ContentGenerationGateway` 只负责服务间 AI 调用。首版使用三张表、固定 JSON 契约、同步 HTTP 网关和轻量周期扫描，避免提前引入通用工作流、发布平台和复杂内容资产系统。

### Hard-coding

- 周期、状态、主题和内容格式使用领域枚举。
- 时区、快照版本、提示词版本、字段上限、媒体数量、超时时间和周期批次进入配置。
- 会员次数复用统一配额服务，不在 Controller 和小程序中复制数字。
- 免责声明、敏感词和输出字段限制由统一校验策略维护，不散落在多个生成入口。

### Coupling / Cohesion

- Java 负责事实、权限、快照、配额和持久化；Python 只生成表达层；小程序只展示和编辑。
- 报告事实组装不依赖 AI，AI 不读取数据库，草稿编辑不修改事实表。
- 报告和内容草稿使用独立聚合与仓储，周期配置只负责调度，不保存内容。
- Agent 只读已有报告，不绕过用户确认调用写入或扣费接口。

### Type Safety

- Java 使用 `LocalDate`、`BigDecimal`、领域枚举和不可变周期值对象。
- 数据库使用 `DATE`、`JSON` 和稳定 `VARCHAR` code，不使用数据库枚举锁死演进。
- Python 使用 Pydantic 请求/响应模型，拒绝自由文本结构猜测。
- Java 对 AI 输出做 sourceKeys、数值、字段长度、媒体 ID 和免责声明二次校验。

---

## 20. 实施完成后的自检命令

```text
1. 查看 git status 和 git diff，确认不覆盖 Phase 8 未提交改动。
2. 检查 V9 SQL 在已有 Phase 8 数据库执行。
3. 检查 domain → application → infrastructure 依赖方向。
4. 检查报告、草稿、媒体和周期配置的 userId/petId 归属。
5. 检查所有报告数字来自 Java 快照，Python 不重新计算事实。
6. 检查同周期报告、草稿 idempotencyKey 和周期扫描幂等。
7. 检查共享配额在成功、失败、超时和并发情况下正确回滚/限制。
8. 检查医疗内容、用户备注、签名 URL、token 和外部发布边界。
9. 检查用户编辑只更新 draft_json，不改变 fact_snapshot_json。
10. 运行 Java compile/test、Python test、Node check 和契约测试。
11. 更新接口、数据库、交接文档和阶段计划。
```

## 21. 本次实施记录（2026-08-23）

已完成核心代码落地：

- Java：V9 Flyway、报告/草稿/周期领域模型、仓储、事实快照组装、报告与内容生成网关、结构校验、共享配额、公开接口、Agent 内部只读接口和周期扫描器。
- Python：报告/内容 Pydantic 契约、受控生成 API、无 Key/AI 异常时的确定性降级、Agent 报告只读工具。
- 小程序：报告列表、详情、重新生成、分享草稿编辑、周期生成设置入口。
- 文档：接口示例、V9 表记录、交接清单、阶段计划已同步。

本次验证结果：

1. `fafa-java`：`mvn -q -DskipTests compile` 通过，`mvn -q test` 通过。
2. `fafa-python`：`python -m compileall -q app tests` 通过；新增结构契约和 Agent 意图测试通过。
3. `fafa-miniapp`：全部页面 JavaScript 语法检查和 `app.json` JSON 检查通过。
4. Python 全量测试暂未在当前执行环境完成，因为环境缺少项目依赖 `fastapi`、`pydantic-settings`、`qdrant-client`、`dashscope`；需要在 `requirements.txt` 安装完成的 Python 环境重新执行。

尚未宣称阶段最终完成：真实 MySQL/Flyway、Redis 配额、Python HTTP、百炼模型、MinIO 素材权限和周期任务仍需按第 17 节执行端到端联调；联调通过后再将状态改为“已完成”。

Phase 9 的交付标准不是“生成了一段 AI 文案”，而是让用户能够在不翻查几十张表的情况下，看到一份有周期、有数据口径、有来源、有缺失说明的宠物报告，并在明确的用户确认下得到可编辑、可复制、但不会自动发布的分享草稿。任何模型猜测、未记录消费、医疗诊断和自动发布行为都不能进入本阶段。
