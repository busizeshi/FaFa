package com.fafa.interfaces.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 喂食记录响应
 * 
 * @author FaFa Team
 * @since 1.0
 */
@Data
@Builder
@Schema(description = "喂食记录响应")
public class FeedRecordResponse {
    
    @Schema(description = "记录ID")
    private Long id;
    
    @Schema(description = "宠物ID")
    private Long petId;
    
    @Schema(description = "喂食时间")
    private LocalDateTime feedTime;
    
    @Schema(description = "食物名称")
    private String foodName;
    
    @Schema(description = "食物类型")
    private String foodType;
    
    @Schema(description = "喂食量")
    private String amount;
    
    @Schema(description = "单位")
    private String unit;
    
    @Schema(description = "品牌")
    private String brand;
    
    @Schema(description = "备注")
    private String remarks;
    
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
