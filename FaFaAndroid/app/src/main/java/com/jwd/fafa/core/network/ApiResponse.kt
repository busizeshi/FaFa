package com.jwd.fafa.core.network

import kotlinx.serialization.Serializable

/** 统一响应结构（docs/接口文档.md §0.2）。失败时 data 可能省略。 */
@Serializable
data class ApiResponse<T>(
    val code: String = "0",
    val message: String = "",
    val data: T? = null,
    val requestId: String? = null,
    val retryable: Boolean = false,
    val fieldErrors: Map<String, String> = emptyMap(),
)
