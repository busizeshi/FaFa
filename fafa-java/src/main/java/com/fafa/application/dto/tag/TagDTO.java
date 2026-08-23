package com.fafa.application.dto.tag;

import java.time.LocalDateTime;

/**
 * 标签DTO
 *
 * @author FaFa Team
 * @since 4.0
 */
public record TagDTO(
    Long id,
    String tagName,
    String category,
    int usageCount,
    LocalDateTime lastUsedAt
) {
}
