package com.fafa.application.dto.reminder;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创建提醒请求
 *
 * @author FaFa
 * @since 2026-08-18
 */
@Data
public class CreateReminderRequest {
    
    private Long petId;
    
    private String title;
    
    private String reminderType; // feed, vaccine, deworming, bath, grooming, vet, medicine, other
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime remindTime;
    
    private String repeatType; // once, daily, weekly, monthly, custom
    
    private String repeatConfig; // JSON 字符串，如 {"days": [1,3,5]}
    
    private Integer advanceMinutes;
    
    private String description;
}
