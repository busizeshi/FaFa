package com.fafa.application.dto.medical;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MedicationRecordDTO(Long id, Long petId, Long medicationPlanId,
                                  Long reminderId, LocalDateTime scheduledAt,
                                  LocalDateTime takenAt, BigDecimal dosageValue,
                                  String dosageUnit, String status, String notes,
                                  LocalDateTime createdAt) {
}
