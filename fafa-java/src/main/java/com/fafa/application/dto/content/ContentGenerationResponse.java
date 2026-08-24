package com.fafa.application.dto.content;

public record ContentGenerationResponse(ContentDraftContentDTO content, String mode,
                                        String modelName, String promptVersion) {
}
