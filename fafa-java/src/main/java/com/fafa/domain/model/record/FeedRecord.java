package com.fafa.domain.model.record;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 喂食记录聚合根
 * 
 * @author FaFa Team
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedRecord {
    
    /**
     * 记录 ID
     */
    private RecordId recordId;
    
    /**
     * 宠物 ID
     */
    private Long petId;
    
    /**
     * 用户 ID
     */
    private Long userId;
    
    /**
     * 喂食时间
     */
    private LocalDateTime feedTime;
    
    /**
     * 食物名称
     */
    private String foodName;
    
    /**
     * 食物类型：main-主粮, snack-零食, wet-罐头, other-其他
     */
    private String foodType;
    
    /**
     * 喂食量（如：35g, 1罐）
     */
    private String amount;
    
    /**
     * 单位：g-克, ml-毫升, can-罐, piece-个
     */
    private String unit;
    
    /**
     * 品牌
     */
    private String brand;
    
    /**
     * 备注
     */
    private String remarks;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 创建喂食记录
     */
    public static FeedRecord create(Long petId, Long userId, LocalDateTime feedTime, String foodName) {
        return FeedRecord.builder()
                .petId(petId)
                .userId(userId)
                .feedTime(feedTime)
                .foodName(foodName)
                .build();
    }
}
