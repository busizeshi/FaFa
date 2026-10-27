package com.jwd.fafa.core.network

import kotlinx.serialization.json.Json
import retrofit2.Response

/**
 * 所有 API 调用统一走此包装：
 * - 2xx 且 code == "0" → 返回 ApiResponse
 * - code != "0" / HTTP 非 2xx → 抛 ApiError.Biz（解析错误体统一结构 + Retry-After 头）
 * - IOException → 抛 ApiError.Network（结果未知，不得假成功）
 */
suspend fun <T> apiCall(
    json: Json,
    block: suspend () -> Response<ApiResponse<T>>,
): ApiResponse<T> = try {
    val response = block()
    if (response.isSuccessful) {
        val body = response.body() ?: throw ApiError.Unexpected("响应为空")
        if (body.code == "0") {
            body
        } else {
            throw bizError(body.code, body, response.code(), retryAfter(response))
        }
    } else {
        val body = runCatching {
            response.errorBody()?.string()?.let { json.decodeFromString<ApiResponse<T>>(it) }
        }.getOrNull()
        if (body != null && body.code != "0") {
            throw bizError(body.code, body, response.code(), retryAfter(response))
        }
        throw ApiError.Biz(
            code = "http-${response.code()}",
            message = "服务暂时不可用（HTTP ${response.code()}），请稍后重试",
            httpStatus = response.code(),
            retryAfterSeconds = retryAfter(response),
            retryable = response.code() >= 500 || response.code() == 429,
        )
    }
} catch (e: ApiError) {
    throw e
} catch (e: java.io.IOException) {
    throw ApiError.Network(e)
} catch (e: Exception) {
    throw ApiError.Unexpected()
}

private fun bizError(
    code: String,
    body: ApiResponse<*>,
    httpStatus: Int,
    retryAfterSeconds: Long?,
): ApiError.Biz = ApiError.Biz(
    code = code,
    message = body.message.ifBlank { "请求失败（$code）" },
    httpStatus = httpStatus,
    retryAfterSeconds = retryAfterSeconds,
    fieldErrors = body.fieldErrors,
    requestId = body.requestId,
    retryable = body.retryable,
)

/** Retry-After 头：支持秒数格式；HTTP-date 格式暂不支持（服务端当前固定秒数）。 */
fun retryAfter(response: Response<*>): Long? = response.headers()["Retry-After"]?.trim()?.toLongOrNull()
