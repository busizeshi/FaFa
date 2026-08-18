package com.fafa.domain.model.record;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 排便记录聚合根
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExcretionRecord {
    
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
     * 排便时间
     */
    private LocalDateTime excretionTime;
    
    /**
     * 类型：urine-尿液, feces-粪便
     */
    private String type;
    
    /**
     * 颜色
     */
    private String color;
    
    /**
     * 形状/状态
     */
    private String shape;
    
    /**
     * 异常标记
     */
    private Boolean abnormal;
    
    /**
     * 备注
     */
    private String remarks;
    
    /**
     * 创建排便记录
     */
    public static ExcretionRecord create(Long petId, Long userId, LocalDateTime excretionTime, String type) {
        return ExcretionRecord.builder()
                .petId(petId)
                .userId(userId)
                .excretionTime(excretionTime)
                .type(type)
                .abnormal(false)
                .build();
    }
    
    /**
     * 更新排便信息
     */
    public void updateExcretionInfo(LocalDateTime excretionTime, String type, String color, 
                                    String shape, Boolean abnormal, String remarks) {
        this.excretionTime = excretionTime;
        this.type = type;
        this.color = color;
        this.shape = shape;
        this.abnormal = abnormal;
        this.remarks = remarks;
    }
}
