package com.jwd.fafa.core.common

/**
 * 客户端本地校验（与服务端契约口径一致：8–64 位、必须同时包含字母和数字）。
 * 注意：页面规格 §AUTH-02 写 8–128 为拟定参数，服务端契约为 8–64，以契约为准
 * （已在联调纪要登记差异）。
 */
object Validators {

    private val LETTER_OR_DIGIT = Regex("^(?=.*[A-Za-z])(?=.*\\d).{8,64}$")

    fun isValidEmail(email: String): Boolean = ANDROID_EMAIL.matches(email)

    fun passwordIssue(password: String): String? = when {
        password.isEmpty() -> "请输入密码"
        password.length < 8 -> "密码至少 8 位"
        password.length > 64 -> "密码最多 64 位"
        !password.any { it.isLetter() } || !password.any { it.isDigit() } -> "密码需同时包含字母和数字"
        else -> null
    }

    fun isValidPassword(password: String): Boolean = passwordIssue(password) == null

    fun isValidVerificationCode(code: String): Boolean = code.length == 6 && code.all { it.isDigit() }

    fun isValidNickname(nickname: String): Boolean = nickname.length in 1..20

    private val ANDROID_EMAIL = Regex(
        "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    )
}
