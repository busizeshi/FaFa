package com.fafa.common.web;

import com.fafa.common.api.R;
import com.fafa.common.error.CommonErrorCode;
import com.fafa.common.error.FafaException;
import com.fafa.common.error.FieldViolation;
import com.fafa.common.error.ErrorDetails;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/** 将预期和未预期的 HTTP 错误统一映射为共享响应包装。 */
@RestControllerAdvice
public class CommonExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(CommonExceptionHandler.class);

    /** 创建共享 MVC 异常处理器。 */
    public CommonExceptionHandler() {
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<R<Void>> handleNotFound(NoResourceFoundException exception) {
        var error = CommonErrorCode.NOT_FOUND;
        return ResponseEntity.status(error.httpStatus()).body(R.error(error.code(), error.message()));
    }

    /** 将已知应用错误转换为声明的状态码和安全消息。
     *
     * @param exception 预期的应用错误
     * @return 使用共享响应包装的 HTTP 响应
     */
    @ExceptionHandler(FafaException.class)
    public ResponseEntity<R<Void>> handleFafaException(FafaException exception) {
        var error = exception.errorCode();
        return ResponseEntity.status(error.httpStatus())
                .body(R.error(error.code(), exception.getMessage()));
    }

    /** 将请求绑定和校验失败转换为字段级错误详情。
     *
     * @param exception 请求解析或校验失败
     * @return 包含安全字段消息的错误请求响应
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, HandlerMethodValidationException.class,
            ConstraintViolationException.class, HttpMessageNotReadableException.class,
            org.springframework.web.bind.MissingRequestHeaderException.class,
            org.springframework.web.bind.MissingServletRequestParameterException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    public ResponseEntity<R<ErrorDetails>> handleInvalidRequest(Exception exception) {
        List<FieldViolation> violations;
        if (exception instanceof MethodArgumentNotValidException validationException) {
            violations = validationException.getBindingResult().getFieldErrors().stream()
                    .map(CommonExceptionHandler::toFieldViolation)
                    .toList();
        } else if (exception instanceof ConstraintViolationException validationException) {
            violations = validationException.getConstraintViolations().stream()
                    .map(violation -> new FieldViolation(violation.getPropertyPath().toString(), violation.getMessage()))
                    .toList();
        } else {
            violations = List.of(new FieldViolation("request", "Request body or parameters are invalid"));
        }
        return ResponseEntity.badRequest().body(R.error(
                CommonErrorCode.INVALID_ARGUMENT.code(), CommonErrorCode.INVALID_ARGUMENT.message(),
                new ErrorDetails(violations)));
    }

    /** 将不支持的 HTTP 方法转换为共享错误响应。
     *
     * @param exception 不支持的方法详情
     * @return 方法不允许响应
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<R<Void>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException exception) {
        var error = CommonErrorCode.METHOD_NOT_ALLOWED;
        return ResponseEntity.status(error.httpStatus()).body(R.error(error.code(), error.message()));
    }

    /** 将不支持的请求媒体类型转换为共享错误响应。
     *
     * @param exception 不支持的媒体类型详情
     * @return 不支持媒体类型响应
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<R<Void>> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException exception) {
        var error = CommonErrorCode.UNSUPPORTED_MEDIA_TYPE;
        return ResponseEntity.status(error.httpStatus()).body(R.error(error.code(), error.message()));
    }

    /** 将不可接受的响应媒体类型转换为共享错误响应。
     *
     * @param exception 不可接受的媒体类型详情
     * @return 不可接受响应
     */
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<R<Void>> handleNotAcceptable(HttpMediaTypeNotAcceptableException exception) {
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(R.error(
                "FAFA-40600", "The requested response format is not available"));
    }

    /** 记录包含请求 ID 的未预期错误，并对客户端隐藏内部详情。
     *
     * @param exception 未预期的服务端错误
     * @return 通用内部服务错误响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<Void>> handleUnexpected(Exception exception) {
        R<Void> body = R.error(CommonErrorCode.INTERNAL_ERROR.code(), CommonErrorCode.INTERNAL_ERROR.message());
        log.error("Unhandled request failure, requestId={}", body.requestId(), exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private static FieldViolation toFieldViolation(FieldError error) {
        return new FieldViolation(error.getField(), error.getDefaultMessage() == null
                ? "Invalid value" : error.getDefaultMessage());
    }
}
