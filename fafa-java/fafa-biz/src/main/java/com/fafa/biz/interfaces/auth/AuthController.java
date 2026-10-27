package com.fafa.biz.interfaces.auth;

import com.fafa.biz.application.auth.AuthApplicationService;
import com.fafa.biz.infrastructure.config.AuthProperties;
import com.fafa.biz.interfaces.auth.dto.AuthSessionResponse;
import com.fafa.biz.interfaces.auth.dto.BootstrapResponse;
import com.fafa.biz.interfaces.auth.dto.ChallengeResponse;
import com.fafa.biz.interfaces.auth.dto.ChallengeVerifyRequest;
import com.fafa.biz.interfaces.auth.dto.LoginRequest;
import com.fafa.biz.interfaces.auth.dto.PasswordResetChallengeRequest;
import com.fafa.biz.interfaces.auth.dto.PasswordResetRequest;
import com.fafa.biz.interfaces.auth.dto.PasswordResetVerifyRequest;
import com.fafa.biz.interfaces.auth.dto.RegisterRequest;
import com.fafa.biz.interfaces.auth.dto.ResetCredentialResponse;
import com.fafa.common.api.ApiResponse;
import com.fafa.common.constant.HeaderConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 认证接口（契约 AUTH / BOOTSTRAP 组，M00）。
 *
 * <p>公开端点：bootstrap / register / login / challenges / password-reset 系；
 * logout 需要会话（由 Sa-Token 拦截器统一校验）。</p>
 *
 * @author FaFa 项目组
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "AUTH · 认证", description = "注册 / 登录 / 邮箱验证 / 找回密码 / 退出（M00）")
public class AuthController {

    private final AuthApplicationService authService;
    private final AuthProperties authProperties;

    /**
     * @param authService    认证应用服务
     * @param authProperties 认证配置（bootstrap 下发协议版本）
     */
    public AuthController(AuthApplicationService authService, AuthProperties authProperties) {
        this.authService = authService;
        this.authProperties = authProperties;
    }

    /**
     * 启动引导：功能开关、协议版本、上传能力、字典版本（公开）。
     *
     * @return bootstrap 响应
     */
    @GetMapping("/bootstrap")
    @Operation(summary = "启动引导（公开）：读取功能/协议/字典版本")
    public ApiResponse<BootstrapResponse> bootstrap() {
        BootstrapResponse response = BootstrapResponse.m00Defaults();
        // 协议版本由服务端配置下发，客户端内置文本以此对齐
        BootstrapResponse withLegal = new BootstrapResponse(
                response.featureFlags(),
                Map.of(
                        "USER_AGREEMENT", authProperties.getLegal().getUserAgreementVersion(),
                        "PRIVACY_POLICY", authProperties.getLegal().getPrivacyPolicyVersion()),
                response.uploadCapabilities(),
                response.dictionaryVersions(),
                response.serverTime());
        return ApiResponse.ok(withLegal);
    }

    /**
     * 注册：创建未验证账号并发送邮箱验证码。
     *
     * @param request        注册请求（邮箱/密码/协议同意）
     * @param idempotencyKey 幂等键（断网重试防重复建档）
     * @param httpRequest    原始请求（取客户端 IP 与 UA）
     * @return 挑战受理（challengeId + 脱敏邮箱 + 过期时间）
     */
    @PostMapping("/auth/register")
    @Operation(summary = "注册：建档并发送验证码")
    public ApiResponse<ChallengeResponse> register(
            @Valid @RequestBody RegisterRequest request,
            @RequestHeader(value = HeaderConstants.IDEMPOTENCY_KEY, required = false) String idempotencyKey,
            HttpServletRequest httpRequest) {
        ChallengeResponse response = authService.register(
                request, idempotencyKey, clientIp(httpRequest));
        return ApiResponse.ok(response);
    }

    /**
     * 登录：校验凭据并签发会话。
     *
     * @param request      登录请求
     * @param httpRequest  原始请求
     * @return 会话响应（tokenName/tokenValue/用户摘要）
     */
    @PostMapping("/auth/login")
    @Operation(summary = "登录：校验凭据并签发会话")
    public ApiResponse<AuthSessionResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        AuthSessionResponse response = authService.login(
                request.email(), request.password(),
                clientIp(httpRequest), httpRequest.getHeader("User-Agent"));
        return ApiResponse.ok(response);
    }

    /**
     * 验证码校验：注册验证成功 → 签发会话；找回密码验证成功 → 签发一次性重置凭据。
     *
     * @param challengeId 挑战 ID
     * @param request     验证码
     * @param httpRequest 原始请求
     * @return 二选一响应
     */
    @PostMapping("/auth/challenges/{id}/verify")
    @Operation(summary = "验证码校验（注册验证 / 找回密码第一步校验）")
    public ApiResponse<AuthApplicationService.VerifyChallengeResponse> verifyChallenge(
            @Parameter(description = "挑战 ID（注册/找回密码发送步骤返回）", required = true)
            @PathVariable("id") Long challengeId,
            @Valid @RequestBody ChallengeVerifyRequest request,
            HttpServletRequest httpRequest) {
        return ApiResponse.ok(authService.verifyChallenge(
                challengeId, request.code(), httpRequest.getHeader("User-Agent")));
    }

    /**
     * 找回密码第一步：发送验证码（响应统一受理，不泄露注册状态）。
     *
     * @param request     邮箱
     * @param httpRequest 原始请求
     * @return 受理响应
     */
    @PostMapping("/auth/password-reset-challenges")
    @Operation(summary = "找回密码：发送验证码")
    public ApiResponse<ChallengeResponse> createPasswordResetChallenge(
            @Valid @RequestBody PasswordResetChallengeRequest request,
            HttpServletRequest httpRequest) {
        return ApiResponse.ok(authService.createPasswordResetChallenge(
                request.email(), clientIp(httpRequest)));
    }

    /**
     * 找回密码第二步：校验验证码换取一次性重置凭据。
     *
     * @param challengeId 挑战 ID（客户端从发送步骤的 mail/交互上下文获得）
     * @param request     验证码
     * @param httpRequest 原始请求
     * @return 一次性凭据
     */
    @PostMapping("/auth/password-reset-challenges/{id}/verify")
    @Operation(summary = "找回密码：校验验证码换一次性凭据")
    public ApiResponse<ResetCredentialResponse> verifyPasswordResetChallenge(
            @Parameter(description = "找回密码挑战 ID", required = true)
            @PathVariable("id") Long challengeId,
            @Valid @RequestBody PasswordResetVerifyRequest request,
            HttpServletRequest httpRequest) {
        AuthApplicationService.VerifyChallengeResponse response =
                authService.verifyChallenge(challengeId, request.code(), httpRequest.getHeader("User-Agent"));
        // 该端点只处理 PASSWORD_RESET 用途的挑战
        if (response.resetCredential() == null) {
            throw new com.fafa.common.exception.BizException(
                    com.fafa.biz.domain.AuthErrorCode.AUTH_CHALLENGE_NOT_FOUND);
        }
        return ApiResponse.ok(response.resetCredential());
    }

    /**
     * 找回密码第三步：设置新密码（成功后撤销全部会话）。
     *
     * @param request 一次性凭据 + 新密码
     * @return 成功确认（客户端跳转登录页）
     */
    @PostMapping("/auth/password-resets")
    @Operation(summary = "找回密码：设置新密码并撤销全部会话")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        authService.resetPassword(request.resetCredential(), request.newPassword());
        return ApiResponse.ok(null);
    }

    /**
     * 退出登录：撤销当前会话（需登录）。
     *
     * @return 成功确认
     */
    @PostMapping("/auth/logout")
    @Operation(summary = "退出登录（需会话）")
    @SecurityRequirement(name = "fafa-token")
    public ApiResponse<Void> logout() {
        authService.logout();
        return ApiResponse.ok(null);
    }

    /**
     * 取客户端 IP：优先 X-Forwarded-For 首段（网关注入），回退 remoteAddr。
     *
     * @param request 原始请求
     * @return 客户端 IP
     */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
