package com.fafa.application.dto.content;

public record ContentGenerationRequest(Long draftId, Long petId, String contentType,
                                       String topicType, String customTopic,
                                       String factSnapshotJson, String selectedMediaJson,
                                       String promptVersion) {
}
