package com.fafa.interfaces.dto.medical;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class HealthEvaluateRequest {
    @NotNull
    private Long petId;
    private LocalDate asOfDate;
}
