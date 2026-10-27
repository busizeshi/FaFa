package com.fafa.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * 统一响应包装结构（架构规范 §9.1：成功含 data/meta，失败含稳定 code/message/requestId/retryable）。
 *
 * <p>使用约定：</p>
 * <ul>
 *   <li>成功：{@code ApiResponse.ok(data)}；</li>
 *   <li>失败：抛出 {@link com.fafa.common.exception.BizException}，由各服务的全局异常处理器
 *       统一转换为 {@code ApiResponse.fail(...)}，禁止在业务代码中手工组装失败响应；</li>
 *   <li>本类禁止携带堆栈、SQL、内部主机名等实现细节。</li>
 * </ul>
 *
 * @param <T> 业务数据类型；失败时为 {@code null}
 * @author FaFa 项目组
 * @version 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    /** 稳定业务错误码：成功为 "0"，失败见 {@link ErrorCode} */
    private String code;

    /** 对外提示信息（中文，稳定，不暴露内部细节） */
    private String message;

    /** 业务数据载荷；失败时为 null */
    private T data;

    /** 请求关联 ID：网关生成并透传，用于全链路日志检索 */
    private String requestId;

    /** 该错误是否可重试；成功响应时为 null 不输出 */
    private Boolean retryable;

    /** 字段级错误明细：字段名 → 用户可读原因（契约 §9.1：失败响应含 fieldErrors） */
    private Map<String, String> fieldErrors;

    /** 仅供反序列化框架使用，业务代码请使用静态工厂方法 */
    public ApiResponse() {
    }

    /**
     * @param code      错误码
     * @param message   提示信息
     * @param data      业务数据
     * @param requestId 请求关联 ID
     * @param retryable 是否可重试
     */
    private ApiResponse(String code, String message, T data, String requestId, Boolean retryable) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.requestId = requestId;
        this.retryable = retryable;
    }

    /**
     * 构造成功响应。
     *
     * @param data 业务数据（可为 null，表示"成功但无载荷"）
     * @param <T>  数据类型
     * @return code=0 的成功响应
     */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(CommonErrorCode.OK.getCode(), CommonErrorCode.OK.getMessage(), data, null, null);
    }

    /**
     * 构造失败响应（通常由全局异常处理器调用）。
     *
     * @param errorCode 错误码枚举
     * @param message   覆盖默认的提示文案；传 null 则使用 errorCode 默认文案
     * @param requestId 请求关联 ID
     * @param retryable 是否建议调用方重试
     * @param <T>       数据类型（恒为 null）
     * @return 失败响应
     */
    public static <T> ApiResponse<T> fail(ErrorCode errorCode, String message, String requestId, boolean retryable) {
        return new ApiResponse<>(errorCode.getCode(),
                message != null ? message : errorCode.getMessage(), null, requestId, retryable);
    }

    // ==================== Getter / Setter ====================

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Boolean getRetryable() {
        return retryable;
    }

    public void setRetryable(Boolean retryable) {
        this.retryable = retryable;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }

    public void setFieldErrors(Map<String, String> fieldErrors) {
        this.fieldErrors = fieldErrors;
    }
}
