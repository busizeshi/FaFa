package com.fafa.application.dto.chat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Agent 体重工具响应。 */
public record AiWeightHistoryDTO(Long petId, LocalDate startDate, LocalDate endDate,
                                 List<AiWeightRecordDTO> records) {
    public record AiWeightRecordDTO(Long id, LocalDate recordDate, BigDecimal weight, String notes) {
    }
}
