package com.fafa.application.dto.event;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 事件记录响应
 */
@Data
@Builder
public class EventRecordResponse {
    
    private Long id;
    
    private Long petId;
    
    private LocalDateTime eventTime;
    
    private String eventType;
    
    private String title;
    
    private String content;
    
    private String images;
    
    private String remarks;
}
