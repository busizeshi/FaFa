package com.fafa.interfaces.web;

import cn.dev33.satoken.exception.NotLoginException;
import com.fafa.common.BusinessException;
import com.fafa.common.ErrorCode;
import com.fafa.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理器
 *
 * 控制器不写 try-catch，所有异常在此统一转换为 Result 响应。
 *
 * @author FaFa Team
 * @since 1.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常：预期内的规则违反，WARN 级别即可 */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException ex) {
        log.warn("业务异常: code={}, message={}", ex.getErrorCode().getCode(), ex.getMessage());
        return Result.fail(ex.getErrorCode(), ex.getMessage());
    }

    /** 未登录 / 会话过期 */
    @ExceptionHandler(NotLoginException.class)
    public Result<Void> handleNotLoginException(NotLoginException ex) {
        log.warn("未登录访问: {}", ex.getMessage());
        return Result.fail(ErrorCode.UNAUTHORIZED);
    }

    /** 请求体参数校验失败 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse(ErrorCode.PARAM_INVALID.getMessage());
        log.warn("参数校验失败: {}", message);
        return Result.fail(ErrorCode.PARAM_INVALID, message);
    }

    /** 表单绑定失败 */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException ex) {
        String message = ex.getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse(ErrorCode.PARAM_INVALID.getMessage());
        log.warn("参数绑定失败: {}", message);
        return Result.fail(ErrorCode.PARAM_INVALID, message);
    }

    /** 请求体缺失或 JSON 格式错误 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("请求体不可读: {}", ex.getMessage());
        return Result.fail(ErrorCode.PARAM_INVALID, "请求体格式错误");
    }

    /** 静态资源 404（Spring Boot 3 需单独处理，避免走系统异常） */
    @ExceptionHandler(NoResourceFoundException.class)
    public Result<Void> handleNoResourceFound(NoResourceFoundException ex) {
        return Result.fail(ErrorCode.NOT_FOUND);
    }

    /** 兜底系统异常：ERROR 级别记录堆栈 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleUnexpectedException(Exception ex) {
        log.error("系统异常", ex);
        return Result.fail(ErrorCode.SYSTEM_ERROR);
    }
}
