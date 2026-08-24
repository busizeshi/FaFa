package com.fafa.application.port;

import com.fafa.application.dto.report.ReportGenerationRequest;
import com.fafa.application.dto.report.ReportGenerationResponse;

public interface ReportGenerationGateway {
    ReportGenerationResponse generate(ReportGenerationRequest request);
}
