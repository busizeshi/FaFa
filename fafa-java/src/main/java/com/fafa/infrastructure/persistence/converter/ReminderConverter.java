package com.fafa.infrastructure.persistence.converter;

import cn.hutool.json.JSONUtil;
import com.fafa.domain.model.reminder.Reminder;
import com.fafa.domain.model.reminder.ReminderId;
import com.fafa.infrastructure.persistence.dataobject.ReminderDO;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 提醒转换器
 *
 * @author FaFa
 * @since 2026-08-18
 */
@Component
public class ReminderConverter {
    
    /**
     * DO 转领域模型
     */
    public Reminder toDomain(ReminderDO reminderDO) {
        if (reminderDO == null) {
            return null;
        }
        
        List<String> completionImages = null;
        if (reminderDO.getCompletionImages() != null && !reminderDO.getCompletionImages().isEmpty()) {
            completionImages = JSONUtil.toList(reminderDO.getCompletionImages(), String.class);
        }
        
        return Reminder.builder()
                .reminderId(ReminderId.of(reminderDO.getId()))
                .petId(reminderDO.getPetId())
                .userId(reminderDO.getUserId())
                .title(reminderDO.getTitle())
                .reminderType(reminderDO.getReminderType())
                .remindTime(reminderDO.getRemindTime())
                .repeatType(reminderDO.getRepeatType())
                .repeatConfig(reminderDO.getRepeatConfig())
                .advanceMinutes(reminderDO.getAdvanceMinutes())
                .description(reminderDO.getDescription())
                .status(reminderDO.getStatus())
                .completedAt(reminderDO.getCompletedAt())
                .completionNote(reminderDO.getCompletionNote())
                .completionImages(completionImages)
                .isNotified(reminderDO.getIsNotified())
                .createdAt(reminderDO.getCreatedAt())
                .updatedAt(reminderDO.getUpdatedAt())
                .build();
    }
    
    /**
     * 领域模型转 DO
     */
    public ReminderDO toDO(Reminder reminder) {
        if (reminder == null) {
            return null;
        }
        
        ReminderDO reminderDO = new ReminderDO();
        if (reminder.getReminderId() != null) {
            reminderDO.setId(reminder.getReminderId().getValue());
        }
        reminderDO.setPetId(reminder.getPetId());
        reminderDO.setUserId(reminder.getUserId());
        reminderDO.setTitle(reminder.getTitle());
        reminderDO.setReminderType(reminder.getReminderType());
        reminderDO.setRemindTime(reminder.getRemindTime());
        reminderDO.setRepeatType(reminder.getRepeatType());
        reminderDO.setRepeatConfig(reminder.getRepeatConfig());
        reminderDO.setAdvanceMinutes(reminder.getAdvanceMinutes());
        reminderDO.setDescription(reminder.getDescription());
        reminderDO.setStatus(reminder.getStatus());
        reminderDO.setCompletedAt(reminder.getCompletedAt());
        reminderDO.setCompletionNote(reminder.getCompletionNote());
        
        if (reminder.getCompletionImages() != null && !reminder.getCompletionImages().isEmpty()) {
            reminderDO.setCompletionImages(JSONUtil.toJsonStr(reminder.getCompletionImages()));
        }
        
        reminderDO.setIsNotified(reminder.getIsNotified());
        reminderDO.setCreatedAt(reminder.getCreatedAt());
        reminderDO.setUpdatedAt(reminder.getUpdatedAt());
        
        return reminderDO;
    }
}
