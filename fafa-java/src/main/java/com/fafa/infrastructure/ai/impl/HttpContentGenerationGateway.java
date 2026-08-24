package com.fafa.infrastructure.ai.impl;

import com.fafa.application.dto.content.ContentGenerationRequest;
import com.fafa.application.dto.content.ContentGenerationResponse;
import com.fafa.application.port.ContentGenerationGateway;
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
public class HttpContentGenerationGateway implements ContentGenerationGateway {
    private final RestClient restClient;
    public HttpContentGenerationGateway(@Qualifier("contentGenerationRestClient") RestClient restClient) {
        this.restClient = restClient;
    }
    @Override public ContentGenerationResponse generate(ContentGenerationRequest request) {
        try {
            ContentGenerationResponse response = restClient.post().uri("/api/content/generate")
                    .contentType(MediaType.APPLICATION_JSON).header("X-Trace-Id", traceId())
                    .body(request).retrieve().body(ContentGenerationResponse.class);
            if (response == null || response.content() == null) {
                throw new BusinessException(ErrorCode.CONTENT_DRAFT_INVALID, "内容生成结果为空");
            }
            return response;
        } catch (BusinessException ex) { throw ex; }
        catch (Exception ex) {
            log.error("调用内容生成服务失败: draftId={}, petId={}", request.draftId(), request.petId(), ex);
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        }
    }
    private String traceId() {
        String value = MDC.get("traceId");
        return value == null || value.isBlank() ? "no-trace" : value;
    }
}
