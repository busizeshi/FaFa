package com.fafa.interfaces.dto.pet;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 宠物排序请求
 *
 * @author FaFa Team
 * @since 1.0
 */
@Data
@Schema(description = "宠物排序请求")
public class ReorderPetsRequest {

    @NotEmpty(message = "排序列表不能为空")
    @Schema(description = "宠物ID列表（按新的排序顺序）", example = "[123, 456, 789]")
    private List<Long> petIds;
}
