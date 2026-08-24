# Phase 8 实施文档：养宠记账与支出统计

> 依据：《FaFa产品功能设计-最终版.md》第九章“养宠记账”；《分阶段开发计划.md》Phase 8；《.agent/development-guidelines.md》；Phase 6 医疗档案与 Phase 7 食品管理的已实现代码基线。
>
> 计划日期：2026-08-23
>
> 状态：**核心代码已实施，进入联调与验收**
>
> 前置假设：Phase 0～7 的核心代码已经完成，Java 仍是业务事实的唯一写入方；Phase 6 已保存就诊费用，Phase 7 已保存食品卡价格和食品来源媒体，Phase 4 已提供媒体上传与用户归属校验能力。

本阶段只实现“记一笔—查账—看统计—同步医疗费用”的可部署闭环。记账金额、分类和日期是用户确认的支出事实；统计只对已保存的有效账单做确定性计算，不引入 AI 猜测、预算建议或货币换算。

---

## 1. 阶段目标与完成定义

### 1.1 用户目标

| 能力 | 用户结果 | 主要事实来源 |
|---|---|---|
| 快速记账 | 3 秒内录入金额、分类和日期，可补备注、票据照片 | `expense_record` |
| 支出分类 | 食品、医疗、用品、美容、保险、其他六类 | `expense_record.category` |
| 账单管理 | 按宠物、日期、分类查看；可修改和逻辑删除手工账单 | `expense_record` |
| 月度统计 | 查看本月总支出、分类占比和每日趋势 | Java 确定性统计服务 |
| 年度统计 | 查看本年总支出、分类占比和月度趋势 | Java 确定性统计服务 |
| 医疗联动 | 就诊档案填写费用后自动生成/更新医疗支出 | `medical_visit.total_cost` → `expense_record` |
| 食品联动 | 记食品支出时可选择食品卡，自动带出名称和当前价格作为录入参考 | `food_card` |
| 后续报告素材 | 为 Phase 9 年报提供按宠物和时间范围汇总的只读事实 | 内部只读接口 |

### 1.2 阶段完成定义

以下链路在开发环境跑通，并通过自动化测试、迁移检查和手工验收后，才认为 Phase 8 核心完成：

```text
用户登录
  → 选择宠物
  → 输入金额与支出分类
  → 可选关联食品卡或票据图片
  → Java 校验归属并保存 expense_record
  → 账单列表可查询/编辑/逻辑删除
  → 月度/年度统计返回总额、分类占比和趋势
  → 创建或修改就诊档案费用
  → Java 在同一事务中幂等同步医疗账单
```

### 1.3 本阶段不做

- 不实现预算、预算超支提醒、分期、AA、报销或保险理赔流程。
- 不实现多币种汇率换算；首版只接受 `CNY`，数据库保留 `currency` 便于后续扩展。
- 不自动把食品卡价格变成购买支出；食品卡价格只是参考值，用户必须主动确认并提交一笔账单。
- 不对票据图片做 OCR 或 AI 金额识别；Phase 4 的媒体上传仅用于保存票据关联。
- 不生成周报、月报、年报和小红书文案；Phase 9 通过本阶段只读事实接口读取统计数据。
- 不允许 Python 直接写账单，也不让 AI 修改金额或分类。

---

## 2. 产品规则落地

### 2.1 支出分类

分类使用稳定的英文 code，页面展示中文 label，数据库使用 `VARCHAR` 而不是 MySQL `ENUM`，便于后续新增分类而不修改表结构。

| code | label | 说明 |
|---|---|---|
| `food` | 食品 | 猫粮、罐头、零食、营养补充品；可关联 Phase 7 食品卡 |
| `medical` | 医疗 | 就诊、检查、药品和治疗相关支出；就诊费用自动进入该类 |
| `supplies` | 用品 | 猫砂、窝垫、玩具、清洁用品等 |
| `grooming` | 美容 | 洗护、剪毛、护理等 |
| `insurance` | 保险 | 宠物保险保费等 |
| `other` | 其他 | 无法归入前五类的支出 |

### 2.2 金额规则

- 金额必须大于 `0`，精度固定为两位小数，Java 使用 `BigDecimal`，数据库使用 `DECIMAL(12,2)`。
- 首版只接受 `CNY`，服务端统一转为大写，默认值由 `fafa.expense.default-currency` 提供。
- 单笔金额上限由 `fafa.expense.max-amount` 配置，默认 `1000000.00`；超过上限返回业务错误，不截断、不四舍五入到上限。
- 统计金额全部以数据库中的已保存金额为准，不使用前端浮点计算结果。

### 2.3 日期规则

- `expenseDate` 是支出发生日期，不能为空，不能晚于今天。
- 手工记账允许录入过去日期，默认页面使用今天。
- 医疗自动账单使用就诊档案的 `visitDate`，不使用创建时间。
- 统计请求使用闭区间 `[startDate, endDate]`，月度为自然月，年度为自然年。
- 所有日期均使用 Java `LocalDate`，不在金额统计中混用 UTC 时间。

### 2.4 来源与可编辑性

| 来源 | `sourceType` | 生成方式 | 是否可编辑 |
|---|---|---|---|
| 手工账单 | `manual` | `POST /api/expenses` | 金额、分类、日期、备注、食品卡、票据均可改 |
| 就诊费用 | `medical_visit` | 医疗档案创建/修改时自动同步 | 不允许通过普通账单接口改金额和分类 |

食品卡不是账单来源类型。手工账单可以携带 `foodCardId`，表示这笔支出购买了哪张食品卡；同一食品卡可以对应多笔购买记录，不能使用食品卡 ID 作为账单唯一键。

### 2.5 删除规则

- 手工账单使用逻辑删除，删除后不参与列表和统计，但数据库记录保留以便审计。
- 医疗自动账单不提供单独删除接口；删除就诊档案时同步逻辑删除该医疗账单。
- 如果就诊费用先被清空后又重新填写，必须恢复并更新同一条来源账单，不得因逻辑删除和唯一索引产生插入冲突。
- 删除账单只解除账单与票据素材的关联，不删除 `media_asset` 文件和向量。
- 食品卡或就诊档案被删除后，历史账单保留名称快照和金额，避免统计断链。

---

## 3. 现有系统复用与边界

### 3.1 复用清单

| 已有能力 | Phase 8 用法 |
|---|---|
| Sa-Token 登录态 | 所有公开接口从登录态获取 `userId`，请求体不接受可伪造的 `userId` |
| `PetApplicationService.checkPetOwnership` | 创建、查询、统计前校验宠物属于当前用户 |
| `Result<T>` 与统一异常处理 | 保持已有成功响应和错误响应格式 |
| MyBatis-Plus + Flyway | V8 迁移、PO、Mapper、Repository 实现 |
| Phase 4 `MediaAssetRepository` | 校验票据图片属于当前用户；不复制文件 |
| Phase 6 `MedicalVisit` | 创建/更新/删除就诊档案时同步医疗账单 |
| Phase 7 `FoodCard` | 校验食品卡归属，读取名称快照和价格作为前端默认值 |
| `springdoc` 分组 | 增加 `expense` 接口分组，保持 Knife4j 可测试 |

### 3.2 责任边界

```text
小程序 / API 客户端
        │ 只提交用户意图和表单字段
        ▼
interfaces：请求 DTO、参数校验、Result
        ▼
application：归属校验、事务、来源同步、DTO 转换
        ▼
domain：金额/分类/日期/来源不变式，统计计算
        ▼
infrastructure：MyBatis、Flyway、媒体关联查询
```

- `domain` 不依赖 Spring、MyBatis、Redis、HTTP、微信或 Python。
- `ExpenseRecordRepository` 在 `domain.repository` 定义，MyBatis 实现在 `infrastructure.persistence.repository`。
- `MedicalVisitApplicationService` 可以调用 `ExpenseApplicationService` 完成同步；`ExpenseApplicationService` 不反向依赖医疗应用服务，避免循环依赖。
- Python 仅通过新增内部 GET 接口读取费用事实；不保存金额、不做写操作。

---

## 4. 领域模型设计

### 4.1 `Money` 值对象

建议新增 `com.fafa.domain.model.expense.Money`：

```java
public record Money(BigDecimal amount, String currency) {
    public Money {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("金额必须大于0");
        }
        if (amount.scale() > 2) {
            throw new IllegalArgumentException("金额最多保留两位小数");
        }
        if (currency == null || !currency.equals(currency.toUpperCase(Locale.ROOT))) {
            throw new IllegalArgumentException("货币代码必须为大写");
        }
    }
}
```

实现时不把上限、默认货币和支持币种硬编码到值对象；值对象只保证正数、精度和格式，业务上限由 `ExpensePolicy` 或应用服务配置校验。

### 4.2 枚举

- `ExpenseCategory`：`FOOD`、`MEDICAL`、`SUPPLIES`、`GROOMING`、`INSURANCE`、`OTHER`。
- `ExpenseSourceType`：`MANUAL`、`MEDICAL_VISIT`。
- `ExpenseStatus`：首版可使用 `isDeleted` 布尔值，不额外引入状态机；如果后续需要作废/冲正，再增加明确状态。

枚举必须提供 `getCode()`、`fromCode()`，`fromCode()` 对大小写不敏感，对非法值抛出 `IllegalArgumentException`，由应用层转换为 `EXPENSE_FIELD_INVALID`。

### 4.3 `ExpenseRecord` 聚合根

建议字段：

| 字段 | 类型 | 规则 |
|---|---|---|
| `id` | `Long` | 持久化 ID |
| `userId` | `Long` | 所属用户，不允许修改 |
| `petId` | `Long` | 所属宠物，不允许修改 |
| `category` | `ExpenseCategory` | 六类之一 |
| `money` | `Money` | 金额和货币 |
| `expenseDate` | `LocalDate` | 支出日期，不晚于今天 |
| `title` | `String` | 可空，最多 100 字符 |
| `notes` | `String` | 可空，最多 500 字符 |
| `receiptMediaId` | `Long` | 可空，只引用已有媒体 |
| `foodCardId` | `Long` | 可空，手工食品账单关联 |
| `foodNameSnapshot` | `String` | 关联食品卡时保存名称快照 |
| `medicalVisitId` | `Long` | 医疗自动账单来源，手工账单必须为空 |
| `sourceType` | `ExpenseSourceType` | `manual` 或 `medical_visit` |
| `systemGenerated` | `boolean` | 医疗自动账单为 true |
| `deleted` | `boolean` | 逻辑删除 |
| `createdAt` / `updatedAt` | `LocalDateTime` | 审计字段 |

领域工厂和行为：

```text
ExpenseRecord.createManual(...)
ExpenseRecord.createFromMedicalVisit(...)
ExpenseRecord.updateManual(...)
ExpenseRecord.syncMedicalVisit(...)
ExpenseRecord.delete()
ExpenseRecord.isEditableByUser()
```

关键不变式：

1. 创建手工账单时，`sourceType=manual`、`medicalVisitId=null`、`systemGenerated=false`。
2. 创建医疗账单时，分类强制为 `medical`，来源强制为 `medical_visit`，`medicalVisitId` 必填，不能携带食品卡。
3. `updateManual` 不能被调用到系统生成账单；修改接口二次检查，避免绕过领域行为。
4. `foodCardId` 只允许关联同一用户、同一宠物的食品卡，归档食品卡仍可作为历史购买关联；名称保存为快照。
5. `receiptMediaId` 只允许引用当前用户的媒体素材；账单删除不删除素材。
6. 逻辑删除后的账单不能再次参与统计，也不能被普通更新接口加载。

### 4.4 统计领域服务

建议新增无框架依赖的 `ExpenseStatisticsCalculator`，输入已过滤的有效 `ExpenseRecord` 和统计周期，输出：

- `totalAmount`：周期总支出。
- `categoryBreakdown`：分类金额、笔数、占比。
- `trend`：月度按天，年度按月。
- `currency`：首版固定 CNY。

计算规则：

- 只计算 `deleted=false` 且日期位于闭区间内的记录。
- 分类金额使用 `BigDecimal` 累加，统一 `scale=2`、`RoundingMode.HALF_UP`。
- 占比为 `categoryAmount / totalAmount * 100`，保留两位小数；最后一个分类用差额修正到 `100.00`，避免前端累计显示 `99.99`。
- 趋势中没有记录的日期/月返回 `0.00`，不能省略，否则折线图会错误连接相邻点。
- 没有账单时返回 `totalAmount=0.00`、空分类列表或全零趋势，并附 `dataStatus=empty`，不返回 null 造成前端分支爆炸。

---

## 5. V8 数据库设计

### 5.1 迁移文件

新增：

```text
fafa-java/src/main/resources/db/migration/V8__create_expense_record_table.sql
```

### 5.2 建表草案

```sql
CREATE TABLE `expense_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '支出记录ID',
  `user_id` BIGINT NOT NULL COMMENT '所属用户ID',
  `pet_id` BIGINT NOT NULL COMMENT '所属宠物ID',
  `category` VARCHAR(24) NOT NULL COMMENT 'food/medical/supplies/grooming/insurance/other',
  `amount` DECIMAL(12,2) NOT NULL COMMENT '支出金额，CNY，必须大于0',
  `currency` CHAR(3) NOT NULL DEFAULT 'CNY' COMMENT '货币代码，首版仅支持CNY',
  `expense_date` DATE NOT NULL COMMENT '支出日期',
  `title` VARCHAR(100) DEFAULT NULL COMMENT '支出标题',
  `notes` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `receipt_media_id` BIGINT DEFAULT NULL COMMENT '票据素材ID，不复制文件',
  `food_card_id` BIGINT DEFAULT NULL COMMENT '关联食品卡ID',
  `food_name_snapshot` VARCHAR(100) DEFAULT NULL COMMENT '食品名称快照',
  `medical_visit_id` BIGINT DEFAULT NULL COMMENT '自动同步的就诊档案ID',
  `source_type` VARCHAR(24) NOT NULL DEFAULT 'manual' COMMENT 'manual/medical_visit',
  `system_generated` TINYINT NOT NULL DEFAULT 0 COMMENT '是否系统自动生成',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_expense_medical_visit` (`medical_visit_id`),
  KEY `idx_expense_user_pet_date` (`user_id`, `pet_id`, `expense_date`, `is_deleted`),
  KEY `idx_expense_user_category_date` (`user_id`, `category`, `expense_date`, `is_deleted`),
  KEY `idx_expense_food_card` (`food_card_id`, `expense_date`),
  KEY `idx_expense_receipt_media` (`receipt_media_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='养宠支出记录';
```

### 5.3 数据库设计决策

- 不新增数据库外键，保持现有项目对逻辑删除和跨模块迁移的处理方式；所有来源关系由 Java 归属校验。
- `medical_visit_id` 的唯一索引保证一个就诊档案最多一条自动医疗账单；多个手工账单的该字段为 `NULL`，不冲突。
- `food_card_id` 不加唯一索引，因为同一食品可以多次购买。
- `amount` 不用 `FLOAT` 或 `DOUBLE`，禁止在数据库层存储格式化金额字符串。
- `is_deleted` 与唯一医疗来源索引配合，删除后不重新创建同一来源；医疗档案更新使用原账单行更新，不采用重复插入。
- 票据媒体、食品卡和就诊档案删除时不级联物理删除支出记录，保证历史统计可解释。

### 5.4 迁移检查

1. 在已有 Phase 7 数据库上执行 Flyway V8，确认 `expense_record` 为空表但旧表无变化。
2. 插入两条手工账单、一条医疗来源账单，确认唯一索引只阻止同一就诊来源重复写入。
3. 逻辑删除手工账单，确认列表和统计不再出现；数据库记录仍存在。
4. 检查 `DECIMAL(12,2)`、`utf8mb4`、索引和默认值与 MyBatis PO 一致。
5. 生产环境执行前备份数据库；Flyway 采用前向迁移，不使用 `DROP TABLE` 回滚。

---

## 6. Java 后端实施方案

### 6.1 目录结构

```text
fafa-java/src/main/java/com/fafa/
├── domain/
│   ├── model/expense/
│   │   ├── ExpenseRecord.java
│   │   ├── ExpenseCategory.java
│   │   ├── ExpenseSourceType.java
│   │   ├── Money.java
│   │   └── ExpenseStatisticsCalculator.java
│   └── repository/
│       └── ExpenseRecordRepository.java
├── application/
│   ├── dto/expense/
│   │   ├── ExpenseCommand.java
│   │   ├── ExpenseDTO.java
│   │   ├── ExpenseStatisticsDTO.java
│   │   └── ExpenseCategoryDTO.java
│   └── service/
│       ├── ExpenseApplicationService.java
│       └── ExpenseStatisticsApplicationService.java
├── interfaces/
│   ├── controller/
│   │   ├── ExpenseController.java
│   │   └── InternalAiQueryController.java  # 增加只读费用接口
│   └── dto/expense/
│       ├── SaveExpenseRequest.java
│       ├── ExpenseStatisticsRequest.java
│       └── ExpenseCategoryResponse.java
└── infrastructure/
    └── persistence/
        ├── mapper/ExpenseRecordMapper.java
        ├── po/ExpenseRecordPO.java
        └── repository/ExpenseRecordRepositoryImpl.java
```

### 6.2 Repository 契约

领域仓储接口至少提供以下能力，接口参数必须带用户和宠物边界，不能只按全局 ID 查询：

```java
public interface ExpenseRecordRepository {
    ExpenseRecord save(ExpenseRecord record);
    Optional<ExpenseRecord> findByIdAndUserId(Long id, Long userId);
    List<ExpenseRecord> findByPetIdAndDateRange(Long userId, Long petId,
                                                 LocalDate start, LocalDate end,
                                                 ExpenseCategory category,
                                                 int offset, int limit);
    long countByPetIdAndDateRange(Long userId, Long petId,
                                  LocalDate start, LocalDate end,
                                  ExpenseCategory category);
    List<ExpenseRecord> findActiveByPetIdAndDateRange(Long userId, Long petId,
                                                       LocalDate start, LocalDate end);
    Optional<ExpenseRecord> findByMedicalVisitId(Long userId, Long medicalVisitId);
    Optional<ExpenseRecord> findByMedicalVisitIdIncludingDeleted(Long userId, Long medicalVisitId);
    void deleteManualById(Long id, Long userId);
    void deleteMedicalSource(Long userId, Long medicalVisitId);
}
```

`findByMedicalVisitIdIncludingDeleted` 和 `deleteMedicalSource` 使用 Mapper 的明确 SQL，确保 MyBatis-Plus 逻辑删除条件不会阻止医疗账单恢复或同步；普通列表和统计仍只读取 `is_deleted=0`。

### 6.3 现有代码修改点

| 文件/模块 | 修改内容 |
|---|---|
| `ErrorCode.java` | 新增 `70xxx` 记账业务错误码 |
| `FaFaProperties.java` | 新增 `Expense` 配置对象 |
| `application-*.yml` | 增加默认货币、金额上限、查询范围和医疗同步开关 |
| `MedicalVisitApplicationService` | 创建/更新/删除后调用医疗账单同步服务 |
| `MedicalVisitDTO`（可选） | 如前端需要，增加 `linkedExpenseId`；默认可通过账单接口查询，不强制改变医疗契约 |
| `InternalAiQueryController` | 增加费用列表/统计只读接口 |
| `AiQueryApplicationService` | 查询费用事实和统计快照，不执行任何写操作 |
| `application-dev.yml` | 确认 `springdoc.groups` 包含 `expense` |
| 小程序 `app.json` | 增加记账列表/录入页面 |
| 首页 | 增加“记一笔”和“本月支出”入口，失败时不影响已有首页加载 |

### 6.4 应用服务职责

`ExpenseApplicationService` 负责：

1. 获取当前登录用户。
2. 校验宠物归属。
3. 校验金额、日期、分类和 CNY 货币规则。
4. 校验食品卡和票据媒体归属，并读取食品名称快照。
5. 创建/更新/逻辑删除手工账单。
6. 对系统生成的医疗账单执行幂等 upsert、更新和删除。
7. 将领域对象转换为 DTO，记录 `userId/petId/expenseId/sourceType` 日志。

`ExpenseStatisticsApplicationService` 负责：

1. 校验统计周期和最大查询范围。
2. 调用仓储读取已删除过滤后的支出事实。
3. 调用 `ExpenseStatisticsCalculator` 计算金额、分类和趋势。
4. 返回 `policyVersion` 或 `calculationVersion`，方便未来统计口径变化时区分结果。

统计首版不添加 Redis 缓存。月度/年度查询范围受配置约束，先用索引查询和内存确定性计算，避免在没有性能数据前引入缓存失效和跨层一致性问题。

### 6.5 医疗费用同步

建议在 `MedicalVisitApplicationService` 中保持事务内同步：

```text
create visit
  → medicalVisitRepository.save(visit)
  → expenseApplicationService.syncMedicalVisitExpense(savedVisit)

update visit
  → save updated totalCost/visitDate/hospitalName
  → syncMedicalVisitExpense(updatedVisit)

delete visit
  → logical delete medical_visit
  → expenseApplicationService.removeMedicalVisitExpense(visitId)
```

同步规则：

- `totalCost == null`：不创建账单；如果已有自动账单，逻辑删除该账单。
- `totalCost > 0`：创建或更新一条 `category=medical`、`sourceType=medical_visit` 的账单。
- 医疗账单标题优先使用医院名称，备注标识“由就医档案自动同步”；不把诊断、处方等敏感文本复制到账单。
- 更新就诊日期时同步 `expenseDate`；更新费用时同步 `amount`。
- 不允许通过普通 `PUT /api/expenses/{id}` 修改自动账单；医疗信息的修改入口只有就诊档案接口。
- 同步方法必须在同一事务中执行，医疗档案保存失败时不产生支出，支出同步失败时医疗档案事务回滚。
- 当费用从空值恢复为正数时，先通过 `findByMedicalVisitIdIncludingDeleted` 找回原账单，再将 `is_deleted` 恢复为 `0` 并更新金额，保证来源唯一键和历史 ID 稳定。

### 6.6 食品卡价格联动

- 食品列表和食品详情已经有 `price`、`currency`，记账页选择食品卡后只用于填充金额和标题初始值。
- 用户可以覆盖金额，因为食品卡价格可能是包装标价，实际购买价可能有折扣或不同规格。
- 提交账单时保存 `foodCardId` 和 `foodNameSnapshot`，不把未来食品卡价格变化反映到历史账单。
- 食品卡已归档但属于当前用户/宠物时，允许补记历史购买；已删除食品卡不能再作为新关联，但历史快照仍显示。
- 不在食品卡创建/更新时自动创建账单，避免把“目录价格”误当成“真实购买”。

---

## 7. 配置与错误码

### 7.1 `FaFaProperties.Expense`

建议新增：

```yaml
fafa:
  expense:
    default-currency: CNY
    max-amount: 1000000.00
    max-range-days: 3660
    auto-sync-medical: true
    calculation-version: v1
```

生产环境金额上限、统计范围和同步开关通过环境变量覆盖；默认值仅用于开发/测试。`calculation-version` 进入统计响应，不能在业务代码中散落字符串。

### 7.2 错误码建议

记账模块使用预留的 `70xxx` 段：

| 错误码 | 含义 | 触发条件 |
|---|---|---|
| `EXPENSE_NOT_FOUND` | 支出记录不存在 | 已删除、不存在或不属于当前用户 |
| `EXPENSE_FIELD_INVALID` | 支出字段无效 | 金额、日期、分类、备注、标题非法 |
| `EXPENSE_CATEGORY_INVALID` | 支出分类无效 | 不在六类稳定 code 中 |
| `EXPENSE_CURRENCY_UNSUPPORTED` | 货币不支持 | 首版传入非 `CNY` |
| `EXPENSE_SOURCE_INVALID` | 来源关联无效 | 食品卡/媒体不属于当前用户或宠物 |
| `EXPENSE_AUTO_RECORD_IMMUTABLE` | 自动账单不可直接修改 | 试图修改医疗同步账单 |
| `EXPENSE_RANGE_INVALID` | 统计范围无效 | 日期倒序、超过配置范围或 period 不支持 |
| `EXPENSE_MEDICAL_SYNC_FAILED` | 医疗费用同步失败 | 同步过程数据不一致或持久化失败 |

错误响应继续使用现有 `BusinessException` 和统一异常处理，不把 SQL 唯一键、堆栈或内部表名直接返回给客户端。

---

## 8. API 契约

### 8.1 通用约定

- Java 地址：`http://localhost:8080`。
- 所有公开接口需要 `Authorization: {登录返回的 token}`。
- 成功响应：`{"code":0,"message":"成功","data":...}`。
- 金额使用 JSON number 或字符串均需由 Jackson 统一反序列化为 `BigDecimal`，接口文档示例使用 number；前端展示必须格式化为两位小数。
- 日期使用 `yyyy-MM-dd`，日期时间只在审计字段使用 ISO-8601。
- `userId` 一律从登录态获取，不允许客户端传入并作为权限依据。

### 8.2 创建手工账单

- 接口定义：`POST application/json`
- 接口地址：`http://localhost:8080/api/expenses`
- 接口说明：创建一笔手工支出，可选关联食品卡和票据媒体。
- 请求 JSON：

```json
{
  "petId": 1,
  "expenseDate": "2026-08-23",
  "category": "food",
  "amount": 159.90,
  "currency": "CNY",
  "title": "室内成猫鸡肉粮",
  "notes": "2kg，活动价",
  "receiptMediaId": 201,
  "foodCardId": 301
}
```

- 响应 JSON：

```json
{
  "code": 0,
  "message": "成功",
  "data": {
    "id": 801,
    "petId": 1,
    "category": "food",
    "categoryLabel": "食品",
    "amount": 159.90,
    "currency": "CNY",
    "expenseDate": "2026-08-23",
    "title": "室内成猫鸡肉粮",
    "notes": "2kg，活动价",
    "receiptMediaId": 201,
    "foodCardId": 301,
    "foodNameSnapshot": "室内成猫鸡肉配方",
    "medicalVisitId": null,
    "sourceType": "manual",
    "systemGenerated": false,
    "editable": true
  }
}
```

### 8.3 查询、修改和删除账单

```text
GET    /api/expenses?petId=1&startDate=2026-08-01&endDate=2026-08-23&category=food&page=1&size=20
GET    /api/expenses/801
PUT    /api/expenses/801
DELETE /api/expenses/801
```

查询响应使用已有 `PageDTO`：

```json
{
  "code": 0,
  "message": "成功",
  "data": {
    "records": [
      {
        "id": 801,
        "petId": 1,
        "category": "food",
        "categoryLabel": "食品",
        "amount": 159.90,
        "currency": "CNY",
        "expenseDate": "2026-08-23",
        "title": "室内成猫鸡肉粮",
        "sourceType": "manual",
        "systemGenerated": false,
        "editable": true
      }
    ],
    "total": 1,
    "page": 1,
    "size": 20
  }
}
```

`PUT` 请求 JSON 与创建请求相同，`petId`、`id` 和 `sourceType` 不可修改。系统生成的医疗账单执行 `PUT` 时返回 `EXPENSE_AUTO_RECORD_IMMUTABLE`。删除成功返回：

```json
{"code":0,"message":"成功","data":null}
```

### 8.4 支出分类字典

- 接口地址：`GET http://localhost:8080/api/expenses/categories`
- 接口说明：返回前端 chips 和统计图例使用的稳定分类，不从数据库动态拼接。

响应 JSON：

```json
{
  "code": 0,
  "message": "成功",
  "data": [
    {"code":"food","label":"食品","sortOrder":1},
    {"code":"medical","label":"医疗","sortOrder":2},
    {"code":"supplies","label":"用品","sortOrder":3},
    {"code":"grooming","label":"美容","sortOrder":4},
    {"code":"insurance","label":"保险","sortOrder":5},
    {"code":"other","label":"其他","sortOrder":6}
  ]
}
```

### 8.5 月度/年度统计

- 接口定义：`GET`
- 接口地址：`http://localhost:8080/api/expenses/statistics`
- 月度请求：`/api/expenses/statistics?petId=1&period=month&anchorDate=2026-08-23`
- 年度请求：`/api/expenses/statistics?petId=1&period=year&anchorDate=2026-08-23`
- 说明：`anchorDate` 决定自然月或自然年，统计只读取当前用户、当前宠物和有效 CNY 账单。

月度响应 JSON：

```json
{
  "code": 0,
  "message": "成功",
  "data": {
    "petId": 1,
    "period": "month",
    "startDate": "2026-08-01",
    "endDate": "2026-08-31",
    "currency": "CNY",
    "totalAmount": 1369.90,
    "expenseCount": 6,
    "dataStatus": "available",
    "calculationVersion": "v1",
    "categoryBreakdown": [
      {"category":"food","categoryLabel":"食品","amount":489.90,"count":3,"percentage":35.76},
      {"category":"medical","categoryLabel":"医疗","amount":680.00,"count":1,"percentage":49.64},
      {"category":"supplies","categoryLabel":"用品","amount":200.00,"count":2,"percentage":14.60}
    ],
    "trend": [
      {"period":"2026-08-01","amount":0.00,"count":0},
      {"period":"2026-08-20","amount":680.00,"count":1},
      {"period":"2026-08-23","amount":159.90,"count":1}
    ],
    "limitations": ["仅统计已记录的支出，不代表未记账日期没有消费"]
  }
}
```

年度趋势的 `period` 使用 `yyyy-MM`，返回 12 个月；月度趋势使用 `yyyy-MM-dd`，返回当月完整日期序列。响应必须包含零值日期/月，方便前端直接绘制折线。

### 8.6 医疗费用自动联动

以下接口不新增请求字段，复用 Phase 6：

```text
POST /api/medical/visits
PUT  /api/medical/visits/{id}
DELETE /api/medical/visits/{id}
```

当请求包含：

```json
{
  "petId": 1,
  "visitDate": "2026-08-20",
  "hospitalName": "安心动物医院",
  "totalCost": 680.00
}
```

Java 自动维护一条：

```json
{
  "category": "medical",
  "amount": 680.00,
  "expenseDate": "2026-08-20",
  "medicalVisitId": 501,
  "sourceType": "medical_visit",
  "systemGenerated": true,
  "editable": false
}
```

重复保存同一就诊档案只更新同一条账单，不新增第二条；费用清空时同步逻辑删除该账单。

### 8.7 食品卡联动

记账页可以先请求：

```text
GET /api/foods?petId=1&status=active
```

选择食品卡后，将 `foodCardId`、`foodNameSnapshot` 的显示值和 `price` 作为表单初始值，但最终仍由用户确认 `amount` 后调用 `POST /api/expenses`。食品卡价格更新不会修改历史账单。

### 8.8 Python Agent 只读接口

新增内部接口：

```text
GET /internal/ai/pets/1/expenses?userId=3&startDate=2026-01-01&endDate=2026-12-31
GET /internal/ai/pets/1/expense-statistics?userId=3&period=year&anchorDate=2026-08-23
```

请求头：`X-Internal-Token`。

响应不包装 `Result`，仅返回 Java 已校验的事实：

```json
{
  "petId": 1,
  "range": {"startDate":"2026-01-01","endDate":"2026-12-31"},
  "records": [
    {"expenseDate":"2026-08-20","category":"medical","amount":680.00,"currency":"CNY","sourceType":"medical_visit"}
  ],
  "statistics": {
    "totalAmount": 1369.90,
    "categoryBreakdown": [{"category":"medical","amount":680.00,"percentage":49.64}],
    "calculationVersion":"v1"
  },
  "limitations": ["只反映已记录账单，不推断未记录消费"]
}
```

Agent 只回答“已记录的费用事实”和统计，不提供省钱、购买或财务建议，不提供写入工具。

---

## 9. 小程序实施方案

### 9.1 页面结构

```text
fafa-miniapp/pages/expense/
├── index.js       # 本月/本年统计、分类占比、趋势、账单列表
├── index.json
├── index.wxml
├── index.wxss
├── edit.js        # 快速记账和编辑手工账单
├── edit.json
├── edit.wxml
└── edit.wxss
```

### 9.2 页面行为

#### 记账页

- 金额输入框使用 `type="digit"`，打开页面自动聚焦或保持单手操作。
- 分类使用六个 chips，默认选中上次选择但不能覆盖服务端枚举。
- 日期默认今天，允许选择历史日期，不显示未来日期。
- 备注、票据图片和食品卡为可选项，不能阻塞快速保存。
- 点击保存后禁用按钮，成功后返回列表并刷新统计，避免重复提交。
- 票据上传复用 `/api/photos/upload`，只保存 `receiptMediaId`，不在前端保存服务器签名 URL。

#### 统计页

- 顶部切换“本月 / 本年”，调用统计接口，不在前端累计账单金额。
- 展示总支出、账单笔数和分类占比。
- 使用原生 Canvas 绘制饼图和趋势折线，首版不新增图表依赖，避免增加包体积和构建复杂度。
- 账单列表按日期倒序，系统医疗账单显示“来自就诊档案”且不显示编辑入口。
- 空数据展示“本周期暂无记录”，不显示 0 元造成误解。

### 9.3 首页入口

- Phase 8 首页新增“记一笔”快捷按钮和“本月支出”卡片。
- 支出接口失败不影响用户信息、宠物列表、AI 对话、医疗和食品入口加载。
- 首页只读取当前选中或第一只宠物，详情页允许切换宠物；不把 `petId` 写死在页面代码中。

### 9.4 前端校验

- 金额为空、非数字、`<=0` 时禁止提交。
- 小数最多两位；最终仍以服务端校验为准。
- 备注最多 500 字符，标题最多 100 字符。
- 分类 code 使用后端字典响应，前端只保存 code，不以 label 作为接口参数。
- 删除前二次确认；系统医疗账单不显示删除按钮。

---

## 10. Agent 与 Phase 9 衔接

### 10.1 Java 内部只读工具

建议在现有 `AiQueryApplicationService` 增加：

- `getExpenseFacts(userId, petId, startDate, endDate)`。
- `getExpenseStatistics(userId, petId, period, anchorDate)`。

`InternalAiQueryController` 仅提供 GET；`userId` 来自内部调用参数，但必须同时校验：

1. `userId` 是有效用户。
2. `petId` 属于该用户。
3. 日期范围不超过内部接口上限。
4. 返回对象只包含脱敏后的金额、分类、日期和来源，不包含票据 URL、诊断/处方文本或内部错误。

### 10.2 Python 工具链

在 `fafa-python/app/agent/tools/java_client.py` 增加 `get_expenses`、`get_expense_statistics`；在 `registry.py` 注册为只读工具，在 `graph.py` 中仅当用户询问“花了多少钱/支出/费用/账单”时选择。

工具错误时返回事实不可用提示，不让模型凭常识补数字。所有回答带限定语：

```text
以上金额仅统计 FaFa 中已记录的支出，不代表未记账消费。
```

Phase 8 不实现 AI 年报生成，只保证 Phase 9 能读取稳定的费用事实和 `calculationVersion`。

---

## 11. 测试计划

### 11.1 Java 领域单元测试

新增：

```text
fafa-java/src/test/java/com/fafa/domain/model/expense/ExpenseRecordTest.java
fafa-java/src/test/java/com/fafa/domain/model/expense/MoneyTest.java
fafa-java/src/test/java/com/fafa/domain/model/expense/ExpenseStatisticsCalculatorTest.java
```

覆盖：

- 六类分类 `fromCode`、大小写兼容、非法分类拒绝。
- 金额为 null、0、负数、三位小数、超过策略上限。
- 日期为未来、标题/备注超长、货币非 CNY。
- 手工账单创建、更新、逻辑删除。
- 自动医疗账单不可通过手工更新行为修改。
- 食品卡名称快照和医疗来源字段不变式。
- 月度按天、年度按月趋势补零。
- 分类金额、笔数、百分比和无数据周期。
- 金额尾差修正后分类占比合计为 `100.00`。

### 11.2 Java 应用服务测试

使用 Mockito 验证：

- 未登录或宠物不属于当前用户时拒绝。
- 票据媒体不属于当前用户时拒绝。
- 食品卡不属于当前宠物时拒绝；归档食品卡可以作为历史关联。
- 普通账单 CRUD 不会修改别人的记录。
- 自动医疗账单创建具有幂等性。
- 就诊费用更新只更新对应来源账单，不影响其他手工医疗账单。
- 就诊费用清空和就诊删除会逻辑删除自动账单。
- 统计只读取有效账单，查询范围超过配置时拒绝。

### 11.3 Repository 与迁移测试

- V8 Flyway 在已有 V7 数据库上执行成功。
- PO 与表字段、日期、`BigDecimal` 精度一致。
- `medical_visit_id` 唯一约束阻止重复自动账单。
- 列表 SQL 使用 `user_id + pet_id + expense_date + is_deleted` 条件，不能只按 `pet_id` 查询。
- 统计范围命中日期索引；不使用字符串拼接构造用户输入的排序或过滤条件。

### 11.4 接口契约测试

使用 MockMvc/Knife4j 手工验证：

1. 创建、查询、更新、删除手工账单。
2. 非法金额、非法分类、未来日期、非 CNY、越权食品卡、越权媒体。
3. 月度和年度统计响应中的零值趋势点。
4. 系统医疗账单编辑返回 `EXPENSE_AUTO_RECORD_IMMUTABLE`。
5. 医疗档案创建/更新/删除与账单同步。
6. 内部费用查询缺少 `X-Internal-Token` 时返回 401，越权 `userId/petId` 时返回空或拒绝，不能泄露数据。

### 11.5 小程序检查

- 所有新增 JS 执行 `node --check`。
- 真机验证金额键盘、分类 chips、日期选择、保存防重复、删除确认和空统计。
- 票据上传失败时仍可保存无票据账单。
- 后端错误后页面恢复按钮状态，不出现无限 loading。
- 医疗自动账单展示只读标识。

---

## 12. 分阶段实施顺序

每个子阶段结束必须保持 Java 可编译、单测可运行、接口契约可检查。

### Phase 8.0：基线冻结与契约确认

- 复核 Phase 6 `MedicalVisit.totalCost`、`visitDate`、逻辑删除行为。
- 复核 Phase 7 `FoodCard.price/currency`、名称快照和媒体归属 API。
- 固定六个分类 code、CNY 规则、医疗自动同步策略。
- 固定 API JSON 字段和错误码，先更新接口文档草案。
- 记录已有工作区改动，不覆盖 Phase 7 未提交文件。

### Phase 8.1：领域模型与 V8 迁移

- 新增 `Money`、`ExpenseCategory`、`ExpenseSourceType`、`ExpenseRecord`。
- 新增 `ExpenseStatisticsCalculator` 和领域测试。
- 编写 V8 迁移，核对金额精度、唯一索引、逻辑删除和查询索引。
- 新增 PO、Mapper、Repository 接口和实现。
- 执行 `mvn -q -DskipTests compile` 与领域单测。

### Phase 8.2：手工记账 CRUD

- 新增请求 DTO、命令、返回 DTO 和 `ExpenseController`。
- 实现创建、分页查询、详情、修改、逻辑删除。
- 接入宠物、食品卡、票据媒体归属校验。
- 完成错误码、配置、日志和 springdoc 分组。
- 用 MockMvc/Knife4j 完成手工账单接口回归。

### Phase 8.3：医疗与食品联动

- 为 `MedicalVisitApplicationService` 增加医疗账单同步调用。
- 处理创建、更新、费用清空和就诊删除四类场景。
- 手工验证同一就诊不会产生重复账单。
- 在记账接口和食品页面接入食品卡名称/价格参考。
- 验证删除食品卡、删除媒体不破坏历史账单。

### Phase 8.4：统计与只读事实

- 新增月度/年度统计接口和确定性计算服务。
- 覆盖空周期、单日、跨月、分类尾差、无数据和大范围查询。
- 增加内部费用记录/统计接口。
- 扩展 Agent 工具注册和安全提示；不新增写工具。

### Phase 8.5：小程序体验

- 增加记账列表/统计页和快速记账页。
- 增加首页快捷入口和本月支出摘要。
- 使用原生 Canvas 绘制分类图和趋势图，不引入第三方图表包。
- 实现票据可选上传、食品卡可选关联、手工账单编辑和删除确认。

### Phase 8.6：联调、文档与发布

- 执行 Java 全量测试、Python 编译/测试、前端脚本检查。
- 在真实 MySQL 执行 V8，确认 Flyway、索引和历史医疗数据。
- 启动 Java、Redis、MinIO、Python，完成跨模块联调。
- 更新 `docs/接口文档.md`、`docs/MySQL数据表记录.md`、`docs/Codex开发交接文档.md` 和阶段计划。
- 记录统计口径和已知限制，准备 Phase 9 报告模块读取契约。

---

## 13. 配置示例

### 13.1 `application-dev.yml`

```yaml
fafa:
  expense:
    default-currency: CNY
    max-amount: 1000000.00
    max-range-days: 3660
    auto-sync-medical: true
    calculation-version: v1
```

### 13.2 测试环境

```yaml
fafa:
  expense:
    default-currency: CNY
    max-amount: 100000.00
    max-range-days: 3660
    auto-sync-medical: true
    calculation-version: v1
```

测试环境金额上限可以较低，便于覆盖超限场景；不要在 Java 业务代码中读取环境变量并散落判断。

### 13.3 生产环境环境变量

```text
FAFA_EXPENSE_DEFAULT_CURRENCY=CNY
FAFA_EXPENSE_MAX_AMOUNT=1000000.00
FAFA_EXPENSE_MAX_RANGE_DAYS=3660
FAFA_EXPENSE_AUTO_SYNC_MEDICAL=true
FAFA_EXPENSE_CALCULATION_VERSION=v1
```

生产日志不得输出票据签名 URL、完整诊断文本、用户 token 或完整账单集合；关键日志只保留 `traceId/userId/petId/expenseId/sourceType`。

---

## 14. 风险与处理方案

| 风险 | 影响 | 处理方案 |
|---|---|---|
| 就诊保存重复同步 | 同一医疗费用出现多笔 | `medical_visit_id` 唯一索引 + 应用层 upsert |
| 用户修改自动医疗账单 | 医疗档案与统计不一致 | 自动账单只读，必须修改医疗档案 |
| 食品卡价格被当作实际消费 | 统计金额虚高 | 创建食品卡不自动记账，价格只作表单初始值 |
| 食品卡删除后历史无法解释 | 账单出现空名称 | 保存 `food_name_snapshot`，历史记录不级联删除 |
| 票据上传失败阻塞记账 | 快速记账体验变差 | 票据完全可选，上传失败仍允许保存 |
| 金额使用浮点 | 统计尾差和账务误差 | Java `BigDecimal` + MySQL `DECIMAL(12,2)` |
| 多币种混算 | 统计结果失真 | 首版只允许 CNY，预留字段不启用换算 |
| 日期按 UTC 转换 | 月末账单跨天 | 支出使用 `LocalDate`，不经 UTC 转换 |
| 统计范围无限扩大 | 查询慢、内存增长 | 配置最大范围，索引查询，首版不做缓存 |
| 账单越权读取 | 隐私泄露 | 每次查询都带 `user_id + pet_id`，内部接口额外校验 userId |
| Agent 编造费用 | 错误年报 | Agent 只读 Java 事实，缺失时明确“暂无已记录数据” |

---

## 15. 验收清单

### 15.1 功能验收

- [ ] 用户可以为当前宠物快速创建食品、医疗、用品、美容、保险、其他六类账单。
- [ ] 金额、分类、日期、备注、票据和食品卡关联字段校验正确。
- [ ] 用户可以按宠物、日期和分类分页查询账单。
- [ ] 手工账单可以修改和逻辑删除，删除后不进入统计。
- [ ] 自动医疗账单在就诊创建/更新时幂等生成或更新。
- [ ] 清空就诊费用或删除就诊档案时，自动医疗账单不再统计。
- [ ] 自动医疗账单不能通过普通账单接口修改或删除。
- [ ] 选择食品卡可以带出名称和价格参考，但必须用户确认金额后保存。
- [ ] 月度统计返回总额、分类占比、笔数和每日趋势。
- [ ] 年度统计返回总额、分类占比、笔数和月度趋势。
- [ ] 无数据时前端显示空状态，统计不会返回虚构消费。
- [ ] 票据图片只保存媒体关联，不因删除账单删除素材文件。
- [ ] Agent 可以查询已记录费用，但不能写账单或输出财务建议。

### 15.2 技术验收

- [ ] V8 Flyway 可在已有 Phase 7 数据库执行。
- [ ] 领域层没有 Spring、MyBatis、Redis、HTTP 或 Python 依赖。
- [ ] 金额全链路使用 `BigDecimal`/`DECIMAL`，没有浮点累计。
- [ ] 所有公开资源检查登录用户和宠物归属。
- [ ] 医疗来源使用唯一索引和应用层幂等同步。
- [ ] 统计计算有固定 `calculationVersion`。
- [ ] 统计范围受配置限制，查询命中日期索引。
- [ ] Java 全量测试通过，新增小程序 JS 语法检查通过。
- [ ] Python Agent 工具测试通过或明确记录依赖阻断项。
- [ ] 关键日志不泄漏票据 URL、诊断文本和 token。

### 15.3 产品边界验收

- [ ] 页面明确“仅统计已记录支出，不代表未记账消费”。
- [ ] 页面不把食品卡价格直接显示为已消费金额，除非用户确认保存账单。
- [ ] 医疗自动账单展示来源和只读状态。
- [ ] 统计图表不使用四舍五入后的前端金额重新计算总额。
- [ ] 用户没有宠物或宠物无账单时，入口和页面有明确空状态。

---

## 16. 文档同步清单

Phase 8 实施完成后必须同步：

1. `docs/接口文档.md`：增加记账 CRUD、分类字典、月度/年度统计、医疗联动和内部只读接口。
2. `docs/MySQL数据表记录.md`：增加 V8 `expense_record` 表、唯一索引和统计查询索引。
3. `docs/Codex开发交接文档.md`：记录 V8 迁移、配置、接口、联调顺序和已知限制。
4. `docs/分阶段开发计划.md`：将 Phase 8 状态更新为“核心代码完成，待联调”或“已完成”。
5. `docs/dev/phase8-implementation-plan.md`：实施后更新状态、勾选验收项和记录真实环境验证结果。
6. Phase 9 实施文档：引用费用只读接口和 `calculationVersion`，不复制账单表逻辑。

---

## 17. 开发规范自检

### Architecture Delineation

记账以 `ExpenseRecord` 为聚合根，金额使用不可变 `Money` 值对象，统计使用无框架依赖的确定性计算器；应用层负责编排归属、事务和医疗同步，基础设施层只负责持久化。首版不引入预算、汇率、缓存和 OCR，使用单表、索引和配置上限覆盖当前产品需求，避免提前扩展。

### Hard-coding

- 分类 code、来源类型和统计 period 使用领域枚举。
- 金额上限、最大查询范围、默认货币和统计版本进入 `FaFaProperties.Expense`。
- 前端分类展示来自 `/api/expenses/categories`，不在多个页面复制字符串。
- 医疗自动账单标题、免责声明和统计限制使用统一资源或常量，禁止散落在 Controller 中。

### Coupling / Cohesion

- `ExpenseApplicationService` 不依赖 `MedicalVisitApplicationService`，由医疗应用服务单向调用费用同步能力。
- 领域层不感知 MyBatis、媒体、微信和 Python。
- 统计读取与账单写入分离，统计计算器可独立单测。
- 内部 Agent 接口保持只读，不能复用公开写入接口绕过权限。

### Type Safety

- Java 金额使用 `BigDecimal`，日期使用 `LocalDate`，分类/来源使用枚举。
- 请求 DTO 使用 `@NotNull`、`@DecimalMin`、`@Digits`、`@Size`，应用层再次校验跨实体归属。
- 统计返回固定的 `totalAmount/categoryBreakdown/trend` 结构，空数据用零值和 `dataStatus` 表达。
- Python Agent 客户端使用明确的 typed response 映射，不从自然语言文本解析金额作为事实。

---

## 18. 实施完成后的自检命令

```text
1. 先查看 git status 和 git diff，确认没有覆盖 Phase 7 的未提交改动。
2. 检查 V8 SQL 在干净库和已有 Phase 7 数据库均可执行。
3. 检查 domain → application → infrastructure 依赖方向。
4. 检查所有账单、食品卡、票据媒体和医疗档案的 userId/petId 归属。
5. 检查金额是否全程 BigDecimal/DECIMAL，统计是否固定 rounding mode。
6. 检查医疗自动账单唯一性、更新和删除同步。
7. 检查食品卡价格没有自动写成消费事实。
8. 检查统计趋势包含零值日期/月，空数据不虚构数字。
9. 检查内部 Agent 接口只读且不泄露票据 URL、诊断文本和 token。
10. 运行 Java compile/test、Python compile/test、小程序 JS 检查。
11. 更新接口、数据库、交接文档和阶段计划。
```

Phase 8 的交付标准不是“多了一张支出表”，而是让用户能够快速记录、准确回看、理解分类趋势，并让医疗费用自动进入统计且不产生重复或不可追溯的账单。任何未确认的食品价格、未记录的消费和 AI 推断都不能进入 `expense_record`。

## 19. 本次实施记录

已落地的核心文件和能力：

- Java：V8 Flyway 迁移、`ExpenseRecord` 聚合、金额值对象、分类/来源枚举、统计计算器、MyBatis 仓储、公开 CRUD/分类/统计接口。
- 医疗联动：`MedicalVisitApplicationService` 在创建、更新、删除事务内调用费用同步，自动账单按 `medical_visit_id` 幂等并支持逻辑删除恢复。
- Agent：Java 内部开支事实/统计接口，Python 查询客户端、固定工具注册和费用意图路由。
- 小程序：开支统计页、分类占比 Canvas、趋势与明细、手工快速录入页，并接入首页入口。
- 配置：开发、测试、生产 profile 均增加 `fafa.expense` 配置；接口、数据库和交接文档已同步。

本地自动验证结果：

| 检查项 | 结果 |
|---|---|
| `mvn -q -DskipTests compile` | 通过 |
| `mvn -q test` | 通过 |
| `python -m compileall -q app tests` | 通过 |
| 小程序 Phase 8 JS `node --check` | 通过 |

仍需在真实开发环境完成：Flyway V8 实库迁移、医疗费用同步端到端、MinIO 小票关联、Python 依赖完整环境下的 Agent 测试、微信开发者工具页面验收。Python 测试环境若缺少 `dashscope`、`fastapi`、`qdrant_client` 等 requirements 依赖，应先执行项目依赖安装，再运行 pytest。
