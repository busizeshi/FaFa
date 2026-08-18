package com.fafa.application.dto.weight;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 体重记录响应
 */
@Data
@Builder
public class WeightRecordResponse {
    
    private Long id;
    
    private Long petId;
    
    private LocalDate recordDate;
    
    private BigDecimal weight;
    
    private Integer bcsScore;
    
    private String remarks;
}
