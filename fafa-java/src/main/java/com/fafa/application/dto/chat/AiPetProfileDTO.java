package com.fafa.application.dto.chat;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Agent 宠物资料工具响应。 */
public record AiPetProfileDTO(Long petId, String name, String type, String breed, String gender,
                              LocalDate birthDate, BigDecimal weight, LocalDate weightRecordDate) {
}
