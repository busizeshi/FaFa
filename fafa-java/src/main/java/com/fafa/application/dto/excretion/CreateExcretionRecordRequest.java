package com.fafa.application.dto.excretion;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 创建排便记录请求
 */
@Data
public class CreateExcretionRecordRequest {
    
    @NotNull(message = "宠物ID不能为空")
    private Long petId;
    
    @NotNull(message = "排便时间不能为空")
    private LocalDateTime excretionTime;
    
    @NotBlank(message = "类型不能为空")
    private String type;
    
    private String color;
    
    private String shape;
    
    private Boolean abnormal;
    
    private String remarks;
}
