package com.fafa.application.dto.content;

public record UpdateContentDraftCommand(Long userId, Long draftId, String draftJson) {
}
