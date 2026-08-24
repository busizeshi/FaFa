package com.fafa.application.dto.medical;

import java.time.LocalDate;

public record MedicalSummaryCommand(Long petId, LocalDate startDate, LocalDate endDate,
                                    boolean includeMedia) {
}
