package com.fafa.application.dto.water;

import lombok.Builder;
import lombok.Data;

/**
 * 饮水统计响应
 */
@Data
@Builder
public class WaterStatisticsResponse {
    
    /**
     * 总饮水量（ml）
     */
    private Integer totalAmount;
    
    /**
     * 平均每日饮水量（ml）
     */
    private Integer averageAmount;
    
    /**
     * 统计天数
     */
    private Integer days;
}
