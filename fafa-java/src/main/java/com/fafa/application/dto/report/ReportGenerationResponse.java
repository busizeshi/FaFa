package com.fafa.application.dto.report;

public record ReportGenerationResponse(ReportContentDTO content, String mode,
                                       String modelName, String promptVersion) {
}
