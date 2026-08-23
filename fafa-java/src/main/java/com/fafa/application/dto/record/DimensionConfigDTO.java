package com.fafa.application.dto.record;

import java.util.List;

/**
 * 记录维度配置DTO（物种差异化打卡页渲染用）
 *
 * @author FaFa Team
 * @since 2.0
 */
public record DimensionConfigDTO(
    String dimension,
    String label,
    boolean required,
    String tip,
    List<DimensionOptionDTO> options
) {
}
