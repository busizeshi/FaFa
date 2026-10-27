package com.jwd.fafa.feature.auth

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** M00 认证域路由（Navigation 3 类型安全 Key）。 */
sealed interface AuthNavKey : NavKey

@Serializable
data object WelcomeRoute : AuthNavKey

@Serializable
data class LegalRoute(val documentType: String) : AuthNavKey

@Serializable
data object LoginRoute : AuthNavKey

@Serializable
data object RegisterRoute : AuthNavKey

/** AUTH-03 邮箱验证（注册校验）。找回密码校验在 ResetPasswordRoute 内部向导完成。 */
@Serializable
data class VerifyRoute(
    val challengeId: Long,
    val maskedEmail: String,
    val expiresAtEpochMs: Long,
) : AuthNavKey

/** AUTH-04 找回密码：同一全屏向导两步（发码+验证 → 设置新密码）。 */
@Serializable
data class ResetPasswordRoute(
    val prefillEmail: String? = null,
) : AuthNavKey

/** AUTH-05 首次建档入口（登录/验证成功后的落点，M00 提供入口 UI 与设备管理）。 */
@Serializable
data object OnboardingEntryRoute : AuthNavKey
