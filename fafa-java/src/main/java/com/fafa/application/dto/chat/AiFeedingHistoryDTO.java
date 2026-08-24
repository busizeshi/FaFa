package com.fafa.application.dto.chat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Agent 喂食工具响应。 */
public record AiFeedingHistoryDTO(Long petId, LocalDate startDate, LocalDate endDate,
                                  List<AiFeedingRecordDTO> records, int count) {
    public record AiFeedingRecordDTO(Long id, LocalDateTime feedingTime, String foodType,
                                     BigDecimal amount, String unit, String notes) {
    }
}
