package com.fafa.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * 统一响应结构
 *
 * @param <T> 业务数据类型
 * @author FaFa Team
 * @since 1.0
 */
@Getter
@Schema(description = "统一接口响应结构")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> {

    /** 业务状态码，0 表示成功，非 0 为错误码 */
    @Schema(description = "业务状态码，0 表示成功，非 0 表示失败", example = "0")
    private final int code;
    @Schema(description = "响应消息，通常用于描述本次请求的处理结果", example = "操作成功")
    private final String message;
    @Schema(description = "业务数据；无业务数据时该字段为空，具体结构由接口定义")
    private final T data;

    private Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> ok() {
        return new Result<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data);
    }

    public static <T> Result<T> fail(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static <T> Result<T> fail(ErrorCode errorCode, String message) {
        return new Result<>(errorCode.getCode(), message, null);
    }
}
