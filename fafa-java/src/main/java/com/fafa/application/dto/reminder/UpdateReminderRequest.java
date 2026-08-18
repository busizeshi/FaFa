package com.fafa.application.dto.reminder;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 更新提醒请求
 *
 * @author FaFa
 * @since 2026-08-18
 */
@Data
public class UpdateReminderRequest {
    
    private String title;
    
    private String reminderType;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime remindTime;
    
    private String repeatType;
    
    private String repeatConfig;
    
    private Integer advanceMinutes;
    
    private String description;
}
