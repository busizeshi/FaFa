package com.fafa.application.dto.medical;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MedicationRecordCommand(Long petId, Long medicationPlanId,
                                      LocalDateTime scheduledAt, LocalDateTime takenAt,
                                      String status, BigDecimal dosageValue, String dosageUnit,
                                      String notes) {
}
