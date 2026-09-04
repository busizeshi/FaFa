package com.fafa.application.dto.record;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 记录维度选项DTO（维度的可选档位）
 *
 * @author FaFa Team
 * @since 2.0
 */
@Schema(description = "每日状态维度选项")
public record DimensionOptionDTO(
    @Schema(description = "选项编码", example = "normal") String code,
    @Schema(description = "选项名称", example = "正常") String label
) {
}
