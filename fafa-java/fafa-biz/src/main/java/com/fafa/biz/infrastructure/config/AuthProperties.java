package com.fafa.biz.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 认证模块配置项（fafa.auth.*）。
 *
 * <p>业务规则尽量服务端配置化（手册 §11）：验证码长度/有效期/冷却/上限、
 * 密码策略、限流阈值均在此集中定义；修改只动配置，不改代码。</p>
 *
 * @author FaFa 项目组
 * @version 1.0.0
 */
@ConfigurationProperties(prefix = "fafa.auth")
public class AuthProperties {

    /** 验证码长度（数字位数） */
    private int codeLength = 6;

    /** 验证码有效期（分钟） */
    private int codeTtlMinutes = 10;

    /** 重发冷却（秒）：同一邮箱同一用途两次发送的最小间隔 */
    private int resendCooldownSeconds = 60;

    /** 单邮箱每日发送上限（所有用途合计） */
    private int dailySendLimit = 10;

    /** 单个挑战最大校验尝试次数，超限作废 */
    private int maxVerifyAttempts = 5;

    /** 密码最小长度 */
    private int passwordMinLength = 8;

    /** 密码最大长度 */
    private int passwordMaxLength = 64;

    /** 登录限流：同一 IP 时间窗口（分钟） */
    private int loginIpWindowMinutes = 5;

    /** 登录限流：同一 IP 窗口内最大次数 */
    private int loginIpLimit = 20;

    /** 登录限流：同一邮箱窗口（分钟） */
    private int loginEmailWindowMinutes = 15;

    /** 登录限流：同一邮箱窗口内最大失败次数 */
    private int loginEmailLimit = 5;

    /** 找回密码一次性凭据有效期（分钟，存 Redis） */
    private int resetCredentialTtlMinutes = 10;

    /** 客户端内置协议文本版本（bootstrap 下发，注册时校验一致） */
    private Legal legal = new Legal();

    /**
     * 协议版本配置。
     */
    public static class Legal {

        /** 用户协议当前版本（客户端内置文本对应版本） */
        private String userAgreementVersion = "1.0";

        /** 隐私政策当前版本 */
        private String privacyPolicyVersion = "1.0";

        public String getUserAgreementVersion() {
            return userAgreementVersion;
        }

        public void setUserAgreementVersion(String userAgreementVersion) {
            this.userAgreementVersion = userAgreementVersion;
        }

        public String getPrivacyPolicyVersion() {
            return privacyPolicyVersion;
        }

        public void setPrivacyPolicyVersion(String privacyPolicyVersion) {
            this.privacyPolicyVersion = privacyPolicyVersion;
        }
    }

    public int getCodeLength() {
        return codeLength;
    }

    public void setCodeLength(int codeLength) {
        this.codeLength = codeLength;
    }

    public int getCodeTtlMinutes() {
        return codeTtlMinutes;
    }

    public void setCodeTtlMinutes(int codeTtlMinutes) {
        this.codeTtlMinutes = codeTtlMinutes;
    }

    public int getResendCooldownSeconds() {
        return resendCooldownSeconds;
    }

    public void setResendCooldownSeconds(int resendCooldownSeconds) {
        this.resendCooldownSeconds = resendCooldownSeconds;
    }

    public int getDailySendLimit() {
        return dailySendLimit;
    }

    public void setDailySendLimit(int dailySendLimit) {
        this.dailySendLimit = dailySendLimit;
    }

    public int getMaxVerifyAttempts() {
        return maxVerifyAttempts;
    }

    public void setMaxVerifyAttempts(int maxVerifyAttempts) {
        this.maxVerifyAttempts = maxVerifyAttempts;
    }

    public int getPasswordMinLength() {
        return passwordMinLength;
    }

    public void setPasswordMinLength(int passwordMinLength) {
        this.passwordMinLength = passwordMinLength;
    }

    public int getPasswordMaxLength() {
        return passwordMaxLength;
    }

    public void setPasswordMaxLength(int passwordMaxLength) {
        this.passwordMaxLength = passwordMaxLength;
    }

    public int getLoginIpWindowMinutes() {
        return loginIpWindowMinutes;
    }

    public void setLoginIpWindowMinutes(int loginIpWindowMinutes) {
        this.loginIpWindowMinutes = loginIpWindowMinutes;
    }

    public int getLoginIpLimit() {
        return loginIpLimit;
    }

    public void setLoginIpLimit(int loginIpLimit) {
        this.loginIpLimit = loginIpLimit;
    }

    public int getLoginEmailWindowMinutes() {
        return loginEmailWindowMinutes;
    }

    public void setLoginEmailWindowMinutes(int loginEmailWindowMinutes) {
        this.loginEmailWindowMinutes = loginEmailWindowMinutes;
    }

    public int getLoginEmailLimit() {
        return loginEmailLimit;
    }

    public void setLoginEmailLimit(int loginEmailLimit) {
        this.loginEmailLimit = loginEmailLimit;
    }

    public int getResetCredentialTtlMinutes() {
        return resetCredentialTtlMinutes;
    }

    public void setResetCredentialTtlMinutes(int resetCredentialTtlMinutes) {
        this.resetCredentialTtlMinutes = resetCredentialTtlMinutes;
    }

    public Legal getLegal() {
        return legal;
    }

    public void setLegal(Legal legal) {
        this.legal = legal;
    }
}
