# Phase 2 实施文档：日常记录系统

> 依据：《FaFa产品功能设计-最终版.md》第四章、《分阶段开发计划.md》第六章。
> 状态：**规划完成，数据库已建表，待开发**。
> 关联：数据库脚本 [V2__create_daily_record_tables.sql](../fafa-java/src/main/resources/db/migration/V2__create_daily_record_tables.sql)，表记录见 [MySQL数据表记录.md](../MySQL数据表记录.md)。

---

## 1. 阶段目标与范围

Phase 2 是产品最重要的数据来源，核心公理：**任何每日记录 ≤2 次点击完成**。本阶段建立宠物健康数据的时间序列，为 Phase 3 提醒、Phase 4 健康预警、Phase 5 AI 检索、Phase 8 报告提供数据基础。

### 1.1 本阶段做什么

| 类别 | 说明 | 落地 |
|------|------|------|
| 状态观察 | 六维度每日打卡（一天一格，upsert） | `daily_status` 表 + 每日打卡接口 |
| 照护动作 | 喂食事件流（一天多次，精确到分钟） | `feeding_record` 表 + 喂食接口 |
| 周期测量 | 体重独立入口 + 趋势 | `weight_record` 表 + 体重接口 |
| 关键交互 | 「今天一切正常」/「同上次」/ 漏记不惩罚 / 物种差异 | 应用服务 + 领域层 |
| 物种差异 | 猫狗维度配置（猫排尿必填、狗排尿降权） | 领域配置（不建表，见 2.2） |

### 1.2 本阶段不做什么（明确边界）

- **不做**记录提醒（Phase 3 实现提醒基础设施，Phase 2 仅预留 `daily_record` 提醒类型语义）
- **不做**用药记录（Phase 6 用药管理生成，`medication_record` 表推迟到 Phase 6）
- **不做**毛发健康/体型特征（并入 Phase 7 自定义事件）
- **不做**AI 自然语言检索（Phase 5）
- **不涉及** Python 侧改动（本阶段纯 Java + MySQL；数据形态为后续 AI 分析预留）
- **不做**宠物删除时级联清理记录（宠物为逻辑删除，记录保留不清理；成本极低且历史数据有分析价值）

---

## 2. 与《分阶段开发计划》的偏差决策

以下 4 处有意偏离计划文档（6.4/6.5/6.6 节），均已写入建表脚本注释，开发时按本文档执行：

### 2.1 daily_status 物理删除，唯一键不含 deleted

计划文档：`UNIQUE KEY uk_pet_date (pet_id, record_date, deleted)` + 逻辑删除。

**调整为**：无逻辑删除列，`UNIQUE KEY uk_pet_date (pet_id, record_date)`，删除为物理 DELETE。

理由：
1. 一天一格 upsert 语义下，删除即当天回到「未记录」空状态，重记是新的一格——物理删除语义完全贴合，无审计需求
2. 唯一键含 `deleted` 的方案存在循环缺陷：记录→删除→重记→再删除时，第二次置 `deleted=1` 会与首条已删记录撞唯一键
3. 逻辑删除方案需实现「绕过逻辑删除过滤器查已删行并复活」的仓储逻辑，复杂度不值
4. 项目已有先例：[SoftDeleteDO.java](../../fafa-java/src/main/java/com/fafa/infrastructure/persistence/SoftDeleteDO.java) 注释明确「行为流水类表按需继承」——并非所有表都要逻辑删除

### 2.2 物种记录配置不建 pet_record_config 表

计划文档标注「可选，也可配置文件实现」。**决策：领域层配置，不建表**。

理由：
1. 配置与代码强耦合——新增维度必然伴随新增枚举、校验、前端 UI 改造，DB 表不会带来免发版灵活性
2. 无运营后台编辑需求，DB 表只引入缓存与一致性复杂度
3. 前端渲染通过接口 `GET /api/daily-records/dimensions?petType=cat` 获取，契约不变

### 2.3 枚举列用 VARCHAR(16) 而非 MySQL ENUM

与既有 `pet.type`/`pet.gender` 约定一致：取值合法性由 Java 领域枚举 `fromCode()` 校验，扩展枚举值（如未来新增排便档位）无需 ALTER TABLE。

### 2.4 接口 URL 不带 /api/v1 前缀

计划文档用 `/api/v1/daily-record` 风格；实际 Phase 0/1 已确立 `/api/pets`、`/user/me` 风格。**跟随现状**：`/api/daily-records`、`/api/feeding-records`、`/api/weight-records`（复数资源名，RESTful）。

---

## 3. 数据库设计（已完成）

三张表已通过 MySQL MCP 在 `fafa` 库创建，Flyway 脚本 [V2__create_daily_record_tables.sql](../fafa-java/src/main/resources/db/migration/V2__create_daily_record_tables.sql) 同步入库（幂等 `CREATE TABLE IF NOT EXISTS`，下次启动 Flyway 记录版本不会因表已存在而失败）。

| 表 | 语义 | 唯一约束 | 删除方式 |
|----|------|---------|---------|
| `daily_status` | 状态观察，一天一格 | `uk_pet_date (pet_id, record_date)` | 物理 |
| `feeding_record` | 照护动作，事件流 | 无（一天多条） | 逻辑 `is_deleted` |
| `weight_record` | 周期测量 | 无（同日可多条） | 逻辑 `is_deleted` |

通用列约定（与既有表一致）：`id BIGINT AUTO_INCREMENT`、`created_at`/`updated_at` 自动维护、`operator_id` 记录人（Phase 10 家庭协同预留，现阶段=当前用户）。

关键点：
- `feeding_record` 增加 `unit VARCHAR(8) DEFAULT 'g'`（g/ml）——喂食量跨单位，区分单位是 Phase 8 喂养分析的数据正确性前提（计划文档未含，属增强）
- `weight_record.weight DECIMAL(5,2)` 与 `pet.weight` 精度一致
- `daily_status` 索引 `uk_pet_date` 前缀即 `(pet_id, record_date)`，直接覆盖「按宠物查日期范围」与「查最近一条」，无需计划文档中的 `idx_pet_created`

---

## 4. 领域层设计

### 4.1 包结构（与 pet 模块平铺风格一致，不建 enums 子包）

```
domain/
├── model/record/
│   ├── DailyStatus.java          # 每日状态（聚合根）
│   ├── FeedingRecord.java        # 喂食记录（实体）
│   ├── WeightRecord.java         # 体重记录（实体）
│   ├── Appetite.java             # 食欲枚举
│   ├── Energy.java               # 精神枚举
│   ├── WaterIntake.java          # 饮水枚举
│   ├── Defecation.java           # 排便枚举
│   ├── Vomiting.java             # 呕吐枚举
│   ├── Urination.java            # 排尿枚举
│   ├── FoodType.java             # 食物类型枚举
│   └── RecordDimension.java      # 维度元信息（code/label，供配置与前端）
└── service/
    └── SpeciesRecordPolicy.java  # 物种记录配置（领域服务）
```

### 4.2 维度枚举（7 个）

统一模式（参照 `PetType`/`Gender` 的 `fromCode` 约定）：

```java
public enum Appetite {
    EXCELLENT("excellent", "很好"),
    NORMAL("normal", "正常"),
    DECREASED("decreased", "下降"),
    REFUSED("refused", "拒食");

    private final String code;
    private final String label;

    public static Appetite fromCode(String code);  // 非法值抛 IllegalArgumentException
    public boolean isNormal();                      // 仅 NORMAL
    public boolean isAbnormal();                    // 明确异常档位（DECREASED/REFUSED）
}
```

各枚举取值与异常档位定义：

| 枚举 | 取值（code → label） | isAbnormal 为 true 的档位 |
|------|---------------------|--------------------------|
| Appetite | excellent很好 / normal正常 / decreased下降 / refused拒食 | decreased, refused |
| Energy | active活跃 / normal正常 / lethargic低迷 | lethargic |
| WaterIntake | normal正常 / more偏多 / less偏少 / not_noticed没注意 | more, less |
| Defecation | normal正常 / soft偏软 / diarrhea腹泻 / constipation便秘 / not_seen没看到 | soft, diarrhea, constipation |
| Vomiting | none无(默认) / once一次 / multiple多次 | once, multiple |
| Urination | normal正常 / abnormal异常 / not_seen没看到 | abnormal |
| FoodType | main_food主食 / snack零食 / can罐头 / other其他 | —（无异常语义） |

注意：`not_seen` / `not_noticed` 为中性逃生口，既非正常也非异常（漏记不惩罚的产品哲学在数据层的体现）。

### 4.3 DailyStatus（聚合根）

```java
public class DailyStatus {
    private Long id;
    private Long petId;
    private LocalDate recordDate;
    private Appetite appetite;
    private Energy energy;
    private WaterIntake waterIntake;
    private Defecation defecation;
    private Vomiting vomiting;
    private Urination urination;
    private BigDecimal appetiteAmount;   // 克，>= 0
    private BigDecimal waterAmount;      // 毫升，>= 0
    private String notes;                // <= 500
    private Long operatorId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 工厂方法（私有全参构造，参照 Pet 模式）
    public static DailyStatus create(Long petId, LocalDate date,
            Appetite appetite, Energy energy, WaterIntake waterIntake,
            Defecation defecation, Vomiting vomiting, Urination urination,
            BigDecimal appetiteAmount, BigDecimal waterAmount,
            String notes, Long operatorId);          // 校验：petId/date 必填，amount >= 0
    public static DailyStatus createNormal(Long petId, LocalDate date, Long operatorId);
    public static DailyStatus copyFrom(DailyStatus previous, LocalDate newDate, Long operatorId);
                                                     // 复制六维度+两 amount；不复制 notes
    public static DailyStatus restore(...);           // 仓储重建

    // 业务方法
    public void update(六维度, 两 amount, notes);      // 覆盖更新，同 create 校验
    public boolean isAllNormal();                     // 所有已填维度均为正常档
    public boolean hasAbnormalSignal();               // 任一维度 isAbnormal
    public List<RecordDimension> getAbnormalDimensions();
    public void assignId(Long id);                    // 仓储回填
}
```

`isAllNormal`/`hasAbnormalSignal` 是 Phase 4 健康预警引擎（拒食+呕吐=急症组合）在领域层的提前落位。

### 4.4 FeedingRecord / WeightRecord（实体）

```java
public class FeedingRecord {
    // 字段：id, petId, feedingTime(LocalDateTime), foodType, amount(>=0), unit(g/ml), foodId, notes, operatorId, deleted
    public static FeedingRecord create(Long petId, LocalDateTime feedingTime, FoodType foodType,
            BigDecimal amount, String unit, Long foodId, String notes, Long operatorId);
    public void update(...);          // 同参覆盖更新
    public static FeedingRecord restore(...);
}

public class WeightRecord {
    // 字段：id, petId, recordDate, weight(>0), notes, operatorId, deleted
    public static WeightRecord create(Long petId, LocalDate date, BigDecimal weight, String notes, Long operatorId);
    public static WeightRecord restore(...);
    public BigDecimal getWeightChange(WeightRecord previous);   // 本次 - 上次，趋势接口复用
}
```

### 4.5 SpeciesRecordPolicy（领域服务，替代 pet_record_config 表）

```java
/**
 * 物种记录配置：猫狗维度排序、必填、特殊提示
 */
public class SpeciesRecordPolicy {

    public record DimensionConfig(RecordDimension dimension, boolean required, String tip) {}

    /** 按物种取维度配置（含顺序）；other 复用 cat */
    public List<DimensionConfig> dimensionsFor(PetType type);

    /** 按物种取必填维度集合（upsert 校验用） */
    public Set<RecordDimension> requiredDimensions(PetType type);

    /** 校验一条记录是否满足物种必填要求，缺失则抛异常并指明维度 */
    public void validateRequired(PetType type, DailyStatus status);
}
```

配置内容（硬编码于策略内，与计划文档 seed 数据一致）：

| 物种 | 维度顺序（粗体=必填） | tip |
|------|----------------------|-----|
| cat（other 复用） | **appetite**, **energy**, **water_intake**, **defecation**, vomiting, **urination** | water_intake: 猫多饮多尿可能是肾病/糖尿病/甲亢早期信号；urination: 公猫排尿困难是按小时计的急症 |
| dog | **appetite**, **energy**, **defecation**, **water_intake**, vomiting | （排尿不展示） |

---

## 5. 仓储设计

接口在 `domain/repository/`，实现在 `infrastructure/persistence/repository/`（PO/Mapper 平铺，参照 Pet 三件套：`XxxPO` + `XxxMapper(BaseMapper)` + `XxxRepositoryImpl`，转换逻辑为私有 `toDomain`/`toPO` 方法，不建独立 converter 包）。

```java
public interface DailyStatusRepository {
    Optional<DailyStatus> findByPetIdAndDate(Long petId, LocalDate date);
    Optional<DailyStatus> findLatestBefore(Long petId, LocalDate exclusiveDate); // 同上次用
    List<DailyStatus> findByPetIdAndDateRange(Long petId, LocalDate start, LocalDate end); // 升序
    DailyStatus save(DailyStatus status);   // upsert：查无则 insert（回填 id），有则 update
    boolean deleteByPetIdAndDate(Long petId, LocalDate date);   // 物理删除，返回是否存在
}

public interface FeedingRecordRepository {
    Optional<FeedingRecord> findById(Long id);
    FeedingRecord save(FeedingRecord record);   // id==null insert 否则 update（参照 PetRepositoryImpl）
    List<FeedingRecord> findByPetIdAndDate(Long petId, LocalDate date);          // [date 00:00, date+1 00:00)
    List<FeedingRecord> findByPetIdAndDateRange(Long petId, LocalDate start, LocalDate end); // 升序
    void deleteById(Long id);   // @TableLogic 逻辑删除
}

public interface WeightRecordRepository {
    Optional<WeightRecord> findById(Long id);
    WeightRecord save(WeightRecord record);
    List<WeightRecord> findLatest(Long petId, int limit);                      // 按日期倒序
    List<WeightRecord> findByPetIdAndDateRange(Long petId, LocalDate start, LocalDate end); // 升序
    Optional<WeightRecord> findLatestByPetId(Long petId);
    void deleteById(Long id);   // 逻辑删除
}
```

PO 细节：
- 三张表均为 `@TableId(type = IdType.AUTO)`（与 user/pet 一致）
- `DailyStatusPO` **不含** `@TableLogic` 字段（物理删除）；`FeedingRecordPO`/`WeightRecordPO` 含 `isDeleted Boolean @TableLogic`（参照 PetPO）
- `createdAt`/`updatedAt` 用 `@TableField(fill = FieldFill.INSERT / INSERT_UPDATE)`，由 `MyMetaObjectHandler` 自动填充

---

## 6. 应用服务设计

`application/service/` 下三个服务，`application/dto/record/` 下 Command/DTO（Java record，参照 pet 模块）。

### 6.1 DailyRecordApplicationService

```java
public class DailyRecordApplicationService {
    private final DailyStatusRepository dailyStatusRepository;
    private final PetRepository petRepository;
    private final SpeciesRecordPolicy speciesRecordPolicy;

    /** upsert 每日状态：校验所有权+物种必填+日期不晚于今天 */
    @Transactional
    public DailyStatusDTO upsertStatus(SaveDailyStatusCommand command);

    /** 「今天一切正常」一键：createNormal 后保存 */
    @Transactional
    public DailyStatusDTO recordNormal(Long petId, LocalDate date);

    /** 最近一条记录（before 可选：返回严格早于该日期的最近一条，同上次预览用） */
    public Optional<DailyStatusDTO> getLatest(Long petId, LocalDate before);

    /** 「同上次」一键：取严格早于 date 的最近一条，copyFrom 后 upsert；无历史抛 RECORD_NO_PREVIOUS */
    @Transactional
    public DailyStatusDTO copyFromLast(Long petId, LocalDate date);

    public Optional<DailyStatusDTO> getByDate(Long petId, LocalDate date);
    public List<DailyStatusDTO> listByRange(Long petId, LocalDate start, LocalDate end); // 升序，范围<=366天
    @Transactional public void delete(Long petId, LocalDate date);

    /** 物种维度配置（前端渲染打卡页） */
    public List<DimensionConfigDTO> getDimensionConfigs(String petType);
}
```

### 6.2 FeedingRecordApplicationService

```java
public class FeedingRecordApplicationService {
    private final FeedingRecordRepository feedingRecordRepository;
    private final PetRepository petRepository;   // 所有权校验

    @Transactional public FeedingRecordDTO create(SaveFeedingRecordCommand command);
    @Transactional public FeedingRecordDTO update(Long id, SaveFeedingRecordCommand command);
    @Transactional public void delete(Long id);
    public List<FeedingRecordDTO> listByDate(Long petId, LocalDate date);
    public List<FeedingRecordDTO> listByRange(Long petId, LocalDate start, LocalDate end);
}
```

### 6.3 WeightRecordApplicationService

```java
public class WeightRecordApplicationService {
    private final WeightRecordRepository weightRecordRepository;
    private final PetRepository petRepository;

    /**
     * 新增体重记录，同一事务内同步 pet.weight 冗余字段（pet.updateWeight）
     * —— 计划文档未提及的跨聚合联动，是本阶段必须实现的细节
     */
    @Transactional
    public WeightRecordDTO create(SaveWeightRecordCommand command);

    public List<WeightRecordDTO> latest(Long petId, int limit);   // 每条含 change（较上一条差值）
    public WeightTrendDTO trend(Long petId, LocalDate start, LocalDate end); // 升序+逐条变化+区间总变化
    @Transactional public void delete(Long id);
}
```

### 6.4 所有权校验（横切规则）

三个服务都需要「petId 属于当前登录用户」校验。**做法**：在 `PetApplicationService` 提取公共方法 `checkPetOwnership(Long petId)`（返回 Pet 或抛 NOT_FOUND/PARAM_INVALID），三个记录服务注入调用——不新建抽象类，复用现有服务，避免三处复制粘贴。

---

## 7. 接口层设计

`interfaces/controller/` 三个 Controller + `interfaces/dto/record/` 请求/响应 DTO（`@Data` + jakarta validation，参照 pet 模块），Knife4j 注解齐全（`@Tag`/`@Operation`/`@Parameter`）。

**鉴权**：三组接口均在 `/api/**` 下，走 Sa-Token 默认拦截，无需改 [SaTokenConfig.java](../../fafa-java/src/main/java/com/fafa/infrastructure/config/SaTokenConfig.java)。

**日期格式**：遵循项目 Jackson 默认 ISO 格式——`LocalDate` → `yyyy-MM-dd`，`LocalDateTime` → `yyyy-MM-dd'T'HH:mm:ss`。

### 7.1 每日打卡 `DailyRecordController` — `/api/daily-records`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/daily-records/dimensions?petType=cat` | 物种维度配置（顺序/必填/提示/选项），打卡页渲染 |
| POST | `/api/daily-records` | upsert 每日状态（同日重复提交=覆盖，替代计划文档的 record+update 两个接口） |
| POST | `/api/daily-records/record-normal?petId=&date=` | 「一切正常」一键 |
| GET | `/api/daily-records/latest?petId=&before=` | 最近一条（`before` 可选，同上次预览回填） |
| POST | `/api/daily-records/copy-last?petId=&date=` | 「同上次」一键 |
| GET | `/api/daily-records?petId=&date=` | 某天详情（404 语义：`data=null`，前端显示「未记录」灰格） |
| GET | `/api/daily-records/list?petId=&startDate=&endDate=` | 范围列表（升序） |
| DELETE | `/api/daily-records?petId=&date=` | 删除某天（物理删除） |

**SaveDailyStatusRequest**（POST body）：

```json
{
  "petId": 1,
  "recordDate": "2026-08-22",
  "appetite": "normal",
  "energy": "active",
  "waterIntake": "normal",
  "defecation": "normal",
  "vomiting": "none",
  "urination": "normal",
  "appetiteAmount": 80,
  "waterAmount": null,
  "notes": "换新粮第一天"
}
```

校验：`petId`/`recordDate` `@NotNull`；枚举字段非法 code 由 `fromCode` 抛出（→ PARAM_INVALID）；物种必填维度缺失 → 30004；`recordDate` 晚于今天 → 30002；`notes` `@Size(max=500)`；amount `@DecimalMin("0")`。

**DailyStatusResponse**：请求体字段 + `id`/`createdAt`/`updatedAt` + `allNormal`(boolean) + `abnormalDimensions`(数组)——前端高亮异常维度与 Phase 4 预警展示直接可用。

**DimensionsResponse**：`[{dimension:"appetite", label:"食欲", required:true, tip:null, options:[{code:"excellent",label:"很好"},...]}, ...]`。

### 7.2 喂食记录 `FeedingRecordController` — `/api/feeding-records`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/feeding-records` | 新增一次喂食 |
| PUT | `/api/feeding-records/{id}` | 修改 |
| DELETE | `/api/feeding-records/{id}` | 删除（逻辑） |
| GET | `/api/feeding-records?petId=&date=` | 当天喂食列表（按时间升序） |
| GET | `/api/feeding-records/range?petId=&startDate=&endDate=` | 范围列表（报告/统计用） |

**SaveFeedingRequest**：`petId`(@NotNull)、`feedingTime`(@NotNull，未来>5分钟容差拒绝)、`foodType`(@NotBlank)、`amount`(可选 `@DecimalMin("0")`)、`unit`(g/ml，默认 g)、`foodId`(可选，Phase 7 前前端不传)、`notes`(@Size(max=500))。

### 7.3 体重记录 `WeightRecordController` — `/api/weight-records`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/weight-records` | 新增体重（联动更新 pet.weight） |
| GET | `/api/weight-records/latest?petId=&limit=10` | 最近 N 条（含较上一条变化值） |
| GET | `/api/weight-records/trend?petId=&startDate=&endDate=` | 趋势（区间缺省=最近90天） |
| DELETE | `/api/weight-records/{id}` | 删除（逻辑） |

**SaveWeightRequest**：`petId`(@NotNull)、`recordDate`(@NotNull，不晚于今天)、`weight`(@NotNull `@DecimalMin(value="0.01")`，单位 kg)、`notes`(@Size(max=500))。

**WeightRecordResponse**：`id/petId/recordDate/weight/notes/change`（较上一条，首条为 null）。

**WeightTrendResponse**：`records[]`（升序含 change）+ `latest` + `totalChange`（末条-首条）。

### 7.4 错误码（ErrorCode 新增 30xxx 记录段）

```java
// 记录模块 30xxx
RECORD_NOT_FOUND(30001, "记录不存在"),
RECORD_DATE_IN_FUTURE(30002, "记录日期不能晚于今天"),
RECORD_NO_PREVIOUS(30003, "没有可复用的历史记录"),
DAILY_DIMENSION_MISSING(30004, "必填记录维度缺失"),
FEEDING_TIME_IN_FUTURE(30005, "喂食时间不能晚于当前时间"),
```

---

## 8. 关键业务规则明细

1. **upsert 并发兜底**：`save` 先查后写；极端并发下 insert 撞 `uk_pet_date` 抛 `DuplicateKeyException`，仓储实现捕获后转 update。单用户对单宠物记录，概率极低，兜底即可不加锁。
2. **补记规则**：`recordDate` 允许过去任意日期（补记）；禁止未来日期。喂食时间同理（5 分钟容差覆盖「刚喂完手速提交」）。
3. **「同上次」语义**：取**严格早于目标日期**的最近一条（若取含当天的最近一条，当天已记录时会复制自己，无意义）；复制六维度+两 amount，**不复制 notes**（备注是当天特定信息）；目标日已有记录则覆盖。无历史记录返回 30003。
4. **「一切正常」语义**：按物种维度全部填正常档（`vomiting=none`）；狗不填排尿（保持 null）；已有记录同样覆盖。
5. **物种必填校验**：仅对 upsert 手动记录强制（一键操作天然齐全）；`not_seen`/`not_noticed` 满足必填（逃生口也是有效记录）。
6. **体重联动**：保存体重记录与 `pet.updateWeight(weight)` 同事务；删除体重记录**不回滚** pet.weight（冗余字段表示「最近已知体重」，回滚语义不明确且需查次新记录，成本不值）。
7. **删除宠物不级联**：记录保留（宠物逻辑删除后记录成为不可达孤儿数据，量小无成本，若 Phase 10 家庭协同需要再处理）。
8. **列表范围上限**：`startDate~endDate` ≤ 366 天，防全表扫描滥用。

---

## 9. 测试与验收

### 9.1 单元测试（`src/test/java`，JUnit5，核心 ≥80% 覆盖）

- `DailyStatusTest`：createNormal 全正常；copyFrom 复制维度/不复制 notes；hasAbnormalSignal（拒食+多次呕吐组合）；getAbnormalDimensions 清单；amount 负数拒绝
- `SpeciesRecordPolicyTest`：cat 含排尿必填；dog 无排尿；other 复用 cat；validateRequired 缺失抛异常并指明维度
- `WeightRecordTest`：weight<=0 拒绝；getWeightChange 正负差值
- 维度枚举参数化测试：fromCode 合法/非法值、isAbnormal 档位

### 9.2 集成验收（Knife4j / Apifox 手工用例）

1. 登录 → 建宠物（猫）→ `POST /api/daily-records` 记录今天 → **再次提交不同值** → 验证 `updated_at` 变化且无新行（upsert 生效）
2. `POST /api/daily-records/record-normal` → 响应 `allNormal=true`
3. 昨天记一条异常（拒食+多次呕吐）→ 今天 `POST /api/daily-records/copy-last` → 验证维度复制、notes 为空、`abnormalDimensions` 非空
4. 猫漏填排尿提交 → 30004；`recordDate` 传明天 → 30002
5. 狗的 `GET /dimensions` 响应无 urination
6. 喂食：同日 POST 两次 → 两条记录；`GET ?petId=&date=` 按时间升序；修改/删除后列表正确
7. 体重：POST 4.50 → 查 `GET /api/pets/{id}` 验证 `weight` 同步为 4.50；再 POST 4.80 → latest 接口 change=+0.30，trend 总变化 +0.30
8. 未登录调任意接口 → 1002；用 B 用户的 token 访问 A 用户的宠物记录 → 宠物不存在/无权
9. 30 天列表查询性能 <1s（数据量小天然满足，验证执行计划走 `uk_pet_date` 即可）

### 9.3 完成标准

- [x] 三张表创建完成（本日已完成，含唯一约束与索引）
- [ ] 领域模型 + 仓储 + 应用服务 + 三 Controller 完成，编译通过
- [ ] upsert / 一切正常 / 同上次 / 物种差异 / 体重联动 五大关键功能验收通过
- [ ] 单元测试通过，领域层覆盖 ≥80%
- [ ] Knife4j 文档自动生成，Apifox 用例全通过
- [x] MySQL数据表记录.md 已更新

---

## 10. 前端对接要点（fafa-miniapp，后续迭代实现）

**打卡页**（核心页）：
- 顶部宠物切换器 + 日期左右滑动（默认今天）
- 「今天一切正常」按钮置顶醒目（点击 → record-normal，1 次点击完成）
- 「同上次」按钮（点击 → GET /latest?before=今天 预览回填 → 用户确认 POST，2 次点击完成）
- 维度卡片按 `GET /dimensions` 返回顺序渲染，异常档位红色高亮，`tip` 角标提示
- 未记录日期灰格展示，**不做**连续打卡天数/红绿灯等愧疚式 UI
- 提交走 upsert，当天重复提交直接覆盖

**喂食页**：首页快速入口 → 轻量表单（时间默认当前 + 数量 + 类型 chips + 单位切换 g/ml）→ 当天列表左滑删除。

**体重页**：宠物详情页独立入口 → 大数字键盘（kg）→ 折线趋势图（最近 10 次）+「较上次 +0.2kg」提示。

---

## 11. 实施顺序

| 步骤 | 内容 | 产出 |
|------|------|------|
| 0 ✅ | 数据库（Flyway V2 + MCP 建表 + 文档） | 本次完成 |
| 1 | 领域层：10 个枚举 + RecordDimension + 三实体 + SpeciesRecordPolicy | 可独立编译验证 |
| 2 | ErrorCode 新增 30xxx + 仓储接口 | — |
| 3 | 基础设施层：三组 PO/Mapper/RepositoryImpl | 编译验证 |
| 4 | 应用层：Command/DTO + 三应用服务 + PetApplicationService.checkPetOwnership 提取 | 编译验证 |
| 5 | 接口层：三 Controller + 请求/响应 DTO | 启动 + Knife4j 验证 |
| 6 | 单元测试 | mvn test |
| 7 | Apifox 全用例回归（9.2 清单） | 验收 |

## 12. 后续阶段预留点

- **Phase 3 提醒**：提醒类型 `daily_record`（每晚定时提醒打卡）；体重每月温和提醒
- **Phase 4 预警**：`hasAbnormalSignal`/`getAbnormalDimensions` 已在领域层就位，规则引擎直接消费
- **Phase 5 AI 检索**：daily_status 宽表=单表单列查趋势，为 AI 查询工具链（"上个月拒食几天"）的完美形态
- **Phase 6 用药**：`medication_record` 表随用药管理落地
- **Phase 7 食品**：`feeding_record.food_id` 外键语义已预留，接口参数已透传
- **Phase 10 家庭协同**：`operator_id` 全表预留，现阶段恒为宠物主人

---

*文档创建：2026-08-22 · FaFa 开发组*
