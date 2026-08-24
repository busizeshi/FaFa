package com.fafa.infrastructure.ai.impl;

import com.fafa.application.dto.report.ReportGenerationRequest;
import com.fafa.application.dto.report.ReportGenerationResponse;
import com.fafa.application.port.ReportGenerationGateway;
import com.fafa.common.BusinessException;
import com.fafa.common.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class HttpReportGenerationGateway implements ReportGenerationGateway {
    private final RestClient restClient;

    public HttpReportGenerationGateway(@Qualifier("reportGenerationRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public ReportGenerationResponse generate(ReportGenerationRequest request) {
        try {
            ReportGenerationResponse response = restClient.post().uri("/api/reports/generate")
                    .contentType(MediaType.APPLICATION_JSON).header("X-Trace-Id", traceId())
                    .body(request).retrieve().body(ReportGenerationResponse.class);
            if (response == null || response.content() == null) {
                throw new BusinessException(ErrorCode.REPORT_CONTENT_INVALID, "报告生成结果为空");
            }
            return response;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("调用报告生成服务失败: reportId={}, petId={}", request.reportId(), request.petId(), ex);
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        }
    }

    private String traceId() {
        String value = MDC.get("traceId");
        return value == null || value.isBlank() ? "no-trace" : value;
    }
}
