package com.fafa.domain.model.record;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 体重记录聚合根
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeightRecord {
    
    /**
     * 记录ID
     */
    private RecordId recordId;
    
    /**
     * 宠物ID
     */
    private Long petId;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 记录日期
     */
    private LocalDate recordDate;
    
    /**
     * 体重（kg）
     */
    private BigDecimal weight;
    
    /**
     * 体况评分（1-9）
     */
    private Integer bcsScore;
    
    /**
     * 备注
     */
    private String remarks;
    
    /**
     * 创建体重记录
     */
    public static WeightRecord create(Long petId, Long userId, LocalDate recordDate, BigDecimal weight) {
        return WeightRecord.builder()
                .petId(petId)
                .userId(userId)
                .recordDate(recordDate)
                .weight(weight)
                .build();
    }
    
    /**
     * 更新体重信息
     */
    public void updateWeight(BigDecimal weight, Integer bcsScore, String remarks) {
        this.weight = weight;
        this.bcsScore = bcsScore;
        this.remarks = remarks;
    }
}
