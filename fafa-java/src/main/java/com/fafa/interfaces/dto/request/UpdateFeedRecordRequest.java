package com.fafa.interfaces.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 更新喂食记录请求
 * 
 * @author FaFa Team
 * @since 1.0
 */
@Data
@Schema(description = "更新喂食记录请求")
public class UpdateFeedRecordRequest {
    
    @Schema(description = "喂食时间")
    private LocalDateTime feedTime;
    
    @Schema(description = "食物名称")
    private String foodName;
    
    @Schema(description = "食物类型: main-主粮, snack-零食, wet-罐头, other-其他")
    private String foodType;
    
    @Schema(description = "喂食量")
    private String amount;
    
    @Schema(description = "单位")
    private String unit;
    
    @Schema(description = "品牌")
    private String brand;
    
    @Schema(description = "备注")
    private String remarks;
}
