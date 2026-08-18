package com.fafa.application.dto.reminder;

import lombok.Data;

import java.util.List;

/**
 * 完成提醒请求
 *
 * @author FaFa
 * @since 2026-08-18
 */
@Data
public class CompleteReminderRequest {
    
    private String completionNote;
    
    private List<String> completionImages;
}
