package com.fafa.gateway.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fafa.common.api.R;
import com.fafa.common.error.CommonErrorCode;
import com.fafa.common.error.FafaException;
import com.fafa.common.web.RequestIdContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

/** 响应式 Gateway 错误适配器；从 exchange 读取请求 ID，而不是读取 Servlet MDC。 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GatewayExceptionHandler implements WebExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GatewayExceptionHandler.class);

    private final ObjectMapper objectMapper;

    public GatewayExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable exception) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(exception);
        }

        String requestId = RequestIdContext.resolve(
                exchange.getRequest().getHeaders().getFirst(RequestIdContext.HEADER_NAME));
        HttpStatusCode status = HttpStatus.INTERNAL_SERVER_ERROR;
        String code = CommonErrorCode.INTERNAL_ERROR.code();
        String message = CommonErrorCode.INTERNAL_ERROR.message();

        if (exception instanceof FafaException fafaException) {
            status = fafaException.errorCode().httpStatus();
            code = fafaException.errorCode().code();
            message = fafaException.getMessage();
        } else if (exception instanceof ResponseStatusException responseStatusException) {
            status = responseStatusException.getStatusCode();
            switch (status.value()) {
                case 400 -> {
                    code = CommonErrorCode.INVALID_ARGUMENT.code();
                    message = CommonErrorCode.INVALID_ARGUMENT.message();
                }
                case 401 -> {
                    code = CommonErrorCode.UNAUTHENTICATED.code();
                    message = CommonErrorCode.UNAUTHENTICATED.message();
                }
                case 403 -> {
                    code = CommonErrorCode.FORBIDDEN.code();
                    message = CommonErrorCode.FORBIDDEN.message();
                }
                case 404 -> {
                    code = CommonErrorCode.NOT_FOUND.code();
                    message = CommonErrorCode.NOT_FOUND.message();
                }
                case 405 -> {
                    code = CommonErrorCode.METHOD_NOT_ALLOWED.code();
                    message = CommonErrorCode.METHOD_NOT_ALLOWED.message();
                }
                case 409 -> {
                    code = CommonErrorCode.CONFLICT.code();
                    message = CommonErrorCode.CONFLICT.message();
                }
                case 415 -> {
                    code = CommonErrorCode.UNSUPPORTED_MEDIA_TYPE.code();
                    message = CommonErrorCode.UNSUPPORTED_MEDIA_TYPE.message();
                }
                case 413 -> {
                    code = CommonErrorCode.REQUEST_TOO_LARGE.code();
                    message = CommonErrorCode.REQUEST_TOO_LARGE.message();
                }
                case 503 -> {
                    code = CommonErrorCode.DEPENDENCY_UNAVAILABLE.code();
                    message = CommonErrorCode.DEPENDENCY_UNAVAILABLE.message();
                }
            }
        }

        if (status.is5xxServerError()) {
            log.error("Unhandled Gateway failure, requestId={}", requestId, exception);
        }

        byte[] body;
        try {
            body = objectMapper.writeValueAsBytes(R.error(code, message, null, requestId));
        } catch (JsonProcessingException serializationException) {
            return Mono.error(serializationException);
        }
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().getHeaders().set(RequestIdContext.HEADER_NAME, requestId);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
    }
}
