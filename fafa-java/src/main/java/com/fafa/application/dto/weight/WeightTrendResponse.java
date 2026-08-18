package com.fafa.application.dto.weight;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 体重趋势响应
 */
@Data
@Builder
public class WeightTrendResponse {
    
    /**
     * 趋势：increasing/decreasing/stable
     */
    private String trend;
    
    /**
     * 趋势描述消息
     */
    private String message;
    
    /**
     * 数据点列表
     */
    private List<WeightDataPoint> dataPoints;
    
    /**
     * 最大体重
     */
    private BigDecimal maxWeight;
    
    /**
     * 最小体重
     */
    private BigDecimal minWeight;
    
    /**
     * 当前体重
     */
    private BigDecimal currentWeight;
}
