package com.fafa.common;

import lombok.Getter;

/**
 * 业务异常
 *
 * 业务规则违反时抛出，由全局异常处理器统一捕获并转换为 Result 响应。
 *
 * @author FaFa Team
 * @since 1.0
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
