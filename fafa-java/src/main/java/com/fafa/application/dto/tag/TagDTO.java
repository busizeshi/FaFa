package com.fafa.application.dto.tag;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 标签DTO
 *
 * @author FaFa Team
 * @since 4.0
 */
@Schema(description = "标签信息")
public record TagDTO(
    @Schema(description = "标签ID", example = "1")
    Long id,
    @Schema(description = "标签名称", example = "晒太阳")
    String tagName,
    @Schema(description = "标签分类：pet/location/action/object/mood/other", example = "action")
    String category,
    @Schema(description = "标签使用次数", example = "12")
    int usageCount,
    @Schema(description = "最近使用时间", example = "2026-08-20T15:30:00")
    LocalDateTime lastUsedAt
) {
}
