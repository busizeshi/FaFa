package com.fafa.interfaces.dto.medical;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateMedicalSummaryRequest {
    @NotNull
    private Long petId;
    @NotNull
    private LocalDate startDate;
    @NotNull
    private LocalDate endDate;
    private boolean includeMedia;
}
