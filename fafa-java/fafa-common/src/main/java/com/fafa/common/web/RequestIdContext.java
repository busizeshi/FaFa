package com.fafa.common.web;

import java.util.UUID;
import java.util.regex.Pattern;

/** 在请求 ID 进入日志或下游请求头之前生成并校验它。 */
public final class RequestIdContext {
    /** Gateway 与后端服务之间传递请求 ID 使用的请求头。 */
    public static final String HEADER_NAME = "X-Request-ID";
    /** Logback 日志格式使用的 MDC 键。 */
    public static final String MDC_KEY = "requestId";
    private static final Pattern SAFE_REQUEST_ID = Pattern.compile("[A-Za-z0-9._-]{1,80}");

    private RequestIdContext() {
    }

    /** 返回校验后的调用方请求 ID；值缺失或不安全时生成新的 UUID。
     * @param candidate 调用方提供的请求 ID
     * @return 校验后的请求 ID
     */
    public static String resolve(String candidate) {
        if (candidate != null && SAFE_REQUEST_ID.matcher(candidate).matches()) {
            return candidate;
        }
        return UUID.randomUUID().toString();
    }
}
