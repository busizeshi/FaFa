package com.fafa.infrastructure.ai.impl;

import com.fafa.application.dto.chat.ChatAgentRequest;
import com.fafa.application.dto.chat.ChatAgentResponse;
import com.fafa.application.port.ChatAgentGateway;
import com.fafa.common.BusinessException;
import com.fafa.common.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Java 到 fafa-python Agent 的同步 HTTP 适配器。 */
@Slf4j
@Component
public class HttpChatAgentGateway implements ChatAgentGateway {

    private final RestClient chatAgentRestClient;

    public HttpChatAgentGateway(@Qualifier("chatAgentRestClient") RestClient chatAgentRestClient) {
        this.chatAgentRestClient = chatAgentRestClient;
    }

    @Override
    public ChatAgentResponse chat(ChatAgentRequest request) {
        try {
            ChatAgentResponse response = chatAgentRestClient.post()
                    .uri("/api/chat/send")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Trace-Id", currentTraceId())
                    .body(request)
                    .retrieve()
                    .body(ChatAgentResponse.class);
            if (response == null || response.answer() == null || response.answer().isBlank()) {
                throw new BusinessException(ErrorCode.AI_CHAT_TOOL_FAILED, "AI 返回内容为空");
            }
            return response;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("调用 AI Agent 失败: sessionId={}, messageId={}",
                    request.sessionId(), request.messageId(), ex);
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        }
    }

    private String currentTraceId() {
        String traceId = MDC.get("traceId");
        return traceId == null || traceId.isBlank() ? "no-trace" : traceId;
    }
}
