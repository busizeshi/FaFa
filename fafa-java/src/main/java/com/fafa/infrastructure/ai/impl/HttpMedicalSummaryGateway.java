package com.fafa.infrastructure.ai.impl;

import com.fafa.application.dto.medical.MedicalSummaryAgentRequest;
import com.fafa.application.dto.medical.MedicalSummaryDTO;
import com.fafa.application.port.MedicalSummaryGateway;
import com.fafa.common.BusinessException;
import com.fafa.common.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Java 到 fafa-python 医疗摘要接口的 HTTP 适配器。 */
@Slf4j
@Component
public class HttpMedicalSummaryGateway implements MedicalSummaryGateway {

    private final RestClient restClient;

    public HttpMedicalSummaryGateway(@Qualifier("medicalSummaryRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public MedicalSummaryDTO generate(MedicalSummaryAgentRequest request) {
        try {
            MedicalSummaryDTO response = restClient.post().uri("/api/medical/summaries")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Trace-Id", currentTraceId())
                    .body(request).retrieve().body(MedicalSummaryDTO.class);
            if (response == null || response.overview() == null) {
                throw new BusinessException(ErrorCode.HEALTH_SUMMARY_INVALID, "医疗摘要返回为空");
            }
            return response;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("调用医疗摘要服务失败: petId={}, startDate={}, endDate={}",
                    request.petId(), request.startDate(), request.endDate(), ex);
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        }
    }

    private String currentTraceId() {
        String traceId = MDC.get("traceId");
        return traceId == null || traceId.isBlank() ? "no-trace" : traceId;
    }
}
