package com.fafa.application.dto.content;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ContentDraftDTO(Long id, Long petId, String contentType, String topicType,
                              String customTopic, String periodType, LocalDate periodStart,
                              LocalDate periodEnd, JsonNode selectedMedia, JsonNode factSnapshot,
                              JsonNode draft, String status, String contentMode,
                              String generationTrigger, String idempotencyKey, int generationVersion,
                              String modelName, String promptVersion, String lastErrorCode,
                              String lastErrorMessage, LocalDateTime createdAt, LocalDateTime updatedAt) {
}
