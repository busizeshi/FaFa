package com.fafa.application.dto.medical;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record MedicationPlanDTO(Long id, Long petId, Long sourceVisitId, String medicationName,
                                String specification, String purpose, BigDecimal dosageValue,
                                String dosageUnit, String frequencyType, Integer frequencyValue,
                                List<LocalTime> administrationTimes, String mealRelation,
                                LocalDate startDate, LocalDate endDate, String status,
                                List<Long> reminderIds, LocalDateTime createdAt,
                                LocalDateTime updatedAt) {
}
