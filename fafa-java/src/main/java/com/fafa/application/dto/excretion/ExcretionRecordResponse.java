package com.fafa.application.dto.excretion;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 排便记录响应
 */
@Data
@Builder
public class ExcretionRecordResponse {
    
    private Long id;
    
    private Long petId;
    
    private LocalDateTime excretionTime;
    
    private String type;
    
    private String color;
    
    private String shape;
    
    private Boolean abnormal;
    
    private String remarks;
}
