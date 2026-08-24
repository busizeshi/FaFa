package com.fafa.application.dto.report;

import java.time.LocalDate;

public record ReportGenerationRequest(Long reportId, Long petId, String periodType,
                                     LocalDate startDate, LocalDate endDate,
                                     String factSnapshotJson, String promptVersion) {
}
