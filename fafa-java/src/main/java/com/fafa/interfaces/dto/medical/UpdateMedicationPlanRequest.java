package com.fafa.interfaces.dto.medical;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class UpdateMedicationPlanRequest {
    @NotNull
    private Long petId;
    @NotBlank
    @Size(max = 200)
    private String medicationName;
    @Size(max = 100)
    private String specification;
    @Size(max = 500)
    private String purpose;
    @NotNull
    private BigDecimal dosageValue;
    @NotBlank
    private String dosageUnit;
    @NotBlank
    private String frequencyType;
    private Integer frequencyValue;
    @NotNull
    @Size(max = 6)
    private List<LocalTime> administrationTimes;
    @NotBlank
    private String mealRelation;
    @NotNull
    private LocalDate startDate;
    private LocalDate endDate;
    @Size(max = 1000)
    private String notes;
}
