package com.fafa.application.dto.pet;

import java.time.LocalDate;

/**
 * 注册宠物命令
 *
 * @author FaFa Team
 * @since 1.0
 */
public record RegisterPetCommand(
    String name,
    String type,
    String breed,
    String gender,
    LocalDate birthDate,
    Boolean neutered,
    LocalDate adoptDate
) {
}
