package com.fafa.application.dto.medical;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record MedicationPlanCommand(Long petId, Long sourceVisitId, String medicationName,
                                    String specification, String purpose, BigDecimal dosageValue,
                                    String dosageUnit, String frequencyType, Integer frequencyValue,
                                    List<LocalTime> administrationTimes, String mealRelation,
                                    LocalDate startDate, LocalDate endDate, String notes) {
}
