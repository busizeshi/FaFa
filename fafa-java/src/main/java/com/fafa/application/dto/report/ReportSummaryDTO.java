package com.fafa.application.dto.report;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReportSummaryDTO(Long id, Long petId, String periodType, LocalDate periodStart,
                               LocalDate periodEnd, String dataStatus, String status,
                               String contentMode, int generationVersion, LocalDateTime generatedAt) {
}
