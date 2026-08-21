package com.fafa.domain.exception;

import com.fafa.domain.common.ErrorCode;
import lombok.Getter;

/**
 * 业务异常
 *
 * 业务规则不满足时抛出，由全局异常处理器统一转换为 Result 响应。
 * 领域层/应用层均可抛出，不依赖任何框架。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, String detail) {
        super(errorCode.getMessage() + ": " + detail);
        this.code = errorCode.getCode();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
