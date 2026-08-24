package com.fafa.application.dto.medical;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record HealthAlertDTO(Long id, Long petId, String ruleCode, String severity,
                             String title, String message, String status,
                             LocalDate observedFrom, LocalDate observedTo,
                             List<EvidenceDTO> evidence, LocalDateTime lastDetectedAt) {

    public record EvidenceDTO(String sourceType, String sourceId, String field,
                              String value, String observedDate) {
    }
}
