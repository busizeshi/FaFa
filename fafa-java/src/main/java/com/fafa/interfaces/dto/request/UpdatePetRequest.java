package com.fafa.interfaces.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 更新宠物请求
 * 
 * @author FaFa Team
 * @since 1.0
 */
@Data
@Schema(description = "更新宠物请求")
public class UpdatePetRequest {
    
    @Schema(description = "宠物名称")
    private String name;
    
    @Schema(description = "头像 URL")
    private String avatar;
    
    @Schema(description = "品种")
    private String breed;
    
    @Schema(description = "性别: male-公, female-母, unknown-未知")
    private String gender;
    
    @Schema(description = "出生日期")
    private LocalDate birthDate;
    
    @Schema(description = "到家日期")
    private LocalDate adoptDate;
    
    @Schema(description = "体重(kg)")
    private BigDecimal weight;
    
    @Schema(description = "是否绝育")
    private Boolean isNeutered;
    
    @Schema(description = "毛色")
    private String coatColor;
    
    @Schema(description = "备注")
    private String remarks;
    
    @Schema(description = "正面照片URL")
    private String frontViewUrl;
    
    @Schema(description = "侧面照片URL")
    private String sideViewUrl;
    
    @Schema(description = "顶部照片URL")
    private String topViewUrl;
}
