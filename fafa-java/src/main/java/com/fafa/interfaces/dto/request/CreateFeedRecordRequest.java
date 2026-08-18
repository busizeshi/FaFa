package com.fafa.interfaces.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创建喂食记录请求
 * 
 * @author FaFa Team
 * @since 1.0
 */
@Data
@Schema(description = "创建喂食记录请求")
public class CreateFeedRecordRequest {
    
    @Schema(description = "宠物ID", required = true)
    @NotNull(message = "宠物ID不能为空")
    private Long petId;
    
    @Schema(description = "喂食时间（不填默认当前时间）")
    private LocalDateTime feedTime;
    
    @Schema(description = "食物名称", required = true)
    @NotBlank(message = "食物名称不能为空")
    private String foodName;
    
    @Schema(description = "食物类型: main-主粮, snack-零食, wet-罐头, other-其他")
    private String foodType;
    
    @Schema(description = "喂食量（如：35g, 1罐）")
    private String amount;
    
    @Schema(description = "单位: g-克, ml-毫升, can-罐, piece-个")
    private String unit;
    
    @Schema(description = "品牌")
    private String brand;
    
    @Schema(description = "备注")
    private String remarks;
}
