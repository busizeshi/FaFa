package com.fafa.application.dto.water;

import lombok.Data;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 创建饮水记录请求
 */
@Data
public class CreateWaterRecordRequest {
    
    @NotNull(message = "宠物ID不能为空")
    private Long petId;
    
    @NotNull(message = "饮水时间不能为空")
    private LocalDateTime waterTime;
    
    @NotNull(message = "饮水量不能为空")
    @Min(value = 1, message = "饮水量必须大于0")
    private Integer amount;
    
    private String remarks;
}
