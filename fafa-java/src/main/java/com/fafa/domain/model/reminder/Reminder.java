package com.fafa.domain.model.reminder;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 提醒聚合根
 *
 * @author FaFa
 * @since 2026-08-18
 */
@Data
@Builder
public class Reminder {
    
    private ReminderId reminderId;
    private Long petId;
    private Long userId;
    private String title;
    private String reminderType; // feed, vaccine, deworming, bath, grooming, vet, medicine, other
    private LocalDateTime remindTime;
    private String repeatType; // once, daily, weekly, monthly, custom
    private String repeatConfig; // JSON 字符串
    private Integer advanceMinutes;
    private String description;
    private String status; // pending, completed, cancelled, expired
    private LocalDateTime completedAt;
    private String completionNote;
    private List<String> completionImages;
    private Boolean isNotified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * 创建提醒
     */
    public static Reminder create(Long petId, Long userId, String title, String reminderType,
                                   LocalDateTime remindTime, String repeatType, String repeatConfig,
                                   Integer advanceMinutes, String description) {
        return Reminder.builder()
                .petId(petId)
                .userId(userId)
                .title(title)
                .reminderType(reminderType)
                .remindTime(remindTime)
                .repeatType(repeatType != null ? repeatType : "once")
                .repeatConfig(repeatConfig)
                .advanceMinutes(advanceMinutes != null ? advanceMinutes : 0)
                .description(description)
                .status("pending")
                .isNotified(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * 更新提醒信息
     */
    public void update(String title, String reminderType, LocalDateTime remindTime,
                       String repeatType, String repeatConfig, Integer advanceMinutes,
                       String description) {
        if (title != null) {
            this.title = title;
        }
        if (reminderType != null) {
            this.reminderType = reminderType;
        }
        if (remindTime != null) {
            this.remindTime = remindTime;
        }
        if (repeatType != null) {
            this.repeatType = repeatType;
        }
        if (repeatConfig != null) {
            this.repeatConfig = repeatConfig;
        }
        if (advanceMinutes != null) {
            this.advanceMinutes = advanceMinutes;
        }
        if (description != null) {
            this.description = description;
        }
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 完成提醒
     */
    public void complete(String completionNote, List<String> completionImages) {
        this.status = "completed";
        this.completedAt = LocalDateTime.now();
        this.completionNote = completionNote;
        this.completionImages = completionImages;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 取消提醒
     */
    public void cancel() {
        this.status = "cancelled";
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 标记为已通知
     */
    public void markAsNotified() {
        this.isNotified = true;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 标记为已过期
     */
    public void markAsExpired() {
        this.status = "expired";
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 是否是重复提醒
     */
    public boolean isRepeating() {
        return !"once".equals(this.repeatType);
    }
    
    /**
     * 是否可以完成
     */
    public boolean canBeCompleted() {
        return "pending".equals(this.status);
    }
    
    /**
     * 是否可以编辑
     */
    public boolean canBeEdited() {
        return "pending".equals(this.status);
    }
}
