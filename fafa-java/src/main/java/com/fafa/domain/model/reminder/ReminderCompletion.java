package com.fafa.domain.model.reminder;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 提醒完成记录（追加只写，不修改不删除）
 *
 * 冗余提醒标题/类型快照：提醒被逻辑删除后，完成历史与「今日已完成」列表仍可独立展示。
 *
 * 业务规则：
 * 1. completionTime 必填（不可晚于当前时间，应用层容差 +5 分钟）
 * 2. notes ≤500；photoUrls ≤9 张
 *
 * @author FaFa Team
 * @since 3.0
 */
@Getter
public class ReminderCompletion {

    private static final int MAX_NOTES_LENGTH = 500;
    private static final int MAX_PHOTO_COUNT = 9;

    private Long id;
    private final Long reminderId;
    private final Long petId;
    /** 完成人ID（家庭协同预留，现阶段=宠物主人） */
    private final Long userId;
    /** 提醒标题快照 */
    private final String reminderTitle;
    /** 提醒类型快照（code） */
    private final String reminderType;
    private final LocalDateTime completionTime;
    private final String notes;
    private final List<String> photoUrls;
    /** 关联生成的事件记录ID（保存事件后回填） */
    private Long eventRecordId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private ReminderCompletion(Long id, Long reminderId, Long petId, Long userId,
                               String reminderTitle, String reminderType,
                               LocalDateTime completionTime, String notes, List<String> photoUrls,
                               Long eventRecordId, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.reminderId = reminderId;
        this.petId = petId;
        this.userId = userId;
        this.reminderTitle = reminderTitle;
        this.reminderType = reminderType;
        this.completionTime = completionTime;
        this.notes = notes;
        this.photoUrls = photoUrls;
        this.eventRecordId = eventRecordId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 创建完成记录（标题/类型取自提醒快照）
     */
    public static ReminderCompletion create(Reminder reminder, Long userId,
                                            LocalDateTime completionTime, String notes,
                                            List<String> photoUrls) {
        Objects.requireNonNull(reminder, "reminder 不能为空");
        Objects.requireNonNull(userId, "userId 不能为空");
        Objects.requireNonNull(completionTime, "completionTime 不能为空");
        validateNotes(notes);
        validatePhotoUrls(photoUrls);

        return new ReminderCompletion(null, reminder.getId(), reminder.getPetId(), userId,
                reminder.getTitle(), reminder.getType().getCode(),
                completionTime, notes, photoUrls, null, null, null);
    }

    /**
     * 仓储重建
     */
    public static ReminderCompletion restore(Long id, Long reminderId, Long petId, Long userId,
                                             String reminderTitle, String reminderType,
                                             LocalDateTime completionTime, String notes,
                                             List<String> photoUrls, Long eventRecordId,
                                             LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new ReminderCompletion(id, reminderId, petId, userId,
                reminderTitle, reminderType, completionTime, notes, photoUrls,
                eventRecordId, createdAt, updatedAt);
    }

    /**
     * 回填关联的事件记录ID
     */
    public void assignEventRecordId(Long eventRecordId) {
        this.eventRecordId = eventRecordId;
    }

    private static void validateNotes(String notes) {
        if (notes != null && notes.length() > MAX_NOTES_LENGTH) {
            throw new IllegalArgumentException("完成备注不能超过 " + MAX_NOTES_LENGTH + " 字符");
        }
    }

    private static void validatePhotoUrls(List<String> photoUrls) {
        if (photoUrls != null && photoUrls.size() > MAX_PHOTO_COUNT) {
            throw new IllegalArgumentException("完成照片不能超过 " + MAX_PHOTO_COUNT + " 张");
        }
    }
}
