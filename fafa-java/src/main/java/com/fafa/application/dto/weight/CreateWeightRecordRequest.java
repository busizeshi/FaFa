package com.fafa.application.dto.weight;

import lombok.Data;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 创建体重记录请求
 */
@Data
public class CreateWeightRecordRequest {
    
    @NotNull(message = "宠物ID不能为空")
    private Long petId;
    
    @NotNull(message = "记录日期不能为空")
    private LocalDate recordDate;
    
    @NotNull(message = "体重不能为空")
    @DecimalMin(value = "0.01", message = "体重必须大于0")
    private BigDecimal weight;
    
    @Min(value = 1, message = "体况评分范围为1-9")
    @Max(value = 9, message = "体况评分范围为1-9")
    private Integer bcsScore;
    
    private String remarks;
}
