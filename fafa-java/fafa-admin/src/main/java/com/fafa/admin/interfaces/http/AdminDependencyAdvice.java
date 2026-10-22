package com.fafa.admin.interfaces.http;

import com.fafa.admin.security.AdminPolicy;
import com.fafa.common.api.R;
import com.fafa.common.error.*;
import org.apache.dubbo.rpc.RpcException;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Order(-20)
@RestControllerAdvice
@org.springframework.context.annotation.Profile("!admin-init")
public class AdminDependencyAdvice {
    private final AdminPolicy policy;
    public AdminDependencyAdvice(AdminPolicy policy) { this.policy = policy; }
    @ExceptionHandler({DataAccessException.class, RpcException.class})
    public ResponseEntity<R<Void>> dependency(Exception exception) {
        var error = CommonErrorCode.DEPENDENCY_UNAVAILABLE;
        return ResponseEntity.status(503).body(R.error(error.code(), error.message()));
    }
    @ExceptionHandler(FafaException.class)
    public ResponseEntity<R<Void>> business(FafaException exception) {
        var response = ResponseEntity.status(exception.errorCode().httpStatus());
        if (exception.errorCode() == BusinessError.ADMIN_LOGIN_LIMITED) response.header("Retry-After", Integer.toString(policy.loginWindowSeconds()));
        return response.body(R.error(exception.errorCode().code(), exception.getMessage()));
    }
}
