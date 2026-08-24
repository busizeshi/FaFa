package com.fafa.domain.model.reminder;

import com.fafa.domain.service.ReminderScheduleService;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 提醒（聚合根）
 *
 * 一行 = 提醒定义 + 当前周期状态。无持久 completed 态：
 * 周期性提醒完成后原地推进 nextRemindTime 保持 pending，一次性提醒完成后进入 finished。
 *
 * 业务规则：
 * 1. petId/userId/type/title/intervalType/nextRemindTime 必填
 * 2. title ≤200，description ≤500
 * 3. nextRemindTime 必须晚于当前时间（创建与编辑）
 * 4. 状态转移：complete/pause 仅 pending；resume 仅 paused
 *
 * @author FaFa Team
 * @since 3.0
 */
@Getter
public class Reminder {

    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MAX_DESCRIPTION_LENGTH = 500;

    private Long id;
    private final Long petId;
    private final Long userId;

    private ReminderType type;
    private String title;
    private String description;

    private IntervalType intervalType;
    /** 周期值语义随 intervalType，见 IntervalType */
    private Integer intervalValue;
    private LocalDateTime nextRemindTime;

    private ReminderStatus status;

    private LocalDateTime lastCompletionTime;
    private String lastCompletionNotes;
    /** 最近一次推送时间（同一提醒每日至多推送一次） */
    private LocalDateTime lastPushTime;

    /** 关联用药ID（Phase 6 用药管理预留） */
    private final Long medicationId;
    private boolean deleted;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Reminder(Long id, Long petId, Long userId, ReminderType type, String title,
                     String description, IntervalType intervalType, Integer intervalValue,
                     LocalDateTime nextRemindTime, ReminderStatus status,
                     LocalDateTime lastCompletionTime, String lastCompletionNotes,
                     LocalDateTime lastPushTime, Long medicationId, boolean deleted,
                     LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.petId = petId;
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.description = description;
        this.intervalType = intervalType;
        this.intervalValue = intervalValue;
        this.nextRemindTime = nextRemindTime;
        this.status = status;
        this.lastCompletionTime = lastCompletionTime;
        this.lastCompletionNotes = lastCompletionNotes;
        this.lastPushTime = lastPushTime;
        this.medicationId = medicationId;
        this.deleted = deleted;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 创建提醒
     *
     * @param now 当前时间（注入便于测试，校验首提醒时间不早于当前）
     */
    public static Reminder create(Long petId, Long userId, ReminderType type, String title,
                                  String description, IntervalType intervalType, Integer intervalValue,
                                  LocalDateTime nextRemindTime, LocalDateTime now) {
        Objects.requireNonNull(petId, "petId 不能为空");
        Objects.requireNonNull(userId, "userId 不能为空");
        Objects.requireNonNull(type, "type 不能为空");
        Objects.requireNonNull(intervalType, "intervalType 不能为空");
        Objects.requireNonNull(nextRemindTime, "nextRemindTime 不能为空");
        validateTitle(title);
        validateDescription(description);
        if (!nextRemindTime.isAfter(now)) {
            throw new IllegalArgumentException("提醒时间不能早于当前时间");
        }

        return createInternal(petId, userId, type, title, description, intervalType,
                intervalValue, nextRemindTime, null, now);
    }

    /**
     * 创建由用药计划拥有的提醒。普通提醒接口不得使用 medication 类型直接创建。
     */
    public static Reminder createMedication(Long petId, Long userId, Long medicationPlanId,
                                             String title, String description,
                                             IntervalType intervalType, Integer intervalValue,
                                             LocalDateTime nextRemindTime, LocalDateTime now) {
        Objects.requireNonNull(medicationPlanId, "medicationPlanId 不能为空");
        return createInternal(petId, userId, ReminderType.MEDICATION, title, description,
                intervalType, intervalValue, nextRemindTime, medicationPlanId, now);
    }

    private static Reminder createInternal(Long petId, Long userId, ReminderType type,
                                           String title, String description,
                                           IntervalType intervalType, Integer intervalValue,
                                           LocalDateTime nextRemindTime, Long medicationPlanId,
                                           LocalDateTime now) {
        Objects.requireNonNull(petId, "petId 不能为空");
        Objects.requireNonNull(userId, "userId 不能为空");
        Objects.requireNonNull(type, "type 不能为空");
        Objects.requireNonNull(intervalType, "intervalType 不能为空");
        Objects.requireNonNull(nextRemindTime, "nextRemindTime 不能为空");
        validateTitle(title);
        validateDescription(description);
        Objects.requireNonNull(now, "now 不能为空");
        if (!nextRemindTime.isAfter(now)) {
            throw new IllegalArgumentException("提醒时间不能早于当前时间");
        }
        return new Reminder(null, petId, userId, type, title, description,
                intervalType, intervalValue, nextRemindTime, ReminderStatus.PENDING,
                null, null, null, medicationPlanId, false, null, null);
    }

    /**
     * 仓储重建
     */
    public static Reminder restore(Long id, Long petId, Long userId, ReminderType type, String title,
                                   String description, IntervalType intervalType, Integer intervalValue,
                                   LocalDateTime nextRemindTime, ReminderStatus status,
                                   LocalDateTime lastCompletionTime, String lastCompletionNotes,
                                   LocalDateTime lastPushTime, Long medicationId, boolean deleted,
                                   LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Reminder(id, petId, userId, type, title, description,
                intervalType, intervalValue, nextRemindTime, status,
                lastCompletionTime, lastCompletionNotes, lastPushTime, medicationId, deleted,
                createdAt, updatedAt);
    }

    /**
     * 完成提醒
     *
     * 业务规则：
     * 1. 仅 pending 可完成
     * 2. 记录完成时间与备注
     * 3. 一次性提醒 → finished 终态；周期性提醒 → 以被完成期计划时间为基准推算下期，保持 pending
     *
     * @param completionTime 实际完成时间（可晚于计划提醒时间）
     * @param notes          完成备注
     * @param schedule       周期推算领域服务
     */
    public void complete(LocalDateTime completionTime, String notes, ReminderScheduleService schedule) {
        if (status != ReminderStatus.PENDING) {
            throw new IllegalStateException("仅生效中的提醒可以完成");
        }
        Objects.requireNonNull(completionTime, "completionTime 不能为空");
        validateNotes(notes);

        this.lastCompletionTime = completionTime;
        this.lastCompletionNotes = notes;

        if (intervalType == IntervalType.ONCE) {
            this.status = ReminderStatus.FINISHED;
        } else {
            // 以被完成期的计划时间为基准推算下期；结果已过期则以完成时间重算（过期滚动）
            this.nextRemindTime = schedule.nextAfter(nextRemindTime, intervalType, intervalValue, completionTime);
        }
    }

    /**
     * 覆盖更新编辑字段（标题/描述/类型/周期/下次提醒时间），保持状态不变
     *
     * @param now 当前时间（校验新提醒时间不早于当前）
     */
    public void update(ReminderType type, String title, String description,
                       IntervalType intervalType, Integer intervalValue,
                       LocalDateTime nextRemindTime, LocalDateTime now) {
        Objects.requireNonNull(type, "type 不能为空");
        Objects.requireNonNull(intervalType, "intervalType 不能为空");
        Objects.requireNonNull(nextRemindTime, "nextRemindTime 不能为空");
        validateTitle(title);
        validateDescription(description);
        if (!nextRemindTime.isAfter(now)) {
            throw new IllegalArgumentException("提醒时间不能早于当前时间");
        }

        this.type = type;
        this.title = title;
        this.description = description;
        this.intervalType = intervalType;
        this.intervalValue = intervalValue;
        this.nextRemindTime = nextRemindTime;
    }

    /**
     * 暂停提醒（仅 pending 可暂停）
     */
    public void pause() {
        if (status != ReminderStatus.PENDING) {
            throw new IllegalStateException("仅生效中的提醒可以暂停");
        }
        this.status = ReminderStatus.PAUSED;
    }

    /**
     * 恢复提醒（仅 paused 可恢复）
     *
     * nextRemindTime 不重算：暂停期间过期则恢复后即为逾期态。
     */
    public void resume() {
        if (status != ReminderStatus.PAUSED) {
            throw new IllegalStateException("仅已暂停的提醒可以恢复");
        }
        this.status = ReminderStatus.PENDING;
    }

    /**
     * 是否逾期（生效中且下次提醒时间已过）
     */
    public boolean isOverdue(LocalDateTime now) {
        return status == ReminderStatus.PENDING && nextRemindTime.isBefore(now);
    }

    /**
     * 标记已推送（推送同日去重）
     */
    public void markPushed(LocalDateTime pushTime) {
        this.lastPushTime = pushTime;
    }

    /**
     * 逻辑删除
     */
    public void delete() {
        this.deleted = true;
    }

    private static void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("提醒标题不能为空");
        }
        if (title.length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException("提醒标题不能超过 " + MAX_TITLE_LENGTH + " 字符");
        }
    }

    private static void validateDescription(String description) {
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("提醒描述不能超过 " + MAX_DESCRIPTION_LENGTH + " 字符");
        }
    }

    private static void validateNotes(String notes) {
        if (notes != null && notes.length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("完成备注不能超过 " + MAX_DESCRIPTION_LENGTH + " 字符");
        }
    }
}
