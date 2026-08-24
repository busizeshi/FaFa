# Phase 7 实施文档：食品管理、包装识别与喂食计划

> 依据：《FaFa产品功能设计-最终版.md》第四章每日记录约定、第六章食品与饮食、第十一章 AI 数据查询工具链、第十八章优先级；《分阶段开发计划.md》Phase 7 概要；《.agent/development-guidelines.md》；Phase 6 实施文档及当前代码基线。
>
> 计划日期：2026-08-23
>
> 状态：**核心代码已实施，进入联调与验收**
>
> 前置假设：Phase 0～6 的核心代码已经完成并可运行。Phase 2 的 `daily_status`、`feeding_record`、`weight_record`，Phase 4 的 `media_asset`，Phase 5 的 Java/Python Agent，Phase 6 的健康数据和规则预警链路继续复用。

本阶段把“食品信息—喂食计划—实际喂食—体重变化—动态调整”连成一个可追踪闭环。AI 负责从包装图片提取候选信息和生成建议，Java 负责权限、事实保存、配额、规则校验和最终写入；任何 AI 识别结果或喂食建议都必须经过用户确认，不能直接成为宠物的事实档案。

> 实施记录（2026-08-23）：V7 数据库迁移、食品卡 CRUD、包装识别草稿/确认链路、食品配方提示分、换粮计划、喂食计划/今日清单、动态事实统计、Agent 只读工具和小程序最小入口已落地；当前剩余工作是接入真实 MySQL/Redis/MinIO/Python/Qdrant 环境进行联调，并补齐本机缺失 Python 依赖后的完整测试。

---

## 1. 阶段目标

### 1.1 用户可完成的核心任务

| 能力 | 用户结果 | 主要事实来源 |
|---|---|---|
| 食品管理卡 | 为每只宠物建立多张食品卡，维护品牌、类型、规格、价格、开封日期和营养成分 | `food_card` |
| 包装 AI 识别 | 先上传包装/营养成分表图片，AI 返回候选字段，用户修改并确认后保存 | `media_asset`、`food_recognition_task` |
| 食品配方提示分 | 查看蛋白来源、争议添加物、钙磷信息、阶段适配和信息完整度 | `food_card`、Java 规则服务 |
| 换粮过渡 | 设置旧粮/新粮、过渡天数和每日比例，记录软便、拒食、正常等反应 | `food_transition_plan`、`daily_status` |
| 喂食计划 | 选择食品、设置周期和数量，获取估算建议，修改后确认并启用计划 | `feeding_plan`、`feeding_plan_item` |
| 今日喂食清单 | 查看应喂时段，完成后复用已有喂食记录接口落事实 | `feeding_plan`、`feeding_record` |
| 动态喂养分析 | 将实际喂食量与体重变化、活动和每日状态放在同一时间范围内观察 | `feeding_record`、`weight_record`、`daily_status` |
| AI 对话查询 | 可以查询食品卡、喂食计划和喂食历史，回答必须基于 Java 返回的只读事实 | Phase 5 Agent 工具链 |

### 1.2 阶段完成定义

以下链路在开发环境中跑通，并通过自动化测试和手工验收，才认为 Phase 7 完成：

```text
上传食品包装图片
  → Java 创建媒体素材并校验用户/宠物归属
  → Java 创建食品识别任务并扣减月度识别配额
  → Python 读取图片并返回结构化候选字段
  → Java 保存草稿，不自动写入食品事实
  → 用户编辑并确认
  → Java 创建食品管理卡
  → Java 根据营养字段计算可解释的配方提示分
  → 用户创建或接受喂食计划
  → 今日清单展示计划时段
  → 完成清单时写入 feeding_record
  → 读取实际喂食、体重和每日状态，生成动态分析
```

### 1.3 非目标

- 不做疾病诊断、治疗方案、处方、减重处方或兽医替代建议。
- 不把“食品安全评分”描述成国家标准认证、绝对安全结论或医疗判断；首版名称和 UI 说明使用“配方信息提示分/食品安全提示”，并展示证据和缺失字段。
- 不做食品电商、价格自动同步、条形码商品库或品牌数据库。
- 不训练机器学习模型。首版使用结构化视觉识别、明确的规则评分和可配置公式；后续模型升级不能改变已保存事实。
- 不允许模型直接创建、修改或删除 `food_card`、`feeding_plan`、`feeding_record`。
- 不做家庭成员协同和复杂 operator 权限；继续沿用当前登录用户拥有宠物的权限模型。
- 不把 Phase 8 的支出统计、记账分类和账单关联带入本阶段；食品价格只作为食品卡事实保存。
- 不把 Phase 9 的周报、月报、季度报告和分享卡片作为本阶段交付。

---

## 2. 基线、依赖与关键决策

### 2.1 已有能力基线

| 基线 | 当前能力 | Phase 7 用法 |
|---|---|---|
| Phase 0 | Sa-Token 登录、统一响应、异常处理、用户会员状态 | 所有公开接口复用登录态和 `Result<T>` |
| Phase 1 | 宠物 CRUD 与归属校验 | 所有食品、计划和过渡数据绑定宠物，写入前检查宠物属于当前用户 |
| Phase 2 | `daily_status`、`feeding_record`、`weight_record` | 复用喂食事实；补充食品卡和计划关联；动态分析读取已有记录 |
| Phase 3 | 提醒、事件和完成历史 | 本阶段首版不新增第二套提醒系统，计划清单先在页面展示；需要提醒时复用现有 `reminder` 能力 |
| Phase 4 | `media_asset`、MinIO/OSS、媒体上传和异步 AI 分发 | 包装图片先作为图片素材保存，食品识别只引用 `mediaId` |
| Phase 5 | Python Agent、Java 内部只读接口、模型统一路由 | 增加食品卡/计划/喂食事实查询工具；Python 不直接访问业务数据库 |
| Phase 6 | 健康规则、用药和就医摘要 | 分析可以读取健康相关事实，但不能重复实现健康预警或诊疗逻辑 |
| 基础设施 | MySQL、Flyway、Redis、FastAPI、Qwen、RestClient | 使用 V7 迁移、Redis 月配额、Python 内部调用和现有配置体系 |

### 2.2 当前代码中的复用点

1. `FeedingRecord` 已有 `foodId` 字段和 `g/ml` 单位校验，本阶段将它从“预留字段”变成可用关联；不能破坏没有食品卡的历史喂食记录。
2. `FeedingRecord` 目前的 `FoodType` 只有 `main_food/snack/can/other`。产品设计还需要 `supplement/homemade`，本阶段应扩展为字符串代码或兼容枚举，不使用数据库原生 `ENUM`，以便以后增加类型。
3. `media_asset` 已经承载照片/视频上传和对象存储地址。食品识别入口不重复做一套文件上传控制器，先调用现有媒体上传，再提交 `mediaId`。
4. `QuotaApplicationService` 当前有 Redis 月度 AI 语义检索配额。本阶段新增同一套原子获取/释放机制，配额类型和 key 前缀必须与语义检索隔离。
5. Python 的 `dashscope_client.py` 已有图片下载、视频地址和多模态模型调用能力。食品识别复用 `Purpose.VISION` 的统一模型入口，不在业务服务中散落模型名称。
6. Java 现有 `PythonAiClient` 和 `HttpAiTaskDispatcher` 已封装内部 HTTP。食品识别和喂食建议应新增职责清晰的 gateway/client，不把食品领域请求继续堆到媒体分发器中。

### 2.3 必须遵守的边界

```text
接口层        解析 HTTP、鉴权上下文、参数校验、DTO 转换
应用层        编排事务、权限检查、调用领域服务和仓储
领域层        食品卡、识别任务、过渡、计划的业务不变式
领域仓储      只定义接口，不依赖 MyBatis、Spring 或 Python
基础设施层    MyBatis PO/Mapper、Redis、对象存储、Python Gateway
Python        视觉识别、建议表达、只读事实查询，不写 MySQL
```

每条公开接口都必须满足：

```text
currentUserId = Sa-Token 登录用户
petId 属于 currentUserId
资源的 userId / petId 与当前上下文一致
```

资源不存在和资源不属于当前用户统一返回“不存在”，避免通过错误码探测其他用户的数据。Python 内部接口除 `X-Internal-Token` 外，仍要求 `user_id + pet_id`；内部令牌不是业务授权。

### 2.4 本阶段采用的关键决策

1. **食品卡是事实聚合根**：用户手工输入和 AI 识别确认都最终走同一套 `FoodCard` 创建/更新规则。
2. **识别结果先草稿后确认**：Python 只能返回 `FoodRecognitionDraft`，Java 保存识别任务状态和候选 JSON；只有确认接口才能创建食品卡。
3. **食品评分在 Java 侧确定性计算**：输入固定时输出稳定，维度、分值、缺失原因和规则版本都可解释；Python 不参与最终分数。
4. **计划建议与计划事实分离**：推荐接口只返回预览，不落库；用户编辑并确认后，才创建 `feeding_plan` 和 `feeding_plan_item`。
5. **今日清单不另建执行表**：首版根据生效计划的计划项和 `feeding_record` 中的 `feedingPlanId/feedingPlanItemId/plannedAt` 推导完成状态，避免“计划执行表”和实际喂食事件双写不一致。
6. **实际喂食仍是事件流**：继续使用 `POST /api/feeding-records`，计划关联字段可选；手工喂食和计划喂食都能记录，保留一天多条事件的设计。
7. **月度识别配额按发起识别消耗**：重复查询任务结果不重复扣减；识别失败释放配额；识别成功但用户拒绝仍视为已使用一次。
8. **营养字段保留单位与来源**：每个字段要能区分“用户输入、包装识别、模型推断”；识别没有读到的字段必须为 `null`，不能补猜。
9. **公式版本化**：每日能量和喂食建议保存 `policyVersion`；以后调整参数只影响新建议，不改写历史计划和已记录的喂食事实。
10. **首版不自动创建提醒**：计划清单先支持查看和完成。若产品确认需要通知，再复用 Phase 3 `reminder`，不新增 scheduler 或通知系统。

---

## 3. 业务流程与状态机

### 3.1 食品卡流程

```text
手工创建 / 识别确认
  → 校验宠物归属和食品字段
  → 保存 food_card
  → 可选查看配方信息提示分
  → 可用于过渡计划和喂食计划
  → 停用或逻辑删除
```

食品卡状态：

| 状态 | 含义 | 可否用于新计划 |
|---|---|---|
| `active` | 正常使用 | 是 |
| `archived` | 暂时停用或已吃完 | 否，历史记录仍保留 |
| `deleted` | 用户逻辑删除 | 否，历史关联只显示快照名称或“已删除食品” |

不能物理删除已有食品卡，避免历史 `feeding_record.food_id` 失去可追溯性。

### 3.2 包装识别流程

```text
已有媒体素材(media_asset)
  → POST /api/foods/recognitions
  → 校验图片类型、宠物归属、月配额、幂等键
  → 创建 PENDING 任务并获取配额
  → Python 读取图片并返回候选字段
  → Java 保存 COMPLETED 草稿
  → 用户查看、修改、确认或拒绝
  → CONFIRMED 时创建 food_card
```

识别任务状态：

| 状态 | 含义 | 允许的下一步 |
|---|---|---|
| `pending` | 已创建，等待调用 Python | `processing`、`failed` |
| `processing` | Python 正在识别 | `completed`、`failed` |
| `completed` | 有待确认候选结果 | `confirmed`、`rejected`、`expired` |
| `confirmed` | 已生成食品卡 | 只读 |
| `rejected` | 用户放弃此结果 | 只读 |
| `failed` | 调用失败或结果无效 | 可用新幂等键重试 |
| `expired` | 草稿超过配置保留时间 | 只读 |

同一 `userId + mediaId + active task` 只允许一个未结束任务。确认接口必须幂等，重复确认返回第一次生成的 `foodCardId`。

### 3.3 食品配方信息提示流程

```text
读取 food_card 营养字段
  → 规范化单位和字段范围
  → FoodSafetyScorePolicy 逐维度计算
  → 生成 score、radar、evidence、missingFields、policyVersion
  → 返回页面展示
```

评分不单独写入食品卡的“永久分数”字段。首版按当前食品卡事实实时计算；如果后续需要历史快照，另建带规则版本的分析快照，不覆盖原始营养数据。

### 3.4 换粮过渡流程

```text
选择旧粮 + 新粮
  → 设置开始日期、过渡天数和每日比例
  → 校验每天比例和总量
  → ACTIVE 后按日期展示目标比例
  → 用户记录软便/拒食/正常等反应
  → 关联 daily_status 的已有记录
  → 完成、取消或重新编辑
```

过渡计划状态：`draft`、`active`、`completed`、`cancelled`。旧粮和新粮必须属于同一宠物；已删除食品不能创建新计划，但历史计划仍可查看。

### 3.5 喂食计划流程

```text
选择宠物和一张或多张 active 食品卡
  → POST /api/feeding-plans/recommendations
  → Java 组装宠物事实、食品营养和用户约束
  → Python 可选返回表达性建议
  → Java 规则校验并返回预览
  → 用户调整总量、时段、每餐量和食品组合
  → POST /api/feeding-plans 保存 ACTIVE 计划
  → 今日页面根据计划项生成清单
  → 完成清单复用 feeding-records 写入实际事件
```

计划状态：

```text
DRAFT ──确认──> ACTIVE ──暂停──> PAUSED
  │                 │              │
  └─取消────────────┴──完成───────┘
```

计划更新采用版本号：修改已生效计划时生成新的 `version`，新日期使用新版本；历史日期和实际喂食记录保持原快照。首版可以采用“更新当前计划并保留 `updatedAt`”实现，但接口和数据模型必须预留版本字段，不能改写已经完成的事件。

### 3.6 动态喂养分析流程

```text
用户选择宠物和日期范围
  → Java 校验 ≤ 366 天并读取喂食、体重、状态和计划事实
  → 计算确定性统计：日均量、计划偏差、体重趋势、状态关联
  → Python 可选生成自然语言说明
  → 返回证据、限制和建议复核项
```

分析只能说“记录显示”“建议复核包装建议/活动变化/咨询专业人士”，不能输出“必须减 10g”“应当诊断为某病”等处方式结论。

---

## 4. 领域模型设计

### 4.1 FoodCard 食品管理卡聚合

建议路径：`fafa-java/src/main/java/com/fafa/domain/model/food/FoodCard.java`

核心字段：

| 字段 | 约束 |
|---|---|
| `id` | 创建后生成，不能由客户端指定 |
| `userId` / `petId` | 创建后不可变，应用层检查宠物归属 |
| `name` | 必填，1～100 字符 |
| `brand` | 可空，最长 100 字符 |
| `foodType` | `main_food/canned/snack/supplement/homemade/other` |
| `specification` | 可空，最长 100 字符，如 `2kg`、`85g*12` |
| `packageWeight` / `packageWeightUnit` | 可空，重量必须大于 0，单位 `g/ml` |
| `price` / `currency` | 可空，价格不能为负，首版默认 `CNY` |
| `openedAt` / `expiresAt` | 可空，过期日期不能早于开封日期 |
| `nutritionBasis` | `per_100g`、`per_100ml`、`serving`、`unknown` |
| `proteinPercent`、`fatPercent`、`fiberPercent`、`moisturePercent`、`ashPercent` | 0～100，可空 |
| `calciumPercent` / `phosphorusPercent` | 0～100，可空；两者都存在时才计算比例 |
| `energyKcal` / `energyUnit` | 能量必须大于 0，首版支持 `kcal_per_100g`、`kcal_per_100ml` |
| `proteinSources` | 结构化文本数组，可空；不能把模糊原料强行归类 |
| `ingredients` | 原始配料表文本或数组，保留用户/包装原文 |
| `additiveFlags` | `bha/bht/artificial_color/unknown` 等机器可识别标记 |
| `lifeStageLabel` | 包装标注的幼年/成犬/全价等原文，可空 |
| `sourceMediaId` | 可选，关联包装图片，不拥有媒体文件 |
| `sourceRecognitionTaskId` | 可选，追溯识别任务 |
| `status` | `active/archived/deleted` |

领域行为：

```java
create(...)
updateUserFields(...)
updateNutrition(...)
archive()
restore()
delete()
canBeUsedForNewPlan()
toNutritionProfile()
```

`FoodCard` 只负责字段不变式和状态转换，不调用对象存储、Python、Redis 或仓储。

### 4.2 FoodNutritionProfile 值对象

建议路径：`domain/model/food/FoodNutritionProfile.java`

- 所有百分比使用 `BigDecimal`，统一保留 4 位小数，展示层再格式化。
- 识别到的单位先通过 `NutritionNormalizer` 转换；无法确定是每 100g 还是每份时，保留原值并把 `nutritionBasis` 标为 `unknown`。
- 能量、钙、磷未识别时保持 `null`，不使用 0 代替缺失。
- 配料表中出现 `BHA/BHT/人工色素` 只生成提示标记，不直接生成危害结论。
- 自制食品如果没有完整营养数据，可以建立食品卡，但评分和每日能量估算应返回“数据不足”。

### 4.3 FoodRecognitionTask 识别任务

建议路径：`domain/model/food/FoodRecognitionTask.java`

任务负责：

- 校验任务只能引用当前用户可访问的图片素材。
- 记录 `requestId/idempotencyKey`、模型名、提示词版本和耗时，便于排查。
- 管理 `pending/processing/completed/confirmed/rejected/failed/expired` 状态。
- 保存候选结果和不确定字段，但不创建食品事实。
- 通过 `confirm()` 保证一次任务只关联一个食品卡。

候选结果建议使用 `FoodRecognitionDraft` DTO/值对象，字段与食品卡相近，但每个字段额外包含：

```json
{
  "value": "鸡肉",
  "confidence": 0.92,
  "source": "package_image",
  "needsConfirmation": false
}
```

不要把模型返回的任意 JSON 直接反序列化到持久化 PO；先进入受限 schema，再进行字段白名单校验和归一化。

### 4.4 FoodTransitionPlan 换粮过渡聚合

建议路径：`domain/model/food/FoodTransitionPlan.java`

字段：

| 字段 | 说明 |
|---|---|
| `petId` | 过渡所属宠物 |
| `oldFoodId` / `newFoodId` | 两张食品卡，必须属于同一宠物 |
| `startDate` / `endDate` | 起止日期，包含首尾 |
| `dailyTargetAmount` / `unit` | 可选的每日目标总量 |
| `ratioSteps` | 每天旧粮/新粮百分比，如 `[{"day":1,"old":75,"new":25}]` |
| `reaction` | 最近一次或按日期记录的 `normal/soft_stool/refused/diarrhea/vomiting/other` |
| `reactionNote` | 最长 500 字符 |
| `status` | `draft/active/completed/cancelled` |

领域不变式：

- 过渡天数在配置范围内，默认 3～30 天；客户端不能绕过上限。
- 每个步骤的旧粮比例与新粮比例之和必须为 100。
- 步骤日期连续且覆盖整个计划区间。
- 目标量不能为负，单位只允许 `g/ml`。
- `refused/diarrhea/vomiting` 只作为用户记录事实和提示复核，不自动改变计划、不生成诊断。

### 4.5 FeedingPlan 与 FeedingPlanItem

建议路径：

- `domain/model/feeding/FeedingPlan.java`
- `domain/model/feeding/FeedingPlanItem.java`

`FeedingPlan` 字段：

| 字段 | 说明 |
|---|---|
| `petId` | 计划所属宠物 |
| `name` | 用户自定义名称，可空 |
| `startDate` / `endDate` | 计划日期范围，可只设开始日期 |
| `dailyTargetCalories` | 可选的估算目标，仅作为用户可编辑输入 |
| `dailyTargetAmount` / `unit` | 可选每日总量 |
| `recommendationSource` | `user_input/rule/ai_assisted` |
| `policyVersion` | 能量/建议公式版本 |
| `status` | `draft/active/paused/completed/cancelled` |
| `version` | 计划编辑版本 |
| `acceptedAt` | 用户确认时间 |
| `notes` | 最长 500 字符 |

`FeedingPlanItem` 字段：

| 字段 | 说明 |
|---|---|
| `itemKey` | 计划内稳定键，如 `breakfast`、`dinner`、`snack-1` |
| `foodId` | 使用的食品卡 |
| `mealTime` | `HH:mm`，计划项时间 |
| `amount` / `unit` | 每次目标量 |
| `percentage` | 占每日总量比例，可空；同一计划的比例总和不超过 100 |
| `cycleType` / `cycleValue` | `daily/weekly/custom`；首版推荐支持 `daily` |
| `nutritionSnapshot` | 创建计划时食品营养快照，JSON 只作为历史展示，不作为新食品事实 |
| `sortOrder` | 展示顺序 |

计划领域行为：

```java
createDraft(...)
replaceItems(...)
accept()
pause()
resume()
complete()
cancel()
buildChecklist(LocalDate date, List<FeedingRecord> records)
```

### 4.6 既有 FeedingRecord 的 Phase 7 扩展

保留现有字段和接口兼容性，新增可选字段：

| 字段 | 说明 |
|---|---|
| `feedingPlanId` | 来源计划，可空；手工喂食不需要填写 |
| `feedingPlanItemId` | 来源计划项，可空 |
| `plannedAt` | 本次计划时段，可空，实际 `feedingTime` 仍为用户记录的真实时间 |
| `foodNameSnapshot` | 可选快照，食品卡删除后仍能在历史中显示名称 |

新增字段不能要求旧记录补值。已有 `foodId=null` 的历史记录继续可查、可编辑和可删除。

---

## 5. 数据库与迁移设计

### 5.1 迁移文件

新增：

```text
fafa-java/src/main/resources/db/migration/V7__create_food_and_feeding_plan_tables.sql
```

迁移要求：

- 使用现有 MySQL 方言、字符集和时间字段风格。
- 不使用数据库原生 `ENUM`，状态和类型使用 `varchar`，由领域层白名单校验。
- 遵循现有逻辑删除策略，删除字段统一为 `is_deleted`。
- 不在首版引入跨聚合数据库外键；所有归属、状态和存在性检查由应用层完成，避免与既有表风格不一致。
- JSON 字段使用 MySQL `json`，仅保存展示或快照信息；需要筛选的字段仍使用普通列。
- 每个新表包含 `created_at`、`updated_at`；需要审计的状态表增加 `last_error`、`completed_at` 等字段。

### 5.2 `food_card`

建议字段：

| 列 | 类型建议 | 说明 |
|---|---|---|
| `id` | `bigint` | 主键 |
| `user_id` / `pet_id` | `bigint` | 归属 |
| `name` / `brand` | `varchar(100)` | 食品名称、品牌 |
| `food_type` | `varchar(32)` | 食品类型代码 |
| `specification` | `varchar(100)` | 包装规格 |
| `package_weight` | `decimal(12,3)` | 包装重量/容量 |
| `package_weight_unit` | `varchar(8)` | `g/ml` |
| `price` | `decimal(12,2)` | 价格，可空 |
| `currency` | `varchar(8)` | 默认 `CNY` |
| `opened_at` / `expires_at` | `date` | 开封、保质期 |
| `nutrition_basis` | `varchar(24)` | 每100g/每100ml/每份/未知 |
| `protein_percent`、`fat_percent`、`fiber_percent`、`moisture_percent`、`ash_percent` | `decimal(7,4)` | 保证分析值 |
| `calcium_percent` / `phosphorus_percent` | `decimal(7,4)` | 钙磷值 |
| `energy_kcal` | `decimal(12,4)` | 与 `energy_unit` 配套 |
| `energy_unit` | `varchar(24)` | 能量单位 |
| `protein_sources` | `json` | 结构化蛋白来源 |
| `ingredients` | `json` | 原始配料表 |
| `additive_flags` | `json` | 规则识别到的标记 |
| `life_stage_label` | `varchar(100)` | 包装阶段标注 |
| `source_media_id` / `source_recognition_task_id` | `bigint` | 来源追溯 |
| `status` | `varchar(16)` | `active/archived/deleted` |
| `is_deleted` | `tinyint` | 逻辑删除 |
| `created_at` / `updated_at` | `datetime` | 时间 |

索引：

```text
idx_food_card_user_pet_status (user_id, pet_id, status, is_deleted)
idx_food_card_pet_updated (pet_id, updated_at)
idx_food_card_source_media (source_media_id)
```

### 5.3 `food_recognition_task`

建议字段：

| 列 | 类型建议 | 说明 |
|---|---|---|
| `id` | `bigint` | 主键 |
| `user_id` / `pet_id` / `media_id` | `bigint` | 请求上下文和输入媒体 |
| `idempotency_key` | `varchar(100)` | 客户端请求幂等键 |
| `status` | `varchar(20)` | 任务状态 |
| `model_name` / `prompt_version` | `varchar(100)` | 模型审计 |
| `draft_json` | `json` | 白名单 schema 后的候选结果 |
| `raw_response_json` | `json` | 可选；脱敏后保留，大小受限 |
| `error_code` / `error_message` | `varchar(64)` / `varchar(500)` | 失败信息 |
| `food_card_id` | `bigint` | 确认后回填 |
| `quota_month` | `char(7)` | 例如 `2026-08` |
| `started_at` / `completed_at` / `expired_at` | `datetime` | 生命周期时间 |
| `created_at` / `updated_at` | `datetime` | 时间 |

唯一性和索引：

```text
uk_food_recognition_request (user_id, idempotency_key)
idx_food_recognition_media_status (user_id, media_id, status)
idx_food_recognition_pet_created (pet_id, created_at)
```

如果客户端未提供幂等键，Java 生成 `food-recognition-{mediaId}-{uuid}`；不要把 mediaId 单独作为全局唯一键，以便用户在识别失败后重新发起任务。

### 5.4 `food_transition_plan`

建议字段：

| 列 | 类型建议 | 说明 |
|---|---|---|
| `id` | `bigint` | 主键 |
| `user_id` / `pet_id` | `bigint` | 归属 |
| `old_food_id` / `new_food_id` | `bigint` | 旧粮、新粮 |
| `start_date` / `end_date` | `date` | 日期范围 |
| `daily_target_amount` | `decimal(12,3)` | 可空 |
| `unit` | `varchar(8)` | `g/ml` |
| `ratio_steps` | `json` | 日期/比例快照 |
| `reaction_code` | `varchar(24)` | 最近反应 |
| `reaction_note` | `varchar(500)` | 反应备注 |
| `status` | `varchar(16)` | 计划状态 |
| `is_deleted` | `tinyint` | 逻辑删除 |
| `created_at` / `updated_at` | `datetime` | 时间 |

索引：`idx_transition_pet_date (pet_id, start_date, end_date, status, is_deleted)`。

### 5.5 `feeding_plan` 与 `feeding_plan_item`

`feeding_plan` 建议字段：

| 列 | 类型建议 | 说明 |
|---|---|---|
| `id` | `bigint` | 主键 |
| `user_id` / `pet_id` | `bigint` | 归属 |
| `name` | `varchar(100)` | 计划名称 |
| `start_date` / `end_date` | `date` | 计划范围 |
| `daily_target_calories` | `decimal(12,4)` | 可空估算值 |
| `daily_target_amount` | `decimal(12,3)` | 可空 |
| `unit` | `varchar(8)` | `g/ml` |
| `recommendation_source` | `varchar(24)` | `user_input/rule/ai_assisted` |
| `policy_version` | `varchar(32)` | 公式版本 |
| `version` | `int` | 编辑版本，从 1 开始 |
| `status` | `varchar(16)` | 计划状态 |
| `accepted_at` | `datetime` | 用户确认时间 |
| `notes` | `varchar(500)` | 备注 |
| `is_deleted` | `tinyint` | 逻辑删除 |
| `created_at` / `updated_at` | `datetime` | 时间 |

`feeding_plan_item` 建议字段：

| 列 | 类型建议 | 说明 |
|---|---|---|
| `id` | `bigint` | 主键 |
| `feeding_plan_id` | `bigint` | 计划 ID |
| `item_key` | `varchar(64)` | 计划内稳定键 |
| `food_id` | `bigint` | 食品卡 ID |
| `meal_time` | `time` | 计划时段 |
| `amount` | `decimal(12,3)` | 目标量 |
| `unit` | `varchar(8)` | `g/ml` |
| `percentage` | `decimal(7,4)` | 每日占比，可空 |
| `cycle_type` / `cycle_value` | `varchar(16)` / `varchar(64)` | 周期规则 |
| `nutrition_snapshot` | `json` | 创建时营养快照 |
| `sort_order` | `int` | 顺序 |
| `is_deleted` | `tinyint` | 逻辑删除 |
| `created_at` / `updated_at` | `datetime` | 时间 |

约束和索引：

```text
uk_feeding_plan_item_key (feeding_plan_id, item_key, is_deleted)
idx_feeding_plan_pet_status (pet_id, status, start_date, end_date, is_deleted)
idx_feeding_plan_item_plan_time (feeding_plan_id, meal_time, is_deleted)
```

### 5.6 `feeding_record` 兼容性修改

在既有表增加：

```text
feeding_plan_id       bigint null
feeding_plan_item_id  bigint null
planned_at            datetime null
food_name_snapshot    varchar(100) null
```

新增索引：

```text
idx_feeding_record_plan (feeding_plan_id, planned_at, is_deleted)
idx_feeding_record_food (pet_id, food_id, feeding_time, is_deleted)
```

迁移不得为历史记录补造计划 ID，也不得删除现有 `food_id`。旧数据中的 `food_type=can` 与新食品卡 `food_type=canned` 的映射只在应用层处理，不能直接改写历史记录。

### 5.7 数据迁移验收

- 在已有 Phase 0～6 数据上执行 V7，所有旧表和旧接口仍可读写。
- 重复执行迁移不会产生额外表或索引错误。
- 创建带食品卡的喂食记录后，食品卡删除/归档不影响历史记录查询。
- 识别任务 JSON 超长、字段异常或模型返回未知字段时，Java 拒绝保存而不是截断写入。
- 通过 explain 检查食品列表、计划列表和喂食范围查询使用索引。

---

## 6. Java 后端实施设计

### 6.1 建议包结构

```text
com.fafa
├── interfaces
│   ├── controller
│   │   ├── FoodCardController.java
│   │   ├── FoodRecognitionController.java
│   │   ├── FoodTransitionController.java
│   │   ├── FeedingPlanController.java
│   │   └── FeedingAnalysisController.java
│   └── dto/food
├── application
│   ├── service
│   │   ├── FoodCardApplicationService.java
│   │   ├── FoodRecognitionApplicationService.java
│   │   ├── FoodTransitionApplicationService.java
│   │   ├── FeedingPlanApplicationService.java
│   │   └── FeedingAnalysisApplicationService.java
│   └── dto/food
├── domain
│   ├── model/food
│   ├── model/feeding
│   ├── repository
│   │   ├── FoodCardRepository.java
│   │   ├── FoodRecognitionTaskRepository.java
│   │   ├── FoodTransitionPlanRepository.java
│   │   └── FeedingPlanRepository.java
│   └── service
│       ├── FoodSafetyScoreService.java
│       ├── NutritionNormalizer.java
│       ├── FeedingRecommendationPolicy.java
│       └── FeedingAnalysisService.java
└── infrastructure
    ├── ai
    │   ├── FoodRecognitionGateway.java
    │   ├── FeedingRecommendationGateway.java
    │   └── impl/HttpFoodAiGateway.java
    └── persistence
        ├── mapper/*Food*Mapper.java
        ├── po/*Food*PO.java
        └── repository/*Food*RepositoryImpl.java
```

命名可以结合项目现有风格调整，但依赖方向必须保持不变。不要让 `FoodCard`、`FoodSafetyScoreService` 直接依赖 MyBatis、RestClient、Redis 或配置 bean。

### 6.2 应用服务职责

#### `FoodCardApplicationService`

- 获取当前用户。
- 校验 `petId` 归属。
- 校验食品字段和食品类型。
- 创建、更新、归档、删除和列表查询。
- 更新食品营养时重新规范化来源字段。
- 防止将其他宠物的食品卡用于当前宠物计划。

#### `FoodRecognitionApplicationService`

- 检查图片 `mediaId` 属于当前用户、类型为 `photo`、未删除且可读取。
- 生成/校验幂等键。
- 调用配额服务获取 `foodRecognition` 配额。
- 创建任务并调用 Python gateway。
- 成功后保存受限草稿；失败时保存失败原因并释放配额。
- 读取任务时隐藏原始敏感 URL，只返回前端需要的媒体引用。
- 确认时应用用户覆盖字段，调用 `FoodCardApplicationService` 创建食品卡，并在同一事务中回填任务。

#### `FoodTransitionApplicationService`

- 校验两张食品卡归属和状态。
- 生成默认比例或校验用户自定义比例。
- 管理过渡状态和按日期的反应记录。
- 查询同宠物的 `daily_status`，只做关联展示，不覆盖每日状态事实。

#### `FeedingPlanApplicationService`

- 读取宠物档案、食品卡营养和用户输入。
- 调用 Java 规则计算基础预览，可选调用 Python 生成说明。
- 对模型返回的建议再次进行数量、时段、食品归属和边界校验。
- 保存用户确认后的计划和计划项。
- 返回指定日期的计划清单，并以实际 `feeding_record` 匹配完成状态。
- 调整或暂停计划时维护版本和食品营养快照。

#### `FeedingAnalysisApplicationService`

- 校验日期范围不超过 366 天。
- 读取 Java 掌握的喂食、体重、状态和计划事实。
- 先计算确定性统计，再调用 Python 生成可选自然语言说明。
- Python 失败时仍返回统计结果和明确的“说明生成失败”状态。

### 6.3 食品类型兼容策略

新增食品卡类型统一使用：

```text
main_food  主食
canned     罐头
snack      零食
supplement 补充剂
homemade   自制
other      其他
```

接口兼容旧喂食记录的 `can`：

```text
feeding_record.food_type=can  → 展示层标签“罐头”
food_card.food_type=canned    → 新建食品卡和新计划使用 canned
```

如果前端仍传 `can` 到喂食记录接口，首版继续接受；新食品卡接口不接受没有明确迁移含义的未知代码。

### 6.4 配额设计

新增配置和服务方法：

```text
fafa.quota.food-recognition.monthly-normal=5
fafa.quota.food-recognition.monthly-vip=50
```

Redis key 建议：

```text
fafa:quota:food-recognition:{userId}:{yyyy-MM}
```

调用顺序：

```text
幂等查询
  → tryAcquireFoodRecognition(userId)
  → 写入 processing 任务
  → 调 Python
  → 成功：保留用量
  → 失败：releaseFoodRecognition(userId)
```

普通用户上限 5 次/月，VIP 上限 50 次/月。VIP 也应计量，便于展示使用量；若产品决定 VIP 不受限，只需在配置/服务中明确而不能写死在 controller。

必须补充并发测试：同一用户并发 10 次识别请求，上限 5 时最多 5 个进入 Python；失败任务释放后可以再次发起，不得出现负数。

### 6.5 配置类建议

在 `FaFaProperties` 增加：

```java
private Food food = new Food();
private Feeding feeding = new Feeding();
```

配置职责：

```text
Food.Recognition.monthlyNormal = 5
Food.Recognition.monthlyVip = 50
Food.Recognition.timeoutSeconds = 90
Food.Recognition.draftRetentionDays = 7
Food.Recognition.maxImageBytes = 10MB
Food.Transition.minDays = 3
Food.Transition.maxDays = 30
Food.Score.policyVersion = "v1"
Feeding.maxRangeDays = 366
Feeding.maxItemsPerPlan = 12
Feeding.recommendationTimeoutSeconds = 45
Feeding.policyVersion = "v1"
```

所有环境在 `application-dev.yml`、`application-test.yml`、`application-prod.yml` 和 `.env.example` 中给出显式值或合理默认值；业务代码不出现会员次数、评分权重、模型名称等魔法数字。

---

## 7. 对外接口定义与 JSON 示例

以下示例遵循现有统一响应结构：`code=0` 表示成功，`message` 为提示，业务内容位于 `data`。所有接口都需要登录请求头：

```http
Authorization: {sa-token}
Content-Type: application/json
```

### 7.1 食品卡

#### 7.1.1 创建食品卡

```http
POST /api/foods
```

请求：

```json
{
  "petId": 1,
  "name": "室内成猫鸡肉配方",
  "brand": "示例品牌",
  "foodType": "main_food",
  "specification": "2kg",
  "packageWeight": 2000,
  "packageWeightUnit": "g",
  "price": 159.90,
  "currency": "CNY",
  "openedAt": "2026-08-20",
  "expiresAt": "2027-08-20",
  "nutritionBasis": "per_100g",
  "proteinPercent": 38.0,
  "fatPercent": 16.0,
  "fiberPercent": 4.0,
  "moisturePercent": 10.0,
  "ashPercent": 8.0,
  "calciumPercent": 1.2,
  "phosphorusPercent": 0.9,
  "energyKcal": 385.0,
  "energyUnit": "kcal_per_100g",
  "proteinSources": ["鸡肉", "鸡肉粉"],
  "ingredients": ["鸡肉", "鸡肉粉", "鱼油"],
  "additiveFlags": [],
  "lifeStageLabel": "成猫/全价",
  "sourceMediaId": 201,
  "notes": "用户确认包装识别结果"
}
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 301,
    "petId": 1,
    "name": "室内成猫鸡肉配方",
    "brand": "示例品牌",
    "foodType": "main_food",
    "status": "active",
    "nutrition": {
      "basis": "per_100g",
      "proteinPercent": 38.0,
      "fatPercent": 16.0,
      "calciumPercent": 1.2,
      "phosphorusPercent": 0.9,
      "energyKcal": 385.0,
      "energyUnit": "kcal_per_100g"
    },
    "source": {
      "mediaId": 201,
      "recognitionTaskId": 401
    },
    "createdAt": "2026-08-23T14:00:00"
  }
}
```

#### 7.1.2 查询食品卡列表

```http
GET /api/foods?petId=1&status=active&foodType=main_food
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": 301,
      "petId": 1,
      "name": "室内成猫鸡肉配方",
      "brand": "示例品牌",
      "foodType": "main_food",
      "specification": "2kg",
      "status": "active",
      "openedAt": "2026-08-20",
      "nutritionCompleteness": 0.92,
      "energyKcal": 385.0,
      "energyUnit": "kcal_per_100g",
      "updatedAt": "2026-08-23T14:00:00"
    }
  ]
}
```

#### 7.1.3 查询食品卡详情

```http
GET /api/foods/301
```

响应：返回创建接口中的完整字段，并增加 `recognitionTaskId`、`nutritionCompleteness`、`activePlanCount`。详情接口不直接返回包装图片二进制，只返回 `sourceMediaId` 和已有媒体详情地址。

#### 7.1.4 更新食品卡

```http
PUT /api/foods/301
```

请求只允许修改名称、品牌、规格、价格、日期、营养字段和备注；`petId`、`id`、创建者不可修改：

```json
{
  "name": "室内成猫鸡肉配方（新包装）",
  "brand": "示例品牌",
  "foodType": "main_food",
  "specification": "2kg",
  "price": 149.90,
  "openedAt": "2026-08-20",
  "proteinPercent": 38.0,
  "fatPercent": 16.0,
  "calciumPercent": 1.2,
  "phosphorusPercent": 0.9,
  "energyKcal": 385.0,
  "energyUnit": "kcal_per_100g",
  "notes": "手工修正价格"
}
```

响应：返回更新后的食品卡详情。

#### 7.1.5 归档/逻辑删除食品卡

```http
DELETE /api/foods/301
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

如果食品卡仍被 active 计划使用，建议先返回业务错误 `FOOD_IN_ACTIVE_PLAN`，要求用户暂停或修改计划；历史喂食记录不受影响。

### 7.2 包装 AI 识别

#### 7.2.1 创建识别任务

包装图片应先通过现有媒体上传接口保存。识别接口只接受已保存图片的 `mediaId`，不接受用户直接传任意 URL。

```http
POST /api/foods/recognitions
```

请求：

```json
{
  "petId": 1,
  "mediaId": 201,
  "idempotencyKey": "food-package-20260823-001"
}
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "taskId": 401,
    "status": "processing",
    "mediaId": 201,
    "quota": {
      "type": "food_recognition",
      "used": 1,
      "limit": 5,
      "month": "2026-08"
    },
    "createdAt": "2026-08-23T14:02:00"
  }
}
```

如果同一幂等键已经有任务，返回原任务，不再次扣配额。

#### 7.2.2 查询识别任务

```http
GET /api/foods/recognitions/401
```

成功草稿响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "taskId": 401,
    "status": "completed",
    "mediaId": 201,
    "draft": {
      "name": {"value": "室内成猫鸡肉配方", "confidence": 0.94, "needsConfirmation": false},
      "brand": {"value": "示例品牌", "confidence": 0.88, "needsConfirmation": false},
      "foodType": {"value": "main_food", "confidence": 0.72, "needsConfirmation": true},
      "specification": {"value": "2kg", "confidence": 0.91, "needsConfirmation": false},
      "proteinPercent": {"value": 38.0, "confidence": 0.97, "needsConfirmation": false},
      "fatPercent": {"value": 16.0, "confidence": 0.96, "needsConfirmation": false},
      "calciumPercent": {"value": 1.2, "confidence": 0.81, "needsConfirmation": true},
      "phosphorusPercent": {"value": 0.9, "confidence": 0.80, "needsConfirmation": true},
      "energyKcal": {"value": 385.0, "confidence": 0.86, "needsConfirmation": false},
      "ingredients": {"value": ["鸡肉", "鸡肉粉", "鱼油"], "confidence": 0.78, "needsConfirmation": true},
      "warnings": ["包装图片中未确认保质期", "部分配料文字清晰度不足"],
      "model": "configured-vision-model",
      "promptVersion": "food-recognition-v1"
    },
    "expiresAt": "2026-08-30T14:05:00"
  }
}
```

处理中响应只返回状态和任务 ID；失败响应返回可展示的错误码和重试提示，不返回 Python 异常堆栈。

#### 7.2.3 确认识别结果并创建食品卡

```http
POST /api/foods/recognitions/401/confirm
```

请求允许用户覆盖任意候选字段，未填写的字段使用草稿值：

```json
{
  "name": "室内成猫鸡肉配方",
  "brand": "示例品牌",
  "foodType": "main_food",
  "specification": "2kg",
  "nutritionBasis": "per_100g",
  "proteinPercent": 38.0,
  "fatPercent": 16.0,
  "calciumPercent": 1.2,
  "phosphorusPercent": 0.9,
  "energyKcal": 385.0,
  "energyUnit": "kcal_per_100g",
  "lifeStageLabel": "成猫/全价",
  "notes": "已核对包装标签"
}
```

响应：返回 `foodCardId` 和完整食品卡。重复确认不创建第二张卡，返回同一个 `foodCardId`。

#### 7.2.4 拒绝识别结果

```http
POST /api/foods/recognitions/401/reject
```

请求：

```json
{"reason": "图片模糊，改为手工录入"}
```

响应为成功空数据。拒绝不释放已成功识别消耗的月度配额；失败任务释放配额。

### 7.3 食品配方信息提示分

#### 7.3.1 查询食品提示分

```http
GET /api/foods/301/safety-score
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "foodId": 301,
    "score": 82,
    "label": "信息较完整",
    "policyVersion": "v1",
    "dimensions": [
      {"key": "protein_source", "label": "蛋白来源清晰度", "score": 23, "maxScore": 25, "status": "known", "evidence": ["鸡肉", "鸡肉粉"]},
      {"key": "additives", "label": "添加物提示", "score": 25, "maxScore": 25, "status": "known", "evidence": []},
      {"key": "calcium_phosphorus", "label": "钙磷信息", "score": 20, "maxScore": 25, "status": "known", "evidence": ["Ca/P=1.33"]},
      {"key": "life_stage", "label": "阶段标注", "score": 14, "maxScore": 25, "status": "partial", "evidence": ["成猫/全价"]}
    ],
    "missingFields": ["具体适用活动量"],
    "warnings": ["该结果是配方信息提示，不是食品安全认证或医疗建议"],
    "calculatedAt": "2026-08-23T14:10:00"
  }
}
```

若营养数据不足，返回 `score=null`、`label="数据不足"` 和缺失字段，不能把缺失数据按 0 分惩罚后伪装成风险分数。

### 7.4 换粮过渡

#### 7.4.1 创建换粮计划

```http
POST /api/food-transitions
```

请求：

```json
{
  "petId": 1,
  "oldFoodId": 301,
  "newFoodId": 302,
  "startDate": "2026-08-24",
  "endDate": "2026-08-30",
  "dailyTargetAmount": 80,
  "unit": "g",
  "ratioSteps": [
    {"date": "2026-08-24", "oldPercent": 75, "newPercent": 25},
    {"date": "2026-08-25", "oldPercent": 60, "newPercent": 40},
    {"date": "2026-08-26", "oldPercent": 50, "newPercent": 50},
    {"date": "2026-08-27", "oldPercent": 40, "newPercent": 60},
    {"date": "2026-08-28", "oldPercent": 25, "newPercent": 75},
    {"date": "2026-08-29", "oldPercent": 10, "newPercent": 90},
    {"date": "2026-08-30", "oldPercent": 0, "newPercent": 100}
  ],
  "status": "active"
}
```

响应：返回计划详情，并增加当天目标量，例如旧粮 60g、新粮 20g。计算结果仅为比例换算，不是医疗建议。

#### 7.4.2 查询换粮计划与反应

```http
GET /api/food-transitions?petId=1&status=active
GET /api/food-transitions/501?includeDailyStatus=true
```

#### 7.4.3 更新每日反应

```http
PUT /api/food-transitions/501/reaction
```

请求：

```json
{
  "date": "2026-08-25",
  "reaction": "soft_stool",
  "note": "当天有一次软便，食欲正常"
}
```

响应：返回反应记录和关联的 `dailyStatus` 摘要。若没有对应每日状态，不自动创建每日状态，只返回 `dailyStatusMatched=false`。

### 7.5 喂食计划

#### 7.5.1 生成建议预览

```http
POST /api/feeding-plans/recommendations
```

请求：

```json
{
  "petId": 1,
  "foodIds": [301, 302],
  "startDate": "2026-08-24",
  "dailyTargetAmount": 80,
  "unit": "g",
  "mealCount": 3,
  "mealTimes": ["08:00", "13:00", "20:00"],
  "activityLevel": "normal",
  "userNote": "最近体重记录比较稳定"
}
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "recommendationId": "rec-20260823-001",
    "source": "rule_with_ai_explanation",
    "policyVersion": "v1",
    "dailyEstimate": {
      "targetCalories": 308.0,
      "targetAmount": 80.0,
      "unit": "g",
      "isEstimate": true,
      "formulaNote": "基于当前宠物资料和食品包装营养字段估算，请由用户确认"
    },
    "items": [
      {"itemKey": "meal-1", "foodId": 301, "foodName": "室内成猫鸡肉配方", "mealTime": "08:00", "amount": 27, "unit": "g", "percentage": 33.75},
      {"itemKey": "meal-2", "foodId": 301, "foodName": "室内成猫鸡肉配方", "mealTime": "13:00", "amount": 27, "unit": "g", "percentage": 33.75},
      {"itemKey": "meal-3", "foodId": 302, "foodName": "新粮", "mealTime": "20:00", "amount": 26, "unit": "g", "percentage": 32.5}
    ],
    "explanations": ["当前食品的能量字段可用", "建议先观察实际喂食量和体重变化"],
    "warnings": ["这不是处方或固定减重方案", "宠物资料或营养字段不完整时结果可能不完整"]
  }
}
```

如果关键字段缺失，接口仍可返回用户输入的计划草稿，但 `targetCalories=null`，并列出 `missingFields`，不能让 Python 估算一个看似精确的数字。

#### 7.5.2 保存用户确认后的计划

```http
POST /api/feeding-plans
```

请求：

```json
{
  "petId": 1,
  "name": "豆包八月日常喂食",
  "startDate": "2026-08-24",
  "endDate": null,
  "dailyTargetCalories": 308.0,
  "dailyTargetAmount": 80,
  "unit": "g",
  "recommendationSource": "ai_assisted",
  "policyVersion": "v1",
  "items": [
    {"itemKey": "meal-1", "foodId": 301, "mealTime": "08:00", "amount": 27, "unit": "g", "percentage": 33.75},
    {"itemKey": "meal-2", "foodId": 301, "mealTime": "13:00", "amount": 27, "unit": "g", "percentage": 33.75},
    {"itemKey": "meal-3", "foodId": 302, "mealTime": "20:00", "amount": 26, "unit": "g", "percentage": 32.5}
  ],
  "notes": "用户确认后启用"
}
```

响应：返回 `status=active` 的计划详情和计划项。保存接口必须重新读取食品卡并校验食品归属，不能信任推荐响应中的名称和营养字段。

#### 7.5.3 计划列表和详情

```http
GET /api/feeding-plans?petId=1&status=active
GET /api/feeding-plans/601
```

计划详情响应至少包含：计划状态、版本、每日目标、食品卡摘要、计划项、公式版本、确认时间和提示语。

#### 7.5.4 查询今日清单

```http
GET /api/feeding-plans/601/today?date=2026-08-24
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "planId": 601,
    "date": "2026-08-24",
    "status": "active",
    "items": [
      {
        "planItemId": 701,
        "itemKey": "meal-1",
        "foodId": 301,
        "foodName": "室内成猫鸡肉配方",
        "plannedAt": "2026-08-24T08:00:00",
        "amount": 27,
        "unit": "g",
        "completionStatus": "completed",
        "feedingRecordId": 801,
        "actualFeedingTime": "2026-08-24T08:12:00"
      },
      {
        "planItemId": 702,
        "itemKey": "meal-2",
        "foodId": 301,
        "foodName": "室内成猫鸡肉配方",
        "plannedAt": "2026-08-24T13:00:00",
        "amount": 27,
        "unit": "g",
        "completionStatus": "pending",
        "feedingRecordId": null,
        "actualFeedingTime": null
      }
    ],
    "completedCount": 1,
    "totalCount": 3
  }
}
```

首版清单只支持 `pending/completed/missed/skipped` 展示。点击“完成”时调用既有喂食记录接口，写入 `feedingPlanId`、`feedingPlanItemId`、`plannedAt`；不要只在前端修改勾选状态。

#### 7.5.5 记录计划喂食

```http
POST /api/feeding-records
```

Phase 7 扩展后的请求示例：

```json
{
  "petId": 1,
  "feedingTime": "2026-08-24T08:12:00",
  "foodType": "main_food",
  "amount": 27,
  "unit": "g",
  "foodId": 301,
  "feedingPlanId": 601,
  "feedingPlanItemId": 701,
  "plannedAt": "2026-08-24T08:00:00",
  "notes": "按今日计划完成"
}
```

响应继续返回现有喂食记录字段，并增加计划关联字段。重复提交同一个 `feedingPlanItemId + plannedAt` 时必须幂等返回已存在记录，不能产生重复完成事件。

### 7.6 动态喂养分析

#### 7.6.1 查询分析

```http
GET /api/feeding-analysis?petId=1&startDate=2026-08-01&endDate=2026-08-23
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "petId": 1,
    "range": {"startDate": "2026-08-01", "endDate": "2026-08-23"},
    "facts": {
      "feedingDays": 18,
      "averageDailyAmount": 76.4,
      "unit": "g",
      "plannedDays": 12,
      "planCompletionRate": 0.83,
      "weight": {"firstKg": 4.20, "lastKg": 4.28, "changeKg": 0.08, "sampleCount": 4},
      "dailyStatus": {"appetiteNormalDays": 16, "softStoolDays": 2, "refusalDays": 1}
    },
    "observations": [
      {"key": "amount_vs_weight", "text": "记录期间喂食量和体重均有记录，可继续观察趋势", "evidence": ["feeding_record", "weight_record"]},
      {"key": "plan_completion", "text": "有部分计划时段未完成记录，不能据此判断实际未喂食", "evidence": ["feeding_plan", "feeding_record"]}
    ],
    "reviewSuggestions": [
      "复核食品包装上的建议喂食量和当前活动变化",
      "如果软便或拒食持续，建议咨询兽医"
    ],
    "limitations": ["未记录不等于未发生", "结果是数据观察，不是诊断或减量处方"],
    "analysisSource": "java_stats_with_ai_explanation",
    "policyVersion": "v1"
  }
}
```

---

## 8. Python AI 实施设计

### 8.1 模块拆分

建议新增：

```text
fafa-python/app/api/foods.py
fafa-python/app/schemas/food.py
fafa-python/app/service/food_recognition_service.py
fafa-python/app/service/feeding_recommendation_service.py
fafa-python/app/service/feeding_analysis_service.py
fafa-python/app/agent/tools/food_tools.py
```

`main.py` 增加食品 AI router，但保持所有业务写入仍由 Java 完成。

### 8.2 食品识别输入/输出

Java → Python：

```http
POST /api/foods/recognize
X-Internal-Token: {internal-token}
```

```json
{
  "taskId": 401,
  "userId": 3,
  "petId": 1,
  "mediaId": 201,
  "imageUrl": "http://object-storage/fafa-media/food-package-201.jpg",
  "traceId": "trace-20260823-001",
  "promptVersion": "food-recognition-v1"
}
```

Python → Java：

```json
{
  "taskId": 401,
  "status": "completed",
  "model": "configured-vision-model",
  "promptVersion": "food-recognition-v1",
  "draft": {
    "name": {"value": "室内成猫鸡肉配方", "confidence": 0.94},
    "brand": {"value": "示例品牌", "confidence": 0.88},
    "foodType": {"value": "main_food", "confidence": 0.72},
    "proteinPercent": {"value": 38.0, "confidence": 0.97},
    "ingredients": {"value": ["鸡肉", "鸡肉粉"], "confidence": 0.78}
  },
  "warnings": ["部分文字模糊"],
  "usage": {"promptTokens": 900, "completionTokens": 300},
  "traceId": "trace-20260823-001"
}
```

Python schema 要求：

- `extra="forbid"` 或等价的未知字段拒绝策略。
- 所有数值字段设置范围校验。
- `confidence` 范围为 0～1。
- 解析失败时返回结构化错误，不返回半成品 JSON。
- 模型没有读到的字段必须省略或为 `null`。

### 8.3 模型提示词约束

食品包装识别提示词必须明确：

1. 只读取包装中可见的名称、品牌、类别、规格、保证分析、能量、配料和阶段标注。
2. 看不清或没有出现的字段返回 `null`，禁止根据常识补全。
3. 数值必须附带原始单位和依据，如每 100g、每 100ml 或每份。
4. 不输出“适合治疗某病”“一定安全”“需要换粮”等医疗或绝对判断。
5. 输出固定 JSON schema，不输出 Markdown、解释段落或额外字段。

### 8.4 喂食建议策略

喂食建议分两层：

1. Java `FeedingRecommendationPolicy`：校验必填事实、计算确定性统计、应用配置边界。
2. Python `FeedingRecommendationService`：基于 Java 提供的宠物和食品事实，生成可读说明或候选分配，不拥有最终写权限。

建议采用配置版本化的估算公式：

```text
RER = 70 × weightKg^0.75
estimatedDailyCalories = RER × activityOrStageFactor
estimatedFoodAmount = estimatedDailyCalories ÷ kcalPerUnit
```

要求：

- `weightKg`、食品能量和阶段因子缺失时，返回 `null` 并说明缺失原因。
- 因子、最小/最大值和小数精度由 `FeedingRecommendationPolicy` 配置，不写死在 Python prompt 或 controller。
- 结果字段必须标记 `isEstimate=true`，用户可以修改总量、餐数、时段和食品组合。
- 保存计划时重新从 Java 数据库读取食品营养和宠物资料，不能直接保存 Python 返回的数值。
- 公式仅用于数据估算，不输出减重处方和固定“减少多少克”的指令。

### 8.5 Agent 只读工具

扩展 `JavaQueryClient`、`ToolRegistry` 和意图规划：

```text
getFoodInfo           查询宠物食品卡和营养字段
getFeedingPlan        查询当前/历史喂食计划
getFeedingHistory     已有工具继续复用，增加食品名称和计划关联
getFoodTransition     查询换粮计划和反应
getFeedingAnalysis    查询 Java 计算的动态喂养统计
```

内部接口建议：

```http
GET /internal/ai/pets/{petId}/foods?userId=3&status=active
GET /internal/ai/pets/{petId}/feeding-plans?userId=3&status=active
GET /internal/ai/pets/{petId}/food-transitions?userId=3&status=active
GET /internal/ai/pets/{petId}/feeding-analysis?userId=3&startDate=...&endDate=...
```

工具只返回结构化事实和来源 ID。`graph.py` 的安全检查增加食品边界：不能因为用户询问食品就输出治疗、诊断、处方或确定性的减量指令。

### 8.6 Python 失败策略

| 失败 | Java 返回 | 是否扣识别配额 |
|---|---|---|
| 图片不存在/无权限 | 参数或资源不存在 | 否 |
| 图片格式不支持 | 参数错误 | 否 |
| Python 超时 | `AI_SERVICE_UNAVAILABLE`，任务 `failed` | 否，释放 |
| 模型返回无效 JSON | 任务 `failed`，提示重新拍摄/手工录入 | 否，释放 |
| 识别成功但用户拒绝 | `rejected` | 是 |
| 分析自然语言失败 | 返回 Java 统计，说明字段为空 | 不涉及识别配额 |

---

## 9. 配方信息提示分规则

### 9.1 评分原则

首版使用 4 个维度、总分 100：

| 维度 | 分值 | 判定重点 |
|---|---:|---|
| 蛋白来源清晰度 | 25 | 配料表是否能识别出明确来源，是否只有模糊“肉类及其制品” |
| 添加物提示 | 25 | 是否出现 BHA/BHT/人工色素等需用户关注的字段；只做提示，不宣称危害 |
| 钙磷信息 | 25 | 两项是否同时提供；有值时展示比例，缺失时标为未知 |
| 阶段/适配信息完整度 | 25 | 是否有幼年/成体/老年、全价等包装标注，结合宠物基本资料做信息匹配 |

评分状态：

```text
known    证据充分
partial  有部分信息
unknown  缺失，不能可靠打分
```

任一关键维度 `unknown` 时，仍可以展示其余维度雷达图，但总分返回 `null` 或标记 `incomplete=true`，具体阈值由产品验收确认。推荐默认：四维都至少 `partial` 才返回总分。

### 9.2 钙磷比例

仅在钙、磷都存在且磷大于 0 时计算：

```text
caToP = calciumPercent / phosphorusPercent
```

分数依据和可接受区间必须放入 `FoodScorePolicy` 配置并带版本；不在页面或 Python 中重复一套判断。若单位、基础或数值不可信，返回 `unknown`，不把异常比值解释成健康风险。

### 9.3 评分说明文案

所有页面和 API 都带固定声明：

```text
该结果仅根据当前已录入的包装与营养信息生成，用于辅助阅读食品标签，
不代表食品安全认证，也不是疾病诊断、治疗或个体化处方建议。
```

### 9.4 评分测试样例

- 完整包装数据、明确蛋白来源、无标记添加物、钙磷齐全、阶段清晰：四维均为 `known`，返回可解释总分。
- 缺失磷值：钙磷维度为 `unknown`，总分不应伪装为低分。
- 配料只有“肉类及其制品”：蛋白来源维度 `partial`，证据中展示原文。
- 识别字段包含未知添加物：只进入 `unknown` 或 `warnings`，不得抛异常或判断为有害。
- 自制食品无包装营养信息：允许建卡，返回数据不足。

---

## 10. 小程序实施设计

### 10.1 页面和路由

建议新增：

```text
fafa-miniapp/pages/food/index          食品卡列表、筛选、入口
fafa-miniapp/pages/food/form           手工创建/编辑食品卡
fafa-miniapp/pages/food/recognition    识别任务状态、草稿确认
fafa-miniapp/pages/food/detail         食品详情、提示分雷达
fafa-miniapp/pages/food/transition     换粮计划和反应
fafa-miniapp/pages/feeding-plan/index  计划列表、今日完成率
fafa-miniapp/pages/feeding-plan/form   创建/编辑/确认计划
fafa-miniapp/pages/feeding-plan/today  今日喂食清单
fafa-miniapp/pages/feeding-plan/analysis 动态喂养分析
```

同时在首页增加“今日喂食”入口或卡片，展示未完成数量；不把首页做成第二套计划编辑页。

### 10.2 食品识别交互

```text
选择宠物
  → 选择已有图片或拍摄包装图
  → 上传为 media_asset
  → 点击“识别包装”
  → 展示识别中和本月剩余次数
  → 草稿字段逐项显示置信度/需确认标识
  → 用户修改并确认
  → 保存食品卡并进入详情
```

必须有：

- 配额不足时的明确提示和手工录入入口。
- 图片模糊/字段缺失时的手工编辑入口。
- 确认按钮二次防抖，避免重复创建。
- 识别结果页显示“这是候选信息，请核对包装原文”。

### 10.3 今日清单交互

- 默认打开当前日期，可切换前后 7 天。
- 每个计划项只保留一个主操作“完成”，完成后显示真实记录时间。
- 完成操作失败时保留未完成状态并展示错误，不在本地静默勾选。
- 允许从今日清单跳转编辑量和备注，但修改真实记录仍调用喂食记录接口。
- 未完成不自动等同于漏喂；分析页面必须说明“未记录不等于未发生”。

### 10.4 无障碍与数据提示

- 颜色不是唯一状态表达，完成/待完成/跳过同时显示文字和图标。
- 雷达图之外提供维度列表和文字说明。
- 所有估算值显示“估算”“来源”“公式版本”。
- 日期、时区和喂食时间沿用当前小程序的本地时间约定。

---

## 11. 测试计划

### 11.1 Java 领域单元测试

新增测试：

```text
FoodCardTest
FoodNutritionProfileTest
FoodRecognitionTaskTest
FoodTransitionPlanTest
FeedingPlanTest
FoodSafetyScoreServiceTest
NutritionNormalizerTest
FeedingRecommendationPolicyTest
FeedingAnalysisServiceTest
```

必须覆盖：

- 食品名称、百分比、价格、日期、单位和类型边界。
- 缺失营养字段与 0 值的区别。
- 识别任务状态非法跳转、重复确认和重复拒绝。
- 换粮比例逐日连续、总和 100、日期范围和反应代码。
- 计划项时间重复、数量为负、食品跨宠物、项目数超限。
- 公式在缺失体重、能量、阶段因子时返回数据不足，而不是抛出或虚构结果。
- 评分规则的维度证据、缺失状态、版本和固定声明。

### 11.2 Java 应用服务测试

- 未登录或宠物不属于当前用户时，所有食品/计划资源均不可见。
- 其他用户的 `mediaId` 不能创建识别任务。
- 非图片媒体不能进入包装识别。
- 同幂等键不重复扣配额、不重复调用 Python。
- Python 成功/失败/超时对应任务状态和配额释放正确。
- 确认识别结果创建食品卡失败时，任务和食品卡事务不产生半成品。
- 归档中的食品卡不能进入新计划，但历史喂食仍能查到。
- 今日清单已存在真实记录时重复点击不重复写事件。
- 计划更新不改变历史 `feeding_record` 的食品名称和营养快照。

### 11.3 Repository/迁移集成测试

- 使用测试 MySQL 执行 V1.1～V7 全量迁移。
- 验证 JSON 字段读写、空值、百分比精度和时间字段。
- 验证列表过滤、计划项排序、喂食范围和索引查询。
- 验证逻辑删除不会从历史关联中物理消失。
- 验证并发识别配额和识别任务唯一键。

### 11.4 Python 测试

新增：

```text
tests/test_food_recognition_schema.py
tests/test_food_recognition_service.py
tests/test_feeding_recommendation_service.py
tests/test_food_tools.py
```

覆盖：

- 合法包装 JSON 解析。
- 模型输出 Markdown、缺字段、额外字段、错误类型、超范围数值。
- 看不清的字段保持 `null`。
- 模型超时和 API 错误映射为可识别异常。
- Java 工具携带 `user_id`、`pet_id`、日期范围和 traceId。
- Agent 对食品问题仍遵守医疗边界，不输出诊断、处方和确定性减量。

### 11.5 接口契约测试

至少固化以下契约：

1. Java 创建识别任务 → Python 识别请求。
2. Python 识别结果 → Java 草稿保存。
3. Java 计划推荐请求 → Python 建议响应。
4. Java Agent 只读食品接口 → Python 工具 schema。
5. 小程序完成今日喂食 → Java `feeding-records` 扩展字段。

契约测试禁止依赖真实大模型；使用固定 fixture 和 mock gateway。真实模型只放在手工联调脚本，不作为 CI 必需条件。

### 11.6 端到端验收脚本

```text
1. 登录并创建/选择宠物。
2. 上传食品包装图片，确认 mediaId。
3. 创建识别任务，验证普通用户配额从 5 变 4。
4. 查询草稿，检查缺失字段为 null，修改一项后确认。
5. 查询食品卡和提示分。
6. 创建 7 天换粮计划，录入 soft_stool，检查每日状态只是关联展示。
7. 请求喂食建议，修改一餐数量后保存计划。
8. 查询今日清单，完成一个计划项并检查 feeding_record。
9. 重复完成同一计划项，确认没有第二条事件。
10. 查询 30 天动态分析，检查体重、喂食和状态证据。
11. 在 AI 对话中询问“这段时间吃了多少”，检查引用食品/喂食事实。
```

---

## 12. 配置、部署与可观测性

### 12.1 Java 配置项

在 `application-dev.yml`、`application-test.yml`、`application-prod.yml` 中配置：

```yaml
fafa:
  quota:
    food-recognition:
      monthly-normal: 5
      monthly-vip: 50
  food:
    recognition:
      timeout-seconds: 90
      draft-retention-days: 7
    transition:
      min-days: 3
      max-days: 30
    score:
      policy-version: v1
  feeding:
    max-range-days: 366
    max-items-per-plan: 12
    recommendation-timeout-seconds: 45
    policy-version: v1
```

配置绑定字段命名要与现有 `FaFaProperties` 风格一致；如果 Spring 对连字符和内部类绑定存在差异，以启动测试验证为准。

### 12.2 Python 配置项

```env
FOOD_RECOGNITION_PROMPT_VERSION=food-recognition-v1
FOOD_RECOGNITION_MAX_IMAGE_BYTES=10485760
FOOD_RECOGNITION_TIMEOUT_SECONDS=90
FEEDING_RECOMMENDATION_PROMPT_VERSION=feeding-recommendation-v1
```

模型名继续从统一 `app/core/llm.py` / settings 路由读取。业务代码禁止直接写 `qwen...` 字符串，模型切换只改配置和路由。

### 12.3 日志和指标

日志必须包含：

```text
traceId, userId, petId, taskId/planId, mediaId, status, policyVersion, elapsedMs
```

不要记录：

- `Authorization`、内部令牌、完整对象存储签名 URL。
- 包装图片 Base64、模型完整 prompt、可能包含隐私的原始图片内容。
- 未脱敏的 Python 异常堆栈到用户响应。

建议指标：

```text
fafa_food_recognition_total{status}
fafa_food_recognition_latency_ms
fafa_food_recognition_quota_released_total{reason}
fafa_food_score_incomplete_total
fafa_feeding_plan_created_total{source}
fafa_feeding_checklist_completion_rate
fafa_feeding_analysis_total{source,status}
```

### 12.4 部署顺序

```text
1. 备份数据库并在测试库执行 V7
2. 部署 Java，确认迁移和旧接口健康
3. 部署 Python 新 schema/service/router
4. 开启食品识别入口，先使用低并发配置
5. 发布小程序页面
6. 运行端到端验收和配额并发测试
7. 观察识别失败率、超时、草稿确认率和计划完成率
```

Python 旧版本在 Java 新版本前不可直接接收食品识别请求；建议先部署兼容的新 Python，再启用 Java 功能开关。不存在食品卡数据时，旧喂食接口必须继续正常工作。

---

## 13. 实施分解与提交顺序

### Phase 7.0：基线冻结与契约先行

- 固定现有食品类型、喂食记录字段和统一响应格式。
- 增加接口 DTO/schema 草案和 JSON fixture。
- 明确配置键、错误码、状态码和数据字典。
- 记录旧接口兼容测试，确保后续修改不会破坏 Phase 2/6。

### Phase 7.1：食品卡与 V7 数据库

- 编写 V7 迁移。
- 实现 `FoodCard`、`FoodNutritionProfile`、仓储接口和 MyBatis 实现。
- 实现食品卡 CRUD、归档和归属校验。
- 扩展 `FoodType` 兼容策略和 `feeding_record` 映射。
- 完成 Java 领域和 repository 测试。

### Phase 7.2：包装识别与配额

- 实现 `FoodRecognitionTask` 和任务仓储。
- 扩展 `QuotaApplicationService`，增加 food recognition 获取/释放/查询。
- 新增 Java Python gateway 和 Python food recognition service。
- 接入现有媒体素材 URL，完成草稿保存、查询、确认、拒绝。
- 完成幂等、失败释放和并发配额测试。

### Phase 7.3：配方提示分与换粮

- 实现 `FoodSafetyScoreService` 和 `FoodScorePolicy`。
- 完成维度证据、缺失字段和固定免责声明。
- 实现 `FoodTransitionPlan`、V7 表、接口和 daily status 关联展示。
- 完成典型食品和数据不足样例测试。

### Phase 7.4：喂食计划与今日清单

- 实现 `FeedingPlan`、`FeedingPlanItem` 和仓储。
- 扩展 `feeding_record` 的计划关联字段和幂等策略。
- 实现建议预览、用户编辑、确认保存、暂停/恢复、今日清单。
- 首版不新增提醒任务；产品确认需要通知后再复用现有提醒模型。

### Phase 7.5：动态分析与 Agent 工具

- 实现 Java 确定性喂养统计。
- 实现 Python 说明服务和失败降级。
- 扩展 JavaQueryClient、ToolRegistry、意图规划和安全检查。
- 增加食品卡、计划、换粮和分析只读内部接口。

### Phase 7.6：小程序交互

- 完成食品卡列表/编辑/识别/详情。
- 完成提示分文字和雷达展示。
- 完成换粮过渡和反应录入。
- 完成计划推荐/编辑/确认、今日清单、分析页。
- 首页接入待完成喂食数量。

### Phase 7.7：联调、文档和发布

- 执行 Java、Python、数据库、契约和小程序检查。
- 更新接口文档、数据库文档、系统架构、开发交接文档和阶段计划。
- 运行端到端验收脚本。
- 记录模型识别失败样例和人工修正率，为后续模型升级提供数据，但不在本阶段训练模型。

每个子阶段都应保持可编译、可测试、可回滚；不要一次提交跨越数据库、Java、Python 和小程序的不可验证大改动。

---

## 14. 错误码建议

复用现有 `ErrorCode`，只新增确有需要的业务错误：

| 错误码语义 | 触发条件 |
|---|---|
| `FOOD_NOT_FOUND` | 食品不存在、已删除或不属于当前用户 |
| `FOOD_IN_ACTIVE_PLAN` | 归档/删除仍被生效计划使用 |
| `FOOD_FIELD_INVALID` | 营养、价格、日期、单位或类型不合法 |
| `FOOD_MEDIA_INVALID` | 识别输入不是当前用户的图片素材 |
| `FOOD_RECOGNITION_QUOTA_EXCEEDED` | 本月识别次数已用完 |
| `FOOD_RECOGNITION_NOT_READY` | 任务仍处理中，不能确认 |
| `FOOD_RECOGNITION_EXPIRED` | 草稿已过期 |
| `FOOD_RECOGNITION_CONFIRM_CONFLICT` | 任务已被其他确认请求完成 |
| `FOOD_TRANSITION_INVALID` | 两粮归属、日期或比例不合法 |
| `FEEDING_PLAN_NOT_FOUND` | 计划不存在或无权访问 |
| `FEEDING_PLAN_INVALID` | 计划项、数量、时间或食品组合不合法 |
| `FEEDING_RECORD_DUPLICATE` | 同一计划项已经完成 |
| `FEEDING_ANALYSIS_RANGE_INVALID` | 日期为空、倒序或超出上限 |
| `AI_SERVICE_UNAVAILABLE` | Python 超时或不可用 |

不要把 Python 原始错误文本作为业务错误码，也不要为了省事把所有食品错误都返回 `PARAM_INVALID`，否则前端无法区分重试、手工录入和资源不存在。

---

## 15. 风险与处理方案

| 风险 | 影响 | 处理方案 |
|---|---|---|
| 包装文字模糊、角度不全 | 营养字段错误 | 置信度和缺失字段可见；确认前必须人工核对；失败可手工录入 |
| 每 100g、每份、每 100ml 混淆 | 喂食估算失真 | 保存 `nutritionBasis` 和原单位；不确定时禁止换算 |
| 配料表存在别名 | 蛋白来源误判 | 规则只输出可解释的原文和 `partial`，不强行归类 |
| 用户把提示分当成安全认证 | 产生误导 | 固定免责声明、维度证据、数据不足状态和文案评审 |
| AI 建议未经确认直接落库 | 事实污染 | Python 无写权限；推荐接口只返回预览；保存时 Java 重新校验 |
| VIP/普通配额并发超发 | 会员权益错误 | Redis 原子 INCR/回滚；幂等任务；并发测试 |
| 食品卡删除后历史记录断链 | 统计无法解释 | 逻辑删除 + `foodNameSnapshot` + 历史记录不级联删除 |
| 计划清单和实际记录不一致 | 用户误以为已喂食 | 以 Java `feeding_record` 为事实；前端只提交动作，不保存本地勾选 |
| 体重样本太少 | 趋势结论不可靠 | 返回 sampleCount 和 limitations，样本不足只展示事实 |
| 新旧 food type 不兼容 | Phase 2 接口回归 | 接受旧 `can`，新食品卡使用 `canned`，补回归测试 |
| 模型升级改变结果结构 | 任务解析失败 | prompt/schema 版本化，未知字段拒绝，保留 fixture 契约测试 |
| Python 对象存储 URL 不可达 | 识别失败 | 沿用媒体 URL 预签名/内网转换能力，失败时返回可重试状态 |

---

## 16. 验收清单

### 16.1 功能验收

- [ ] 每只宠物可以新建多张食品卡。
- [ ] 食品卡支持主食、罐头、零食、补充剂、自制和其他类型。
- [ ] 手工创建、更新、归档和逻辑删除可用。
- [ ] 包装图片必须先成为 `media_asset`，识别任务不可读取其他用户媒体。
- [ ] 普通用户每月 5 次、VIP 每月 50 次识别配额可显示、扣减和释放。
- [ ] 识别结果以草稿展示，用户确认后才创建食品卡。
- [ ] 识别失败不占用最终配额，重复确认不创建重复食品卡。
- [ ] 配方提示分展示雷达、维度证据、缺失字段、版本和免责声明。
- [ ] 换粮计划支持每日比例、目标量、反应记录和 daily status 关联展示。
- [ ] 喂食计划支持多食品、多时段、每日数量、用户调整和确认。
- [ ] 今日清单完成操作最终落为 `feeding_record`。
- [ ] 重复完成计划项不重复产生实际记录。
- [ ] 动态分析能展示喂食量、计划完成、体重和状态的事实及限制。
- [ ] Agent 可以查询食品和计划事实，不能输出诊断/处方式回答。

### 16.2 技术验收

- [ ] V7 迁移可在已有数据上升级，旧接口回归通过。
- [ ] 领域层没有 Spring、MyBatis、Redis、HTTP 或 Python 依赖。
- [ ] Java 是食品和计划事实唯一写入方。
- [ ] Python 模型输出经过 schema、范围和来源校验。
- [ ] 所有公开资源检查当前用户和宠物归属。
- [ ] 任务状态、计划状态、版本和幂等键有测试。
- [ ] 配额并发场景没有超发和负数。
- [ ] 关键日志包含 traceId/taskId/planId，但不泄漏 token、签名 URL 和 Base64。
- [ ] Java 测试、Python 测试、编译检查和接口契约测试全部通过。

### 16.3 产品边界验收

- [ ] 页面明确标注 AI 识别候选结果和估算值。
- [ ] 页面明确说明提示分不是安全认证。
- [ ] 页面没有“必须减多少克”“治疗”“确诊”等确定性医疗文案。
- [ ] 数据缺失时显示“数据不足”，不以 0 分或虚构值替代。
- [ ] 用户可以跳过 AI，完整使用手工食品卡和手工喂食记录。

---

## 17. 文档同步清单

Phase 7 实施完成后必须同步：

1. `docs/接口文档.md`：增加食品卡、识别、提示分、换粮、喂食计划和动态分析的接口定义、地址、请求 JSON、响应 JSON。
2. `docs/MySQL数据库设计.md`：增加 V7 表、字段、索引、状态和历史兼容说明。
3. `docs/系统架构说明.md`：增加“Java 事实写入、Python 识别/说明、媒体引用和配额”链路。
4. `docs/Codex开发交接文档.md`：记录 V7 迁移、启动方式、配置键、测试命令、已知限制和下一阶段入口。
5. `docs/分阶段开发计划.md`：将 Phase 7 从概要更新为已完成/联调状态，并记录实际偏差。
6. `docs/dev/phase7-implementation-plan.md`：实施后把状态改为“核心代码已实施，进入联调与验收”，勾选验收结果并记录未完成项。

---

## 18. 开发完成后的自检

提交前按以下顺序自检：

```text
1. 先看 git diff，确认没有覆盖用户已有修改。
2. 检查 V7 SQL 在干净库和已有数据上均可执行。
3. 检查 domain → application → infrastructure 依赖方向。
4. 检查所有资源归属和内部接口 userId/petId 校验。
5. 检查 AI 输出是否可能把 null 变成猜测值。
6. 检查识别配额获取、释放、幂等和并发。
7. 检查计划完成是否真实写入 feeding_record。
8. 检查食品归档/删除后历史是否仍可解释。
9. 检查所有估算、评分和分析是否带来源、版本和限制。
10. 运行 Java、Python、数据库迁移、契约和小程序检查。
11. 更新接口、数据库、架构和交接文档。
```

Phase 7 的最终交付不是“接入一个视觉模型”，而是一个用户可确认、事实可追溯、建议可解释、失败可降级的食品与喂食闭环。任何无法通过归属校验、结构化校验、用户确认或安全边界检查的结果，都必须停留在草稿/提示层，不能写入宠物事实。

---

## 19. 实施结果与联调入口

### 19.1 已落地代码

- Java：`V7__create_food_and_feeding_plan_tables.sql`、食品卡/识别任务/换粮/喂食计划领域模型、MyBatis 仓储、应用服务和公开接口。
- Java：食品识别月度配额、幂等键、失败释放、来源任务唯一约束、用户/宠物/媒体归属检查。
- Java：食品配方提示分、动态喂养事实统计、食品卡和喂食计划 Agent 只读工具。
- Python：`POST /api/foods/recognize`，包含图片下载大小限制、视觉模型超时、JSON 结构化解析和字段白名单清洗。
- 小程序：食品列表/包装识别最小闭环、今日喂食清单入口和计划完成写入既有喂食记录。

### 19.2 已执行检查

| 检查 | 结果 |
|---|---|
| `mvn -q -DskipTests compile` | 通过 |
| `mvn -q test` | 通过 |
| `python -m compileall -q app` | 通过 |
| 小程序 JavaScript `node --check` | 通过 |
| `git diff --check` | 无代码空白错误；仅有换行符提示 |
| `python -m pytest -q` | 当前机器缺少 `dashscope`、`fastapi`、`pydantic-settings`、`qdrant-client`，在测试收集阶段阻断 |

### 19.3 联调顺序

1. 执行 Flyway V7，确认旧 `feeding_record` 数据可读，检查新增字段和索引。
2. 启动 Java、Redis、MinIO 和 Python，使用接口文档 7.1～7.9 的顺序验证食品卡、识别、确认、换粮、喂食计划和分析。
3. 连续执行普通用户第 6 次识别，确认返回 `FOOD_RECOGNITION_QUOTA_EXCEEDED`；让 Python 超时，确认任务失败并释放配额。
4. 对同一识别任务并发确认，确认只生成一张来源任务一致的食品卡。
5. 对被 active 喂食计划或 active 换粮计划引用的食品卡执行归档/删除，确认返回 `FOOD_IN_ACTIVE_PLAN`。
6. 安装 `fafa-python/requirements.txt` 后重新执行 Python 全量测试，并在真实百炼视觉模型下保存一组脱敏响应 fixture。
