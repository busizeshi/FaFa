package com.fafa.interfaces.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 宠物排序请求
 * 
 * @author FaFa Team
 * @since 1.0
 */
@Data
@Schema(description = "宠物排序请求")
public class SortPetsRequest {
    
    @Schema(description = "宠物 ID 列表（按顺序排列）", required = true)
    @NotEmpty(message = "宠物 ID 列表不能为空")
    private List<Long> petIds;
}
