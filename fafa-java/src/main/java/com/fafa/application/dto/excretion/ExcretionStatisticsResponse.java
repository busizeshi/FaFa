package com.fafa.application.dto.excretion;

import lombok.Builder;
import lombok.Data;

/**
 * 排便统计响应
 */
@Data
@Builder
public class ExcretionStatisticsResponse {
    
    /**
     * 总次数
     */
    private Integer totalCount;
    
    /**
     * 平均每日次数
     */
    private Double averageCount;
    
    /**
     * 统计天数
     */
    private Integer days;
    
    /**
     * 类型：urine-尿液, feces-粪便，null-全部
     */
    private String type;
}
