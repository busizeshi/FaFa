package com.fafa.interfaces.dto.medical;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateMedicalVisitRequest {
    @NotNull
    private Long petId;
    @NotNull
    private LocalDate visitDate;
    @NotBlank
    @Size(max = 200)
    private String hospitalName;
    @Size(max = 100)
    private String doctorName;
    @Size(max = 1000)
    private String chiefComplaint;
    @Size(max = 2000)
    private String diagnosis;
    @Size(max = 3000)
    private String prescription;
    @Size(max = 2000)
    private String treatment;
    private BigDecimal totalCost;
    @Size(max = 2000)
    private String notes;
}
