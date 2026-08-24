# Phase 3 实施文档：提醒系统

> 依据：《FaFa产品功能设计-最终版.md》第八章、《分阶段开发计划.md》第七章（Phase 3）。
> 状态：**开发完成**（`mvn compile` 通过，95 项单元测试全部通过），待联调。
> 关联：数据库脚本 [V3__create_reminder_tables.sql](../../fafa-java/src/main/resources/db/migration/V3__create_reminder_tables.sql)，表记录见 [MySQL数据表记录.md](../MySQL数据表记录.md)。

---

## 1. 阶段目标与范围

Phase 3 实现完整的提醒系统：疫苗/驱虫/洗澡/剪指甲/体检/生日/每日打卡/自定义提醒，六种周期配置，完成闭环（完成 → 生成事件记录 → 自动计算下次时间），并为 Phase 6 用药提醒提供基础设施。

**核心价值**：
- 避免遗忘关键照护任务（疫苗过期、驱虫漏做）
- 完成即自动生成事件记录，成长档案时间轴从第一天起就有数据
- 首页「今日提醒」区成为每日打开产品的第一价值触点

### 1.1 本阶段做什么

| 类别 | 说明 | 落地 |
|------|------|------|
| 提醒 CRUD | 9 种类型提醒的创建/编辑/删除/暂停/恢复 | `reminder` 表 + 提醒接口 |
| 周期引擎 | 6 种周期（once/days/weekly/monthly/months/yearly）的下期推算 | 领域服务 ReminderScheduleService |
| 完成闭环 | 完成 → 记录历史 → 生成事件 → 推进下期/终态 | 应用服务 + 三表事务写入 |
| 今日提醒 | 首页「今日待办 + 今日已完成」合并视图（跨宠物） | today 接口 |
| 事件写入 | 疫苗/驱虫/洗澡/剪指甲/体检/自定义完成时生成事件记录 | `event_record` 表（写入侧先行） |
| 定时扫描 | 每小时扫描到期与明日重要提醒，组装推送内容（日志网关） | ReminderPushScheduler |
| 事件查询 | 按宠物+日期范围查事件记录（闭环验证 + 时间轴预览） | event-records 只读接口 |

### 1.2 本阶段不做什么（明确边界）

- **不做**微信订阅消息真实发送——依赖小程序类目审核、模板 ID 配置与用户订阅授权，均未就绪。Phase 3 交付扫描/去重/内容组装/日志推送的完整基础设施，订阅消息在小程序发布流程中接入（见第 8.3 节接入预留）
- **不做**用药提醒的 medication_record 生成——Phase 6 用药管理实现；`type=medication` 枚举与 `medication_id` 字段已预留，前端本阶段不提供用药提醒创建入口
- **不做**成长档案读模块（手动创建事件、时间轴聚合页、标签编辑）——Phase 4+ 扩展；本阶段 `event_record` 仅提醒完成写入 + 简单范围查询
- **不做**宠物创建时自动生成生日提醒——用户手动创建（yearly 周期已覆盖），自动生成列为后续增强
- **不做**打卡提交后自动完成当日打卡提醒——后端不耦合两个模块；前端在打卡成功后自行调用 complete 接口（见 12.2 前端要点）
- **不做**多实例部署下的调度去重——当前单实例部署；扩容时用已有 Redisson 加分布式锁（见 8.4）

---

## 2. 与《分阶段开发计划》的偏差决策

以下 7 处有意偏离计划文档（第七章），均已写入建表脚本注释，开发时按本文档执行：

### 2.1 状态机简化：取消持久态 completed

计划文档状态机为 `pending → completed → [生成新的pending]`，且 7.2.3 称「同时生成新的提醒记录」。

**调整为**：`reminder` 行 = 提醒定义 + 当前周期状态，只有三个持久态：

```
pending（生效中，等待 next_remind_time）
   │ complete()                    pause()
   ├── 周期性 → 原地推进 next_remind_time，保持 pending
   ├── 一次性 → finished（终态）
   └── paused（暂停，不扫描不展示） ── resume() ──→ pending
```

理由：
1. 「每期一行」会让同一提醒定义在列表中出现多行，按宠物列提醒、编辑、暂停的查询与语义全部复杂化
2. 计划文档 7.3.1 的实体设计本身就有 `lastCompletionTime`（单数、挂在行上），与「行=定义」自洽；完成历史由 `reminder_completion_history` 独立承载，审计需求完整满足
3. 「今日已完成」从完成历史表查询，不依赖主表 completed 态

### 2.2 新增 event_record 表（写入侧本阶段落地）

计划文档完成闭环要求「完成 → 生成事件记录」，且 `reminder_completion_history.event_record_id` 外键引用它，但未安排 event_record 的建表阶段（成长档案在阶段总览中无独立 Phase）。

**决策**：Phase 3 建立 `event_record` 表并落地提醒完成的写入侧。理由：完成闭环是本阶段验收标准；时间轴（Phase 4+）聚合照片/日记/事件，事件数据从第一天开始积累；现在建表避免后续迁移。表结构已为成长档案预留 `source`/`material_urls`/`tags` 字段。

### 2.3 推送策略：基础设施先行，订阅消息延后启用

计划文档要求微信订阅消息推送。**调整为**：交付扫描调度、同日去重（`last_push_time`）、推送内容组装（含宠物名/类型文案）、日志网关；真实订阅消息发送延后（外部依赖未就绪，见 1.2）。不引入推送网关接口抽象——单一日志实现下接口属过度设计，订阅消息接入时再抽（届时有两个实现，才构成真实策略点）。

### 2.4 枚举列用 VARCHAR(16) 而非 MySQL ENUM

计划文档建表脚本用 MySQL ENUM。与 Phase 0-2 约定一致：`type`/`interval_type`/`status`/`source` 均为 VARCHAR，取值合法性由 Java 领域枚举 `fromCode()` 校验，扩展无需 ALTER TABLE。

### 2.5 新增 last_push_time 列与完成历史快照列（计划文档未含，属增强）

- `reminder.last_push_time`：计划要求「过期提醒每天提醒一次」，但每小时扫描若无去重标记会重复推送。同一提醒每日至多推送一次
- `reminder_completion_history.reminder_title`/`reminder_type` 快照：提醒被删除（逻辑删除）后，其完成历史与「今日已完成」列表仍可独立展示（@TableLogic 逻辑删除下按 ID 反查提醒会查不到已删行）

### 2.6 接口 URL 不带 /api/v1 前缀，风格 RESTful

计划文档用 `/api/v1/reminder/create` 动词风格；跟随 Phase 0-2 现状：`/api/reminders`（复数资源名），动作用 HTTP 方法 + 子资源表达（`POST /{id}/complete`、`POST /{id}/pause`）。`userId` 一律从 Sa-Token 会话取，不做请求参数。

### 2.7 下次提醒时间基准：以被完成期的计划时间为基准 + 过期滚动

计划文档 `calculateNextRemindTime()` 以「当前时间」为基准。

**调整为**：以 `next_remind_time`（被完成这一期的计划时间）为基准推算下期；若推算结果已早于当前时间（长期过期后才补完成），以当前时间为基准重算一次。

理由：以完成时间为基准会让周期漂移——月度驱虫计划 9 月 1 日，用户 9 月 3 日完成，下次应为 10 月 1 日而非 10 月 3 日。滚动规则防止「补完成一条过期 3 周的每 3 天提醒后，下期仍是过去时间」立即再次过期。

另明确计划文档中矛盾之处：`interval_value` 为 INT，但 yearly 示例 `'08-21'` 是字符串。**决策**：yearly 不使用 interval_value（目标月日隐含在 next_remind_time 中，plusYears(1) 即得），各周期 value 语义见 4.4。

---

## 3. 数据库设计（已完成）

三张表已通过 MySQL MCP 在 `fafa` 库创建，Flyway 脚本 [V3__create_reminder_tables.sql](../../fafa-java/src/main/resources/db/migration/V3__create_reminder_tables.sql) 同步入库（幂等 `CREATE TABLE IF NOT EXISTS`，下次启动 Flyway 不会因表已存在而失败）。

| 表 | 语义 | 删除方式 | 写入模式 |
|----|------|---------|---------|
| `reminder` | 提醒定义 + 当前周期状态 | 逻辑 `is_deleted` | upsert |
| `reminder_completion_history` | 完成历史（审计 + 今日已完成查询） | 不删除 | 追加只写 |
| `event_record` | 成长档案事件（本阶段仅提醒完成写入） | 逻辑 `is_deleted` | 追加 + 逻辑删除 |

通用列约定与 Phase 0-2 一致：`id BIGINT AUTO_INCREMENT`、`created_at`/`updated_at` 自动维护、`user_id`/`operator_id` 家庭协同预留（现阶段=当前用户）。

关键点：
- `idx_scan (status, next_remind_time)` 覆盖定时扫描（`status='pending' AND next_remind_time BETWEEN ? AND ?`），`is_deleted=0` 由 MyBatis-Plus `@TableLogic` 自动追加
- `photo_urls`/`material_urls`/`tags` 用 MySQL JSON 类型存 URL/标签数组，PO 侧以 `String`（JSON 序列化）或 `List<String>` + TypeHandler 映射（见 5.2）
- 建表语句与索引说明已同步至 [MySQL数据表记录.md](../MySQL数据表记录.md)

---

## 4. 领域层设计

### 4.1 包结构（与 record 模块平铺风格一致，不建 enums 子包）

```
domain/
├── model/reminder/
│   ├── Reminder.java              # 提醒（聚合根）
│   ├── ReminderCompletion.java    # 完成记录（实体）
│   ├── ReminderType.java          # 提醒类型枚举
│   ├── IntervalType.java          # 周期类型枚举
│   └── ReminderStatus.java        # 提醒状态枚举
├── model/event/
│   ├── EventRecord.java           # 事件记录（实体）
│   ├── EventType.java             # 事件类型枚举
│   └── EventSource.java           # 事件来源枚举
├── repository/
│   ├── ReminderRepository.java
│   ├── ReminderCompletionRepository.java
│   └── EventRecordRepository.java
└── service/
    └── ReminderScheduleService.java  # 周期推算（领域服务）
```

### 4.2 枚举（5 个）

统一 `code`/`label` + `fromCode()`（大小写不敏感，非法值抛 IllegalArgumentException）模式：

| 枚举 | 取值（code → label） | 备注 |
|------|---------------------|------|
| ReminderType | vaccine疫苗 / deworming驱虫 / bath洗澡 / nail_trim剪指甲 / checkup体检 / medication用药 / birthday生日 / daily_record每日打卡 / custom自定义 | `generatesEvent()`：仅 vaccine/deworming/bath/nail_trim/checkup/custom 为 true（birthday/daily_record 纯提醒；medication Phase 6 写 medication_record）；`isImportant()`：vaccine/checkup/deworming 为 true（提前 1 天推送） |
| IntervalType | once一次性 / days每N天 / weekly每周 / monthly每月 / months每N个月 / yearly每年 | value 语义见 4.4 |
| ReminderStatus | pending生效中 / paused已暂停 / finished已结束 | 无 completed 持久态（见 2.1） |
| EventType | vaccine / deworming / bath / nail_trim / checkup / custom | Phase 4+ 成长档案可扩展（如 first_walk 换粮等） |
| EventSource | manual手动 / reminder提醒完成 | |

### 4.3 Reminder（聚合根）

```java
public class Reminder {
    private Long id;
    private final Long petId;
    private final Long userId;

    private ReminderType type;
    private String title;              // ≤200，必填
    private String description;        // ≤500

    private IntervalType intervalType;
    private Integer intervalValue;     // 语义随 intervalType，见 4.4
    private LocalDateTime nextRemindTime;

    private ReminderStatus status;

    private LocalDateTime lastCompletionTime;
    private String lastCompletionNotes;
    private LocalDateTime lastPushTime;

    private final Long medicationId;   // Phase 6 预留
    private boolean deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 工厂
    public static Reminder create(Long petId, Long userId, ReminderType type, String title,
                                  String description, IntervalType intervalType, Integer intervalValue,
                                  LocalDateTime nextRemindTime, LocalDateTime now);   // now 用于校验首提醒时间不早于当前

    public static Reminder restore(...);   // 仓储重建，全字段

    // 行为
    /** 完成：记录完成信息；一次性→finished；周期性→以 nextRemindTime 为基准推算下期（领域服务传入） */
    public void complete(LocalDateTime completionTime, String notes, ReminderScheduleService schedule);
    /** 覆盖更新编辑字段（标题/描述/类型/周期/下次提醒时间），保持状态不变 */
    public void update(ReminderType type, String title, String description,
                       IntervalType intervalType, Integer intervalValue,
                       LocalDateTime nextRemindTime, LocalDateTime now);
    public void pause();      // 仅 pending 可暂停
    public void resume();     // 仅 paused 可恢复；nextRemindTime 不重算（暂停期间过期则恢复后即为逾期态）
    public boolean isOverdue(LocalDateTime now);      // pending && nextRemindTime < now
    public void markPushed(LocalDateTime pushTime);   // 推送去重标记
}
```

业务规则（create/update 校验，非法抛 IllegalArgumentException，应用层转 ErrorCode）：
1. `petId`/`userId`/`type`/`title`/`intervalType`/`nextRemindTime` 必填
2. `title` ≤200 字符，`description` ≤500 字符
3. `nextRemindTime` 必须晚于当前时间（`now` 参数注入便于测试）
4. 周期配置合法性由 `ReminderScheduleService.validateInterval` 校验（见 4.4）
5. 状态转移约束：complete 仅 pending；pause 仅 pending；resume 仅 paused

### 4.4 ReminderScheduleService（周期推算领域服务）

**interval_value 语义**：

| intervalType | intervalValue | 示例 |
|--------------|---------------|------|
| once | 不使用（null） | 明天去宠物店洗澡 |
| days | N ≥ 1 | 每 3 天 → 3 |
| weekly | ISO 星期值 1（周一）~ 7（周日） | 每周日 → 7 |
| monthly | 每月几号 1~31 | 每月 1 号 → 1 |
| months | N ≥ 1 | 每 3 个月 → 3 |
| yearly | 不使用（null，目标月日隐含在基准时间里） | 每年生日 |

**核心方法**：

```java
/**
 * 推算下一期提醒时间
 *
 * @param base  基准时间（被完成期的计划 nextRemindTime）
 * @param now   当前时间（用于过期滚动）
 */
public LocalDateTime nextAfter(LocalDateTime base, IntervalType type, Integer value, LocalDateTime now) {
    LocalDateTime next = switch (type) {
        case ONCE -> null;                                   // 调用方不触发
        case DAYS -> base.plusDays(value);
        case WEEKLY -> nextWeekday(base, value);             // 下一个目标星期几，同日则 +7 天
        case MONTHLY -> sameDayNextMonth(base, value);       // 下月 value 号，超长月末钳制
        case MONTHS -> base.plusMonths(value);
        case YEARLY -> base.plusYears(1);                    // 2/29 生日闰年规则由 plusYears 处理
    };
    // 过期滚动：长期过期后补完成，推算结果已在过去 → 以当前时间为基准重算一次
    if (next != null && !next.isAfter(now)) {
        return nextAfter(now, type, value, now.plusNanos(1));  // 递归一次即退出（now+ε 保证结果必然未来）
    }
    return next;
}

/** 周期配置合法性校验（create/update 复用） */
public void validateInterval(IntervalType type, Integer value);
```

边界规则明细（单元测试必覆盖）：
- **weekly**：`daysAhead = (target - base.dayOfWeek + 7) % 7`，为 0 时取 7；如基准周日、目标周日 → 下周日；目标周三 → +3 天
- **monthly**：下月 `min(value, 下月最大天数)` 号，时刻（时分秒）取基准时间；如 31 号在 30 天月份 → 30 号，2 月 → 28/29 号
- **monthly 滚动示例**：每月 1 号的提醒过期至 9/20 才完成 → 首算 9/1（已过）→ 以 now 重算 → 10/1
- **validateInterval**：once/yearly 的 value 忽略（null/任意值均合法）；days/months 的 value 必填且 ≥1；weekly 必填且 1~7；monthly 必填且 1~31

### 4.5 ReminderCompletion（完成记录实体）

```java
public class ReminderCompletion {
    private Long id;
    private final Long reminderId;
    private final Long petId;
    private final Long userId;           // 完成人
    private final String reminderTitle;  // 快照
    private final String reminderType;   // 快照（code）
    private final LocalDateTime completionTime;
    private final String notes;          // ≤500
    private final List<String> photoUrls; // ≤9 张
    private Long eventRecordId;          // 生成事件后回填

    public static ReminderCompletion create(Reminder reminder, Long userId,
                                            LocalDateTime completionTime, String notes, List<String> photoUrls);
    public static ReminderCompletion restore(...);
    public void assignEventRecordId(Long eventRecordId);
}
```

校验：`completionTime` 不可晚于当前时间（应用层容差 +5 分钟，与喂食时间容差一致）；`notes` ≤500；`photoUrls` ≤9。

### 4.6 EventRecord（事件实体）

```java
public class EventRecord {
    private Long id;
    private final Long petId;
    private final Long userId;
    private final EventSource source;    // 本阶段只写 REMINDER
    private final Long sourceId;         // reminderId
    private final EventType eventType;   // 由 ReminderType 映射
    private final String title;          // = 提醒标题
    private final String content;        // = 完成备注（可空）
    private final LocalDate eventDate;   // = 完成日期
    private final List<String> materialUrls; // = 完成照片
    private final Long operatorId;

    /** 提醒完成生成事件 */
    public static EventRecord fromReminder(Reminder reminder, ReminderCompletion completion);
    public static EventRecord restore(...);
}
```

`ReminderType → EventType` 映射：vaccine/deworming/bath/nail_trim/checkup/custom 一一同名映射；birthday/daily_record/medication 不生成事件（前三者 `generatesEvent()=false`，medication 留待 Phase 6 写用药记录）。

---

## 5. 仓储设计

### 5.1 仓储接口（domain/repository）

```java
public interface ReminderRepository {
    Reminder save(Reminder reminder);
    Optional<Reminder> findById(Long id);
    /** 宠物的全部提醒（提醒管理页，含 paused/finished，按 nextRemindTime 升序） */
    List<Reminder> findByPetId(Long petId);
    /** 用户待办：status=pending 且 nextRemindTime < endExclusive（今日+逾期，首页今日提醒） */
    List<Reminder> findDueByUserId(Long userId, LocalDateTime endExclusive);
    /** 定时扫描：status=pending 且 nextRemindTime ∈ [start, end]（全用户） */
    List<Reminder> findPendingBetween(LocalDateTime start, LocalDateTime end);
    /** 宠物删除级联：逻辑删除该宠物全部提醒 */
    void deleteByPetId(Long petId);
    /** 推送去重标记回写（独立小更新，避免整行 update 竞争） */
    void updateLastPushTime(Long id, LocalDateTime pushTime);
}

public interface ReminderCompletionRepository {
    ReminderCompletion save(ReminderCompletion completion);
    /** 单提醒完成历史（时间倒序） */
    List<ReminderCompletion> findByReminderId(Long reminderId);
    /** 用户当日完成记录 [start, end)，首页「今日已完成」 */
    List<ReminderCompletion> findByUserIdAndTimeBetween(Long userId, LocalDateTime start, LocalDateTime end);
}

public interface EventRecordRepository {
    EventRecord save(EventRecord record);
    /** 宠物事件时间轴（日期升序，验证/预览接口用） */
    List<EventRecord> findByPetIdAndDateRange(Long petId, LocalDate start, LocalDate end);
}
```

### 5.2 PO/Mapper 约定（infrastructure/persistence）

与 `FeedingRecordPO` 风格一致：`@TableName` + `@TableId(type = IdType.AUTO)` + `@TableLogic` + `createdAt`/`updatedAt` `FieldFill`。

JSON 列映射：`photoUrls`/`materialUrls`/`tags` 在 PO 中声明为 `String`（存 JSON 文本），仓储转换层用 Jackson `ObjectMapper` 做 `List<String>` ↔ JSON 互转（领域对象保持 `List<String>`）。不引入 MyBatis-Plus TypeHandler——仅三处使用，转换逻辑集中在仓储实现内更直观。

`findById` 注意：`@TableLogic` 自动过滤已删除行，被删提醒查不到——完成历史快照列（2.5）即为该场景设计。

### 5.3 仓储实现要点

- `save`：id 为空 insert，否则 updateById，返回重查领域对象（与既有仓储一致）
- `findPendingBetween`：`LambdaQueryWrapper` eq status pending + between nextRemindTime，`is_deleted=0` 自动追加
- `deleteByPetId`：`LambdaUpdateWrapper` eq petId + set isDeleted=1（`@TableLogic` 下用 mapper.delete(wrapper) 即批量逻辑删除）
- `updateLastPushTime`：`LambdaUpdateWrapper` set lastPushTime，单列更新

---

## 6. 应用层设计

### 6.1 DTO/Command（application/dto/reminder）

| 类 | 说明 |
|----|------|
| CreateReminderCommand(petId, type, title, description, intervalType, intervalValue, nextRemindTime) | 创建命令 |
| UpdateReminderCommand(type, title, description, intervalType, intervalValue, nextRemindTime) | 编辑命令（petId 不可改） |
| CompleteReminderCommand(completionTime, notes, photoUrls) | 完成命令（completionTime 缺省=当前时间） |
| ReminderDTO(id, petId, petName, type, title, description, intervalType, intervalValue, nextRemindTime, status, lastCompletionTime, lastCompletionNotes, createdAt, updatedAt) | 提醒响应 |
| TodayReminderDTO(reminder: ReminderDTO, overdue: boolean, completedToday: boolean, completionTime) | 今日提醒合并视图项 |
| CompletionDTO(id, reminderId, petId, petName, reminderTitle, reminderType, completionTime, notes, photoUrls, eventRecordId, createdAt) | 完成历史响应 |
| EventRecordDTO(id, petId, source, eventType, title, content, eventDate, materialUrls, createdAt) | 事件响应 |

### 6.2 ReminderApplicationService（核心应用服务）

依赖：`ReminderRepository`、`ReminderCompletionRepository`、`EventRecordRepository`、`ReminderScheduleService`、`PetApplicationService`（归属校验+宠物名）。

```java
@Transactional create(command):
    Pet pet = checkPetOwnership(petId);                  // 复用 Phase 2 方法
    Reminder reminder = Reminder.create(petId, pet.getUserId(), type, ...);  // 内部校验
    save → log.info → ReminderDTO(petName)

@Transactional update(id, command):
    findOwnedReminder(id) → reminder.update(...) → save

@Transactional delete(id):
    findOwnedReminder(id) → repository.deleteById(id)    // 逻辑删除；完成历史保留（快照可读）

/** 完成闭环（本阶段核心事务） */
@Transactional complete(id, command):
    Reminder reminder = findOwnedReminder(id);           // 不存在/无权 → REMINDER_NOT_FOUND
    校验 completionTime ≤ now+5min（容差）
    LocalDateTime completionTime = command.completionTime() != null ? ... : now;

    // 1. 领域完成：一次性→finished；周期性→推进 nextRemindTime
    reminder.complete(completionTime, command.notes(), scheduleService);
    reminderRepository.save(reminder);

    // 2. 生成事件记录（type.generatesEvent() 才生成）
    EventRecord event = null;
    if (reminder.getType().generatesEvent()) {
        event = EventRecord.fromReminder(reminder, completion);
        eventRecordRepository.save(event);
    }

    // 3. 保存完成历史（含事件回链）
    ReminderCompletion completion = ReminderCompletion.create(reminder, userId, completionTime, notes, photoUrls);
    if (event != null) completion.assignEventRecordId(event.getId());
    completionRepository.save(completion);
    log.info("提醒完成: reminderId={}, petId={}, type={}, 下次={}, 事件={}", ...);
    → ReminderDTO（含推进后的 nextRemindTime）

@Transactional pause(id) / resume(id):
    findOwnedReminder → reminder.pause()/resume() → save

/** 首页今日提醒：待办（今日+逾期）与今日已完成合并 */
today():
    Long userId = current;
    List<Reminder> due = reminderRepository.findDueByUserId(userId, tomorrow 00:00);
    List<ReminderCompletion> doneToday = completionRepository.findByUserIdAndTimeBetween(userId, today 00:00, tomorrow 00:00);
    Map<Long, Pet> pets = 用户全部宠物 → petName 装配
    合并：due 项 {overdue = nextRemindTime < today 00:00, completedToday=false}
         doneToday 项 {completedToday=true, completionTime, overdue=false}
    排序：待办在前（逾期优先→按 nextRemindTime 升序），已完成在后（按 completionTime 倒序）

listByPet(petId, status?): checkPetOwnership → findByPetId → 按 status 过滤（可选）
getById(id): findOwnedReminder
history(reminderId): findOwnedReminder → completionRepository.findByReminderId（倒序）

/** 查询并校验归属（提醒不存在或不属于当前用户均按不存在处理） */
private Reminder findOwnedReminder(Long id):
    reminderRepository.findById(id).orElseThrow(REMINDER_NOT_FOUND)
    if (!reminder.getUserId().equals(current)) throw REMINDER_NOT_FOUND;   // 不泄露他人提醒存在性
```

### 6.3 EventRecordApplicationService（只读，轻量）

```
listByPetAndRange(petId, startDate, endDate):
    checkPetOwnership；范围 ≤366 天（与 Phase 2 一致）；返回 EventRecordDTO 列表
```

> 本阶段不提供事件写接口（手动创建属成长档案）。Phase 4+ 成长档案落地时扩展该服务。

### 6.4 ReminderPushService（推送组装，application）

```
/** 定时扫描入口（由 ReminderPushScheduler 每小时调用） */
scanAndPush():
    now = LocalDateTime.now()

    // 1. 到期提醒（含逾期）：nextRemindTime ≤ now
    for reminder in reminderRepository.findPendingBetween(任意过去, now):
        if (已推送今日) continue;               // lastPushTime 日期 == today → 跳过
        push(reminder, "到期提醒")               // 组装："{宠物名}：{title}" / 类型文案
        updateLastPushTime(reminder.getId(), now)

    // 2. 明日重要提醒（疫苗/体检/驱虫 提前 1 天）：nextRemindTime ∈ 明日
    for reminder in reminderRepository.findPendingBetween(明日00:00, 明日23:59:59):
        if (!reminder.getType().isImportant()) continue;
        if (已推送今日) continue;
        push(reminder, "明日提醒")               // "{宠物名}明天该{title}了"
        updateLastPushTime(...)

push(reminder, kind):
    // 内容组装完整（宠物名需查 pet），当前网关为日志输出：
    log.info("[提醒推送-{}] petId={}, type={}, title={}, remindTime={}", kind, ...)
    // 微信订阅消息接入点：此处替换/追加真实发送（见 8.3）
```

宠物名获取：`petRepository.findById(petId)`；宠物已删除（理论不该发生——删除宠物级联删提醒，见 6.5）则降级用 title。

### 6.5 与宠物模块的集成点

**删除宠物级联删除提醒**：`PetApplicationService.deletePet` 中追加 `reminderRepository.deleteByPetId(petId)`（同一事务）。提醒是「活着的待办」而非历史数据，宠物删除后不应继续被扫描推送；完成历史与事件记录保留（快照可独立展示，与 Phase 2「记录保留不清理」一致）。

依赖方向注意：`PetApplicationService`（application）注入 `ReminderRepository`（domain 接口）——同层模块间经领域仓储协作，无需反向依赖 ReminderApplicationService。

---

## 7. 接口层设计

### 7.1 ReminderController（/api/reminders）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/reminders` | 创建提醒 |
| GET | `/api/reminders/today` | 今日提醒（待办+逾期+今日已完成，跨宠物） |
| GET | `/api/reminders?petId=&status=` | 宠物提醒列表（status 可选：pending/paused/finished） |
| GET | `/api/reminders/{id}` | 提醒详情（编辑页回显） |
| PUT | `/api/reminders/{id}` | 编辑提醒 |
| DELETE | `/api/reminders/{id}` | 删除提醒（逻辑删除） |
| POST | `/api/reminders/{id}/complete` | 完成提醒（完成闭环） |
| POST | `/api/reminders/{id}/pause` | 暂停提醒 |
| POST | `/api/reminders/{id}/resume` | 恢复提醒 |
| GET | `/api/reminders/{id}/history` | 完成历史 |

### 7.2 EventRecordController（/api/event-records）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/event-records?petId=&startDate=&endDate=` | 事件记录范围查询（闭环验证/时间轴预览，升序） |

### 7.3 请求 DTO（interfaces/dto/reminder）

```java
CreateReminderRequest {
    @NotNull Long petId;
    @NotBlank @Size(max = 200) String title;
    @Size(max = 500) String description;
    @NotBlank String type;               // ReminderType code
    @NotBlank String intervalType;       // IntervalType code
    Integer intervalValue;               // 语义校验在领域层
    @NotNull LocalDateTime nextRemindTime;
}

UpdateReminderRequest          // 同上，去 petId
CompleteReminderRequest {      // 全选填
    LocalDateTime completionTime;        // 缺省=当前时间，≤now+5min
    @Size(max = 500) String notes;
    @Size(max = 9) List<String> photoUrls;
}
```

枚举 code 解析复用 Phase 2 模式：应用服务 `parseEnum(Xxx::fromCode, code, "字段名")`，非法值 → `PARAM_INVALID`。

鉴权：`/api/reminders/**`、`/api/event-records/**` 均需登录，SaTokenConfig 现有规则（`/**` 拦截 + 白名单排除）已覆盖，**无需改动**。

---

## 8. 定时任务与推送设计

### 8.1 调度启用

启动类 `FaFaApplication` 追加 `@EnableScheduling`（项目当前未启用 Spring 调度）。

### 8.2 ReminderPushScheduler（infrastructure/schedule）

```java
@Component
public class ReminderPushScheduler {
    @Scheduled(cron = "0 0 * * * *")   // 每小时整点
    public void scanAndPush() {
        try {
            reminderPushService.scanAndPush();
        } catch (Exception ex) {
            log.error("提醒扫描推送异常", ex);   // 调度异常不抛出，等下一轮
        }
    }
}
```

扫描与去重规则（业务规则汇总见第 9 节 R13-R15）：
- **到期推送**：`pending && nextRemindTime ≤ now` → 「该 XX 了」
- **逾期持续提醒**：逾期提醒天然命中到期条件，`last_push_time` 同日去重实现「每天一次直到完成」
- **明日重要提醒**：`pending && 类型 isImportant && nextRemindTime ∈ 明日` → 「明天该 XX 了」

### 8.3 微信订阅消息接入预留（本阶段不实现）

接入条件与步骤（届时执行）：
1. 小程序类目审核通过后，在微信公众平台「订阅消息」申请模板（计划文档 7.7 的 thing1/thing2/time3 三字段模板）
2. `application.yml` 增加 `fafa.wechat.subscribe-template-id` 配置
3. `WeChatClient` 扩展：access_token 管理（已有 appid/secret 配置）+ `subscribeMessage.send` 调用
4. 小程序端在创建提醒时调用 `wx.requestSubscribeMessage` 获取用户订阅授权（一次性订阅，每推送一次消耗一次授权）
5. `ReminderPushService.push()` 处追加真实发送调用（失败仅告警，不影响扫描主流程）

### 8.4 多实例部署说明

当前单实例部署，调度无重复执行问题。未来多实例时：项目已引入 Redisson，在 `scanAndPush` 外层加 `RLock`（tryLock 即走，抢不到跳过本轮）即可，无需改表结构。

---

## 9. 业务规则汇总（开发校验清单）

| # | 规则 | 层 |
|---|------|----|
| R1 | petId/type/title/intervalType/nextRemindTime 必填 | 领域 |
| R2 | title ≤200；description ≤500；完成 notes ≤500 | 领域 |
| R3 | nextRemindTime 晚于当前时间（创建与编辑） | 领域 |
| R4 | intervalValue：days/months ≥1；weekly 1~7（ISO 周一~周日）；monthly 1~31；once/yearly 忽略 | 领域服务 |
| R5 | 枚举 code 非法 → PARAM_INVALID（类型/周期/状态/来源） | 应用 |
| R6 | 创建/列表前校验宠物归属（checkPetOwnership） | 应用 |
| R7 | 提醒不存在或不属于当前用户 → REMINDER_NOT_FOUND（不泄露存在性） | 应用 |
| R8 | complete 仅 pending 可执行；一次性→finished；周期性→推进 nextRemindTime 保持 pending | 领域 |
| R9 | pause 仅 pending；resume 仅 paused；暂停期间不扫描不展示 | 领域 |
| R10 | 下期推算以被完成期计划时间为基准；结果已过期则以当前时间重算一次 | 领域服务 |
| R11 | completionTime ≤ 当前时间+5 分钟容差（可补记，不可未来） | 应用 |
| R12 | photoUrls ≤9 张；完成历史与事件事务内同写（完成闭环原子性） | 应用 |
| R13 | 定时扫描：到期（≤now）+ 明日重要（vaccine/checkup/deworming）两类推送 | 推送 |
| R14 | 同一提醒每日至多推送一次（lastPushTime 同日跳过） | 推送 |
| R15 | paused/finished/deleted 提醒不参与扫描与今日列表 | 推送/查询 |
| R16 | 事件生成白名单：vaccine/deworming/bath/nail_trim/checkup/custom；birthday/daily_record/medication 不生成 | 领域 |
| R17 | 删除宠物 → 同事务级联逻辑删除其提醒；完成历史/事件保留 | 应用 |
| R18 | 事件/完成历史范围查询 ≤366 天 | 应用 |

---

## 10. ErrorCode 新增（50xxx 提醒段）

```java
// 提醒模块 50xxx
REMINDER_NOT_FOUND(50001, "提醒不存在"),
REMINDER_TIME_IN_PAST(50002, "提醒时间不能早于当前时间"),
REMINDER_INTERVAL_INVALID(50003, "周期配置无效"),
REMINDER_STATE_INVALID(50004, "提醒当前状态不支持该操作"),
REMINDER_COMPLETION_TIME_INVALID(50005, "完成时间不能晚于当前时间"),
```

（ErrorCode 枚举头注释已预规划 50xxx 提醒段，直接追加。）

---

## 11. 单元测试计划（领域层，与 Phase 2 同规格）

| 测试类 | 覆盖点 |
|--------|--------|
| ReminderTest | create 必填/长度/过去时间校验；complete 一次性→finished、周期性→nextRemindTime 推进且保持 pending；complete 非 pending 抛异常；pause/resume 状态机与非法转移；update 覆盖与校验；isOverdue/markPushed |
| ReminderScheduleServiceTest | 六种周期 nextAfter 基准推算；weekly 同日+7/跨日；monthly 月末钳制（31→2月28/29、→30天月30）；monthly 过期滚动（9/1 基准、9/20 完成→10/1）；days 过期滚动；validateInterval 全部分支 |
| ReminderCompletionTest | create 快照字段（标题/类型取自提醒）；notes/photoUrls 边界 |
| EventRecordTest | fromReminder 映射（source=reminder、eventType 映射、title/content/eventDate/materialUrls 来源）；restore |
| 枚举测试 | ReminderType.generatesEvent/isImportant 矩阵；五枚举 fromCode 大小写/非法值 |

集成验证（Knife4j 手工）：创建→today 待办→complete→today 已完成划线+下期推进→history→event-records 可见。

---

## 12. 前端对接要点（信息同步，非本阶段后端交付）

### 12.1 首页「今日提醒」区
- `GET /api/reminders/today` 单接口渲染：待办在前（逾期优先、按提醒时间升序，逾期项红色标记）、今日已完成在后（划线，不立即消失）
- 点击待办展开「已完成」（跳完成表单）与「推迟」（编辑 nextRemindTime）

### 12.2 打卡提醒联动
- 用户配置 `daily_record` 类型提醒（建议 intervalType=days, intervalValue=1）
- 打卡页提交成功后，前端自动调用 `POST /api/reminders/{id}/complete` 关闭当日待办（后端不做模块耦合）

### 12.3 创建/编辑提醒页
- 类型 chips（8 预设 + 自定义）；周期选择器按 intervalType 渲染（每N天/每周星期几/每月几号/每N月/每年/一次性）
- photoUrls 上传复用现有照片上传交互（StorageService 通用能力），完成后传 URL 列表

---

## 13. 开发任务清单（执行顺序）

1. ErrorCode 50xxx + 领域枚举（5 个）
2. Reminder / ReminderCompletion / EventRecord 实体 + ReminderScheduleService
3. 三个仓储接口 + PO/Mapper/RepositoryImpl（JSON 列转换）
4. ReminderApplicationService（含完成闭环事务）+ EventRecordApplicationService
5. ReminderController / EventRecordController + 请求/响应 DTO
6. @EnableScheduling + ReminderPushScheduler + ReminderPushService
7. PetApplicationService.deletePet 级联删除提醒
8. 领域层单元测试（第 11 节）
9. `mvn compile` + `mvn test` 全量验证
10. 文档收尾（本文档状态更新、代码审查清单自查）

---

## 14. 验收标准（对照计划文档 7.11，含偏差标注）

**数据库**：
- [x] reminder / reminder_completion_history / event_record 三表已创建（MCP 执行 + Flyway V3 同步 + 表记录文档更新）

**后端代码**：
- [x] 领域模型完成：Reminder 含周期推进与状态机；ReminderScheduleService 六种周期计算
- [x] 三组仓储接口及实现完成（JSON 列映射正确）
- [x] ReminderApplicationService 完成闭环事务（完成 → 事件生成 → 历史落库 → 下期推进，原子性）
- [ ] ReminderController / EventRecordController 通过 Knife4j 测试（代码已交付，待联调验证）

**关键功能**：
- [x] 完成闭环正确：一次性→finished；周期性→下期按计划基准推进；事件按 R16 白名单生成（ReminderTest/ReminderCompletionTest/EventRecordTest 覆盖）
- [x] 周期计算正确：六种周期 + 月末钳制 + 过期滚动单元测试全过（ReminderScheduleServiceTest 15 例）
- [x] 今日提醒视图正确：待办（含逾期标记）+ 今日已完成合并（应用层实现，待联调验证）
- [x] 暂停/恢复/编辑/删除状态机正确（ReminderTest 19 例覆盖）

**定时任务**：
- [x] 每小时扫描代码交付（ReminderPushScheduler + ReminderPushService，含同日去重），运行验证待联调
- [ ] ~~微信订阅消息模板配置完成~~ → **偏差（2.3）**：接入点与步骤已预留（8.3），真实发送延后至小程序发布流程

**文档**：
- [x] MySQL数据表记录.md 已更新 Phase 3 表结构
- [x] API 文档通过 Knife4j 自动生成（注解已标注，随应用启动生成）
- [x] 本文档开发完成后状态更新为「已完成」

---

> 本文档创建：2026-08-22
> 开发完成：2026-08-22（`mvn compile` 通过；`mvn test` 95 例全过，其中 Phase 3 新增 5 个测试类 48 例）
> 下一步：启动应用进行联调测试（Knife4j 接口验证、定时扫描运行观察），之后进入 Phase 4 规划。
