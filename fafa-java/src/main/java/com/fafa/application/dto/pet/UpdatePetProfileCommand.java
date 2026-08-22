package com.fafa.application.dto.pet;

import java.time.LocalDate;

/**
 * 更新宠物资料命令
 *
 * @author FaFa Team
 * @since 1.0
 */
public record UpdatePetProfileCommand(
    Long petId,
    String name,
    String breed,
    String gender,
    LocalDate birthDate,
    Boolean neutered,
    LocalDate adoptDate
) {
}
