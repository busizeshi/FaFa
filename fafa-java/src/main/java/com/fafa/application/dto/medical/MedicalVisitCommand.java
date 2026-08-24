package com.fafa.application.dto.medical;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MedicalVisitCommand(Long petId, LocalDate visitDate, String hospitalName,
                                  String doctorName, String chiefComplaint, String diagnosis,
                                  String prescription, String treatment, BigDecimal totalCost,
                                  String notes) {
}
