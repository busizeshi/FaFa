package com.fafa.interfaces.dto.medical;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreateMedicationRecordRequest {
    @NotNull
    private Long petId;
    @NotNull
    private Long medicationPlanId;
    @NotNull
    private LocalDateTime scheduledAt;
    private LocalDateTime takenAt;
    @NotNull
    private String status;
    @NotNull
    private BigDecimal dosageValue;
    @NotNull
    private String dosageUnit;
    private String notes;
}
