package com.fafa.common.api;

import com.fafa.common.web.RequestIdContext;
import org.slf4j.MDC;

/** Java 服务之间共享的稳定 HTTP 响应包装。
 *
 * @param <T> 数据载荷类型
 * @param code 稳定的响应编码
 * @param message 可安全返回给客户端的响应消息
 * @param data 响应数据，允许为 {@code null}
 * @param requestId 请求关联 ID
 */
public record R<T>(String code, String message, T data, String requestId) {
    private static final String OK_CODE = "0";
    private static final String OK_MESSAGE = "OK";

    /** 创建成功响应，并从当前请求上下文解析请求 ID。
     *
     * @param <T> 数据载荷类型
     * @param data 成功响应的数据载荷
     * @return 成功响应包装
     */
    public static <T> R<T> ok(T data) {
        return ok(data, currentRequestId());
    }

    /** 使用明确的请求 ID 创建成功响应。
     *
     * @param <T> 数据载荷类型
     * @param data 成功响应的数据载荷
     * @param requestId 请求关联 ID
     * @return 成功响应包装
     */
    public static <T> R<T> ok(T data, String requestId) {
        return new R<>(OK_CODE, OK_MESSAGE, data, requestId);
    }

    /** 创建不带数据载荷的成功响应。
     *
     * @param <T> 数据载荷类型
     * @return 成功响应包装
     */
    public static <T> R<T> ok() {
        return ok(null);
    }

    /** 创建错误响应，并从当前请求上下文解析请求 ID。
     *
     * @param <T> 数据载荷类型
     * @param code 稳定的错误编码
     * @param message 可安全返回给客户端的错误消息
     * @return 错误响应包装
     */
    public static <T> R<T> error(String code, String message) {
        return error(code, message, null);
    }

    /** 创建带安全错误详情的错误响应，并从当前请求上下文解析请求 ID。
     *
     * @param <T> 数据载荷类型
     * @param code 稳定的错误编码
     * @param message 可安全返回给客户端的错误消息
     * @param data 可选的安全错误详情
     * @return 错误响应包装
     */
    public static <T> R<T> error(String code, String message, T data) {
        return error(code, message, data, currentRequestId());
    }

    /** 使用明确的请求 ID 创建错误响应。
     *
     * @param <T> 数据载荷类型
     * @param code 稳定的错误编码
     * @param message 可安全返回给客户端的错误消息
     * @param data 可选的安全错误详情
     * @param requestId 请求关联 ID
     * @return 错误响应包装
     */
    public static <T> R<T> error(String code, String message, T data, String requestId) {
        return new R<>(code, message, data, requestId);
    }

    private static String currentRequestId() {
        String requestId = MDC.get(RequestIdContext.MDC_KEY);
        return requestId == null ? RequestIdContext.resolve(null) : requestId;
    }
}
