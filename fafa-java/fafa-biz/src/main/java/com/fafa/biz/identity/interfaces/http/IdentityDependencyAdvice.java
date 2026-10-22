package com.fafa.biz.identity.interfaces.http;

import com.fafa.common.api.R;
import com.fafa.common.error.CommonErrorCode;
import com.fafa.biz.identity.application.AuthRateLimitException;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(-10)
@RestControllerAdvice
public class IdentityDependencyAdvice {
    @ExceptionHandler(AuthRateLimitException.class)
    public ResponseEntity<R<Void>> limited(AuthRateLimitException exception) {
        return ResponseEntity.status(429).header("Retry-After", Long.toString(exception.retryAfter()))
                .body(R.error(exception.errorCode().code(), exception.getMessage()));
    }
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<R<Void>> unavailable(DataAccessException exception) {
        var error = CommonErrorCode.DEPENDENCY_UNAVAILABLE;
        return ResponseEntity.status(error.httpStatus()).body(R.error(error.code(), error.message()));
    }
}
