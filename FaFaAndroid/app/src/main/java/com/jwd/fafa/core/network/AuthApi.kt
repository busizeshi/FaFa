package com.jwd.fafa.core.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

/** M00 认证模块 API（docs/接口文档.md §1–§10）。 */
interface AuthApi {

    @GET("api/v1/bootstrap")
    suspend fun bootstrap(): Response<ApiResponse<BootstrapDto>>

    @POST("api/v1/auth/register")
    suspend fun register(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: RegisterRequest,
    ): Response<ApiResponse<ChallengeDto>>

    @POST("api/v1/auth/challenges/{id}/verify")
    suspend fun verifyRegisterChallenge(
        @Path("id") challengeId: Long,
        @Body body: VerifyRequest,
    ): Response<ApiResponse<VerifyResultDto>>

    @POST("api/v1/auth/login")
    suspend fun login(@Body body: LoginRequest): Response<ApiResponse<SessionDto>>

    @POST("api/v1/auth/password-reset-challenges")
    suspend fun createPasswordResetChallenge(@Body body: EmailOnlyRequest): Response<ApiResponse<ChallengeDto>>

    @POST("api/v1/auth/password-reset-challenges/{id}/verify")
    suspend fun verifyPasswordResetChallenge(
        @Path("id") challengeId: Long,
        @Body body: VerifyRequest,
    ): Response<ApiResponse<ResetCredentialDto>>

    @POST("api/v1/auth/password-resets")
    suspend fun resetPassword(@Body body: ResetPasswordRequest): Response<ApiResponse<Unit>>

    @POST("api/v1/auth/logout")
    suspend fun logout(): Response<ApiResponse<Unit>>

    @GET("api/v1/me/sessions")
    suspend fun sessions(): Response<ApiResponse<List<SessionInfoDto>>>

    @DELETE("api/v1/me/sessions/{id}")
    suspend fun revokeSession(@Path("id") sessionId: String): Response<ApiResponse<Unit>>
}
