package com.fafa.application.dto.water;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 饮水记录响应
 */
@Data
@Builder
public class WaterRecordResponse {
    
    private Long id;
    
    private Long petId;
    
    private LocalDateTime waterTime;
    
    private Integer amount;
    
    private String remarks;
}
