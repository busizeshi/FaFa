package com.fafa.interfaces.common;

import com.fafa.domain.common.ErrorCode;
import lombok.Getter;

/**
 * 统一响应包装
 *
 * 所有对外接口返回此结构：{ code, message, data }
 */
@Getter
public class Result<T> {

    /** 业务状态码，0 表示成功 */
    private final int code;
    private final String message;
    private final T data;

    private Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> ok() {
        return new Result<>(0, "success", null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(0, "success", data);
    }

    public static <T> Result<T> fail(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }
}
