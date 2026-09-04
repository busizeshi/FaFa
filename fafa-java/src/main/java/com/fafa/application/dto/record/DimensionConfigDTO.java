package com.fafa.application.dto.record;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 记录维度配置DTO（物种差异化打卡页渲染用）
 *
 * @author FaFa Team
 * @since 2.0
 */
@Schema(description = "每日状态记录维度配置")
public record DimensionConfigDTO(
    @Schema(description = "维度编码", example = "appetite") String dimension,
    @Schema(description = "维度名称", example = "食欲") String label,
    @Schema(description = "该维度是否必填", example = "false") boolean required,
    @Schema(description = "填写提示") String tip,
    @Schema(description = "该维度可选项列表") List<DimensionOptionDTO> options
) {
}
