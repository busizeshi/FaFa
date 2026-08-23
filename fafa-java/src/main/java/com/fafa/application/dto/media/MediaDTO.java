package com.fafa.application.dto.media;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 素材DTO
 *
 * @author FaFa Team
 * @since 4.0
 */
public record MediaDTO(
    Long id,
    Long petId,
    String petName,
    String mediaType,
    String fileUrl,
    String fileName,
    Long fileSize,
    String mimeType,
    List<String> tags,
    String status,
    String aiDescription,
    List<String> aiTags,
    BigDecimal recognitionScore,
    boolean recognitionConfirmed,
    LocalDateTime takenAt,
    String errorMessage,
    LocalDateTime createdAt
) {
}
