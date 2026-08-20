package com.fafa.application.dto.event;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 更新事件记录请求
 */
@Data
public class UpdateEventRecordRequest {
    
    @NotNull(message = "事件时间不能为空")
    private LocalDateTime eventTime;
    
    @NotBlank(message = "事件类型不能为空")
    private String eventType;
    
    @NotBlank(message = "事件标题不能为空")
    private String title;
    
    private String content;
    
    private String images;
    
    private String remarks;
}
