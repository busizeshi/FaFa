package com.fafa.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 提醒 DO
 *
 * @author FaFa
 * @since 2026-08-18
 */
@Data
@TableName("reminder")
public class ReminderDO {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long petId;
    
    private Long userId;
    
    private String title;
    
    private String reminderType;
    
    private LocalDateTime remindTime;
    
    private String repeatType;
    
    private String repeatConfig;
    
    private Integer advanceMinutes;
    
    private String description;
    
    private String status;
    
    private LocalDateTime completedAt;
    
    private String completionNote;
    
    private String completionImages; // JSON 字符串
    
    private Boolean isNotified;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
