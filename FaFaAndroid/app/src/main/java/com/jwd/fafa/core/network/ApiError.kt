package com.jwd.fafa.core.network

/** 客户端统一的错误模型：UI 只消费 ApiError，不接触 HTTP 细节。 */
sealed class ApiError(
    message: String,
    val requestId: String? = null,
    val retryable: Boolean = false,
) : Exception(message) {

    /**
     * 服务端业务错误（统一响应结构 code != "0"，或 HTTP 非 2xx）。
     * @param retryAfterSeconds 429 限流时来自 Retry-After 响应头（秒），用于客户端倒计时。
     */
    class Biz(
        val code: String,
        message: String,
        val httpStatus: Int,
        val retryAfterSeconds: Long? = null,
        val fieldErrors: Map<String, String> = emptyMap(),
        requestId: String? = null,
        retryable: Boolean = false,
    ) : ApiError(message, requestId, retryable) {
        /** 非空用户可读文案（Exception.message 在 Kotlin 中是 String?）。 */
        val displayMessage: String = message
    }

    /** 网络/超时/DNS 等传输层错误。结果未知，禁止假成功。 */
    class Network(cause: Throwable) : ApiError(
        message = "网络连接不可用，请检查网络后重试",
        retryable = true,
    )

    class Unexpected(message: String = "发生未知错误，请稍后重试") : ApiError(message)
}
