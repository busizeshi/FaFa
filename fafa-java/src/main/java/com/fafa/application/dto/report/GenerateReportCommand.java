package com.fafa.application.dto.report;

import java.time.LocalDate;

public record GenerateReportCommand(Long userId, Long petId, String periodType,
                                    LocalDate anchorDate, boolean force, String trigger) {
}
