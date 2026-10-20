package com.fafa.common.web;

import com.fafa.common.api.ApiResponse;
import com.fafa.common.error.CommonErrorCode;
import com.fafa.common.error.FafaException;
import com.fafa.common.error.FieldViolation;
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

import java.util.List;

/** Maps expected and unexpected HTTP failures into the shared response envelope. */
@RestControllerAdvice
public class CommonExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(CommonExceptionHandler.class);

    /** Creates the shared MVC exception mapper. */
    public CommonExceptionHandler() {
    }

    /** Converts a known application failure to its declared status and safe message.
     *
     * @param exception expected application failure
     * @return an HTTP response using the shared envelope
     */
    @ExceptionHandler(FafaException.class)
    public ResponseEntity<ApiResponse<Void>> handleFafaException(FafaException exception) {
        var error = exception.errorCode();
        return ResponseEntity.status(error.httpStatus())
                .body(ApiResponse.failure(error.code(), exception.getMessage(), null, currentRequestId()));
    }

    /** Converts request binding and validation failures into field-level details.
     *
     * @param exception request parsing or validation failure
     * @return a bad-request response with safe field messages
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, HandlerMethodValidationException.class,
            ConstraintViolationException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ApiResponse<List<FieldViolation>>> handleInvalidRequest(Exception exception) {
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
        return ResponseEntity.badRequest().body(ApiResponse.failure(
                CommonErrorCode.INVALID_ARGUMENT.code(), CommonErrorCode.INVALID_ARGUMENT.message(),
                violations, currentRequestId()));
    }

    /** Converts an unsupported HTTP method into the shared error envelope.
     *
     * @param exception unsupported method details
     * @return a method-not-allowed response
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException exception) {
        var error = CommonErrorCode.METHOD_NOT_ALLOWED;
        return ResponseEntity.status(error.httpStatus()).body(ApiResponse.failure(
                error.code(), error.message(), null, currentRequestId()));
    }

    /** Converts an unsupported request media type into the shared error envelope.
     *
     * @param exception unsupported media type details
     * @return an unsupported-media-type response
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException exception) {
        var error = CommonErrorCode.UNSUPPORTED_MEDIA_TYPE;
        return ResponseEntity.status(error.httpStatus()).body(ApiResponse.failure(
                error.code(), error.message(), null, currentRequestId()));
    }

    /** Converts an unacceptable response media type into the shared error envelope.
     *
     * @param exception unacceptable media type details
     * @return a not-acceptable response
     */
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotAcceptable(HttpMediaTypeNotAcceptableException exception) {
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(ApiResponse.failure(
                "FAFA-40600", "The requested response format is not available", null, currentRequestId()));
    }

    /** Logs an unexpected failure with its request ID and hides internal details from clients.
     *
     * @param exception unexpected server failure
     * @return a generic internal-server-error response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception exception) {
        String requestId = currentRequestId();
        log.error("Unhandled request failure, requestId={}", requestId, exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.failure(
                CommonErrorCode.INTERNAL_ERROR.code(), CommonErrorCode.INTERNAL_ERROR.message(), null, requestId));
    }

    private static FieldViolation toFieldViolation(FieldError error) {
        return new FieldViolation(error.getField(), error.getDefaultMessage() == null
                ? "Invalid value" : error.getDefaultMessage());
    }

    private static String currentRequestId() {
        String requestId = org.slf4j.MDC.get(RequestIdContext.MDC_KEY);
        return requestId == null ? RequestIdContext.resolve(null) : requestId;
    }
}
