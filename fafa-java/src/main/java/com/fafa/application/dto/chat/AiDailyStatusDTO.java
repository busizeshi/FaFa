package com.fafa.application.dto.chat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Agent 每日状态工具响应。 */
public record AiDailyStatusDTO(Long petId, LocalDate startDate, LocalDate endDate,
                               List<AiDailyStatusRecordDTO> records, int recordedDays,
                               int missingDays, int refusedDays) {

    public record AiDailyStatusRecordDTO(LocalDate recordDate, String appetite, String energy,
                                         String waterIntake, String defecation, String vomiting,
                                         String urination, BigDecimal appetiteAmount,
                                         BigDecimal waterAmount, String notes) {
    }
}
