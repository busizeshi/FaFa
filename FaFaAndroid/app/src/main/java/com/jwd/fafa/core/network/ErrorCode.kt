package com.jwd.fafa.core.network

/** M00 错误码表（docs/接口文档.md §0.3）。code 前三位即 HTTP 状态。 */
object ErrorCode {
    const val BAD_REQUEST = "400-00-00"
    const val UNAUTHORIZED = "401-00-00"
    const val INVALID_CREDENTIALS = "401-01-01"
    const val RESET_CREDENTIAL_INVALID = "401-01-02"
    const val ACCOUNT_SUSPENDED = "403-01-01"
    const val CHALLENGE_NOT_FOUND = "404-01-01"
    const val EMAIL_ALREADY_REGISTERED = "409-01-01"
    const val CODE_EXPIRED = "422-01-01"
    const val CODE_INVALID = "422-01-02"
    const val TOO_MANY_ATTEMPTS = "422-01-03"
    const val WEAK_PASSWORD = "422-01-04"
    const val LEGAL_VERSION_MISMATCH = "422-01-05"
    const val SEND_TOO_FREQUENT = "429-01-01"
    const val DAILY_SEND_LIMIT = "429-01-02"
    const val LOGIN_RATE_LIMITED = "429-01-03"
    const val RATE_LIMITED = "429-01-04"
    const val SERVER_BUSY = "500-00-00"
    const val MAIL_SEND_FAILED = "503-01-01"
}
