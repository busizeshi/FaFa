package com.fafa.interfaces.dto.medical;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateMedicalVisitRequest {
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
