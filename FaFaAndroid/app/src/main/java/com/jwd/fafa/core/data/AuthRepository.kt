package com.jwd.fafa.core.data

import com.jwd.fafa.core.network.ApiError
import com.jwd.fafa.core.network.AuthApi
import com.jwd.fafa.core.network.ChallengeDto
import com.jwd.fafa.core.network.EmailOnlyRequest
import com.jwd.fafa.core.network.LoginRequest
import com.jwd.fafa.core.network.RegisterRequest
import com.jwd.fafa.core.network.ResetPasswordRequest
import com.jwd.fafa.core.network.SessionDto
import com.jwd.fafa.core.network.SessionInfoDto
import com.jwd.fafa.core.network.VerifyRequest
import com.jwd.fafa.core.network.apiCall
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json

/** M00 认证用例集合。会话签发类接口成功后立即写入安全存储。 */
@Singleton
class AuthRepository @Inject constructor(
    private val api: AuthApi,
    private val json: Json,
    private val sessionStore: SessionStore,
) {

    suspend fun register(
        idempotencyKey: String,
        email: String,
        password: String,
        acceptedLegal: List<Pair<String, String>>,
    ): ChallengeDto {
        val body = RegisterRequest(
            email = email,
            password = password,
            acceptedLegal = acceptedLegal.map { com.jwd.fafa.core.network.LegalAcceptDto(it.first, it.second) },
        )
        return requireData(apiCall(json) { api.register(idempotencyKey, body) })
    }

    /** 注册验证成功即激活并签发会话。 */
    suspend fun verifyRegisterChallenge(challengeId: Long, code: String): SessionDto {
        val result = requireData(apiCall(json) { api.verifyRegisterChallenge(challengeId, VerifyRequest(code)) })
        val session = result.session ?: throw ApiError.Unexpected("服务未返回会话")
        sessionStore.save(session)
        return session
    }

    suspend fun login(email: String, password: String): SessionDto {
        val session = requireData(apiCall(json) { api.login(LoginRequest(email, password)) })
        sessionStore.save(session)
        return session
    }

    suspend fun createPasswordResetChallenge(email: String): ChallengeDto =
        requireData(apiCall(json) { api.createPasswordResetChallenge(EmailOnlyRequest(email)) })

    suspend fun verifyPasswordResetChallenge(challengeId: Long, code: String): String =
        requireData(apiCall(json) { api.verifyPasswordResetChallenge(challengeId, VerifyRequest(code)) })
            .resetCredential ?: throw ApiError.Unexpected("服务未返回重置凭据")

    suspend fun resetPassword(resetCredential: String, newPassword: String) {
        apiCall(json) { api.resetPassword(ResetPasswordRequest(resetCredential, newPassword)) }
        // 契约：成功后服务端撤销该用户全部会话；客户端必须清空本地令牌
        sessionStore.clear()
    }

    suspend fun logout() {
        try {
            apiCall(json) { api.logout() }
        } finally {
            // 无论接口结果如何，本地会话与缓存都清空（手册 M00：登出后缓存不能打开旧账号页面）
            sessionStore.clear()
        }
    }

    suspend fun sessions(): List<SessionInfoDto> =
        requireData(apiCall(json) { api.sessions() })

    suspend fun revokeSession(sessionId: String) {
        apiCall(json) { api.revokeSession(sessionId) }
    }

    private fun <T> requireData(response: com.jwd.fafa.core.network.ApiResponse<T>): T =
        response.data ?: throw ApiError.Unexpected("服务返回数据为空")
}
