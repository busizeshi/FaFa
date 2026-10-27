package com.fafa.biz.interfaces;

import com.fafa.common.api.ApiResponse;
import com.fafa.common.api.CommonErrorCode;
import com.fafa.common.constant.HeaderConstants;
import com.fafa.common.exception.BizException;
import cn.dev33.satoken.exception.NotLoginException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.dao.DuplicateKeyException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 全局异常处理器：业务异常 → 稳定错误响应（架构规范 §8.2 / 手册 §6.4）。
 *
 * <p>约定：</p>
 * <ul>
 *   <li>HTTP 状态由稳定错误码前三位推导（401-01-01 → 401）；</li>
 *   <li>对外永远不返回堆栈、SQL、内部主机名；异常栈只进受控日志；</li>
 *   <li>429 响应附带 {@code Retry-After} 头（设计稿 rate-limited 状态帧要求展示服务端重试时间）；</li>
 *   <li>参数校验失败返回 fieldErrors 明细，保留用户输入。</li>
 * </ul>
 *
 * @author FaFa 项目组
 * @version 1.0.0
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 业务异常：BizException 承载的稳定错误码 → 统一失败响应。
     *
     * @param e 业务异常
     * @return 统一错误结构
     */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiResponse<Void>> handleBizException(BizException e) {
        int httpStatus = resolveHttpStatus(e.getErrorCode().getCode());
        boolean retryable = httpStatus >= 500 || httpStatus == HttpStatus.TOO_MANY_REQUESTS.value();
        log.warn("[业务异常] code={} message={} httpStatus={}",
                e.getErrorCode().getCode(), e.getMessage(), httpStatus);
        ApiResponse<Void> body = ApiResponse.fail(e.getErrorCode(), e.getMessage(), currentRequestId(), retryable);
        HttpHeaders headers = new HttpHeaders();
        if (httpStatus == HttpStatus.TOO_MANY_REQUESTS.value()) {
            // 限流：服务端给出建议重试时间（秒），供 rate-limited 状态帧展示
            headers.add(HttpHeaders.RETRY_AFTER, "60");
        }
        return ResponseEntity.status(httpStatus).headers(headers).body(body);
    }

    /**
     * Sa-Token 未登录 / 会话失效：统一返回 401 稳定错误（与网关 401 结构一致）。
     *
     * @param e 未登录异常
     * @return 401 + 统一错误结构
     */
    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotLogin(NotLoginException e) {
        log.info("[会话校验] 未登录或会话已失效 type={}", e.getType());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ApiResponse.fail(CommonErrorCode.UNAUTHORIZED, null, currentRequestId(), false));
    }

    /**
     * Bean Validation 参数校验失败：字段级错误明细 + 保留输入。
     *
     * @param e 校验异常
     * @return 400 + fieldErrors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            // 同字段多条时保留首条，避免信息过载
            fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        log.info("[参数校验失败] fields={}", fieldErrors.keySet());
        ApiResponse<Void> body = ApiResponse.fail(
                CommonErrorCode.BAD_REQUEST, "请求参数错误", currentRequestId(), false);
        body.setFieldErrors(fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * 数据库唯一约束冲突（并发重复注册等）：统一映射到 409。
     *
     * @param e 唯一约束异常
     * @return 409 冲突
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateKey(DuplicateKeyException e) {
        log.warn("[唯一约束冲突] {}", e.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ApiResponse.fail(CommonErrorCode.CONFLICT, null, currentRequestId(), false));
    }

    /**
     * 兜底异常：完整堆栈进日志，对外只返回稳定文案。
     *
     * @param e 未预期异常
     * @return 500 稳定响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
        log.error("[未预期异常]", e);
        return ResponseEntity.internalServerError().body(
                ApiResponse.fail(CommonErrorCode.INTERNAL_ERROR, null, currentRequestId(), true));
    }

    /**
     * 从错误码前三位推导 HTTP 状态（401-01-01 → 401）；推导失败按 500 处理。
     *
     * @param code 稳定错误码
     * @return HTTP 状态值
     */
    private int resolveHttpStatus(String code) {
        try {
            int httpStatus = Integer.parseInt(code.substring(0, 3));
            // 只接受合法的 HTTP 状态段，防止错误码笔误导致异常状态
            if (httpStatus >= 400 && httpStatus <= 599) {
                return httpStatus;
            }
        } catch (NumberFormatException ignored) {
            // fall through
        }
        return HttpStatus.INTERNAL_SERVER_ERROR.value();
    }

    /**
     * @return 当前 MDC 中的 requestId（未经过网关时为 "-"）
     */
    private String currentRequestId() {
        String requestId = MDC.get(RequestIdFilter.MDC_REQUEST_ID);
        return requestId == null ? "-" : requestId;
    }
}
