package com.fafa.domain.model.record;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 饮水记录聚合根
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaterRecord {
    
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
     * 饮水时间
     */
    private LocalDateTime waterTime;
    
    /**
     * 饮水量（ml）
     */
    private Integer amount;
    
    /**
     * 备注
     */
    private String remarks;
    
    /**
     * 创建饮水记录
     */
    public static WaterRecord create(Long petId, Long userId, LocalDateTime waterTime, Integer amount) {
        return WaterRecord.builder()
                .petId(petId)
                .userId(userId)
                .waterTime(waterTime)
                .amount(amount)
                .build();
    }
    
    /**
     * 更新饮水信息
     */
    public void updateWaterInfo(LocalDateTime waterTime, Integer amount, String remarks) {
        this.waterTime = waterTime;
        this.amount = amount;
        this.remarks = remarks;
    }
}
