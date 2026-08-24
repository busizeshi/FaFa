package com.fafa.application.dto.report;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReportDTO(Long id, Long petId, String periodType, LocalDate periodStart,
                        LocalDate periodEnd, String dataStatus, String status,
                        String snapshotVersion, String calculationVersion, JsonNode factSnapshot,
                        JsonNode content, String contentMode, String modelName,
                        String promptVersion, String generationTrigger, int generationVersion,
                        String lastErrorCode, String lastErrorMessage, LocalDateTime generatedAt,
                        LocalDateTime createdAt, LocalDateTime updatedAt) {
}
