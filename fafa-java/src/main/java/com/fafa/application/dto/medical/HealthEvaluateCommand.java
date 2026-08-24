package com.fafa.application.dto.medical;

import java.time.LocalDate;

public record HealthEvaluateCommand(Long petId, LocalDate asOfDate) {
}
