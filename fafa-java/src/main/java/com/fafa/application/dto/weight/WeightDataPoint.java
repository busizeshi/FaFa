package com.fafa.application.dto.weight;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 体重数据点
 */
@Data
@Builder
public class WeightDataPoint {
    
    private LocalDate date;
    
    private BigDecimal weight;
    
    private Integer bcsScore;
}
