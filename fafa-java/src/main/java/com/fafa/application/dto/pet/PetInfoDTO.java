package com.fafa.application.dto.pet;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 宠物信息DTO
 *
 * @author FaFa Team
 * @since 1.0
 */
public record PetInfoDTO(
    Long id,
    Long userId,
    String name,
    String type,
    String breed,
    String gender,
    LocalDate birthDate,
    Boolean neutered,
    LocalDate adoptDate,
    BigDecimal weight,
    String avatarUrl,
    String frontPhotoUrl,
    String sidePhotoUrl,
    Integer sortOrder,
    Boolean hasProfilePhotos
) {
}
