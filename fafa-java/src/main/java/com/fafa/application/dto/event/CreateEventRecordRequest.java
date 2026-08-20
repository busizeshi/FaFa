package com.fafa.application.dto.event;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 创建事件记录请求
 */
@Data
public class CreateEventRecordRequest {
    
    @NotNull(message = "宠物ID不能为空")
    private Long petId;
    
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
