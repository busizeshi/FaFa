package com.fafa.biz.domain;

import com.fafa.common.api.ErrorCode;

/**
 * M00 认证模块稳定错误码（对齐接口契约 v2.1 AUTH 组与手册 §7-M00）。
 *
 * <p>语义红线：</p>
 * <ul>
 *   <li>{@link #AUTH_INVALID_CREDENTIALS} 对"邮箱不存在"与"密码错误"返回同一文案，
 *       不透露邮箱是否已注册（设计稿 AUTH-02 / invalid-credentials 状态帧）；</li>
 *   <li>限流类错误必须通过响应头 {@code Retry-After} 携带服务端提供的重试时间
 *       （设计稿 AUTH-02 / rate-limited 状态帧）；</li>
 *   <li>找回密码的"发送结果"不泄露邮箱是否已注册，因此没有专门的"邮箱不存在"错误码。</li>
 * </ul>
 *
 * @author FaFa 项目组
 * @version 1.0.0
 */
public enum AuthErrorCode implements ErrorCode {

    /** 邮箱已被注册且已完成验证：注册阶段可直接暴露（409 冲突） */
    AUTH_EMAIL_ALREADY_REGISTERED("409-01-01", "该邮箱已注册，请直接登录"),

    /** 登录凭据错误：邮箱不存在与密码错误统一文案（401） */
    AUTH_INVALID_CREDENTIALS("401-01-01", "邮箱或密码不正确"),

    /** 账号被运营停用（403） */
    AUTH_ACCOUNT_SUSPENDED("403-01-01", "账号已被停用，如有疑问请联系客服"),

    /** 验证挑战不存在 / 已消费 / 已作废，统一文案避免可枚举性（404） */
    AUTH_CHALLENGE_NOT_FOUND("404-01-01", "验证请求不存在或已失效，请重新获取"),

    /** 验证码已过期（422） */
    AUTH_CHALLENGE_EXPIRED("422-01-01", "验证码已过期，请重新获取"),

    /** 验证码不匹配（422） */
    AUTH_CHALLENGE_CODE_MISMATCH("422-01-02", "验证码错误，请检查后重试"),

    /** 校验尝试超限，挑战作废（422） */
    AUTH_CHALLENGE_ATTEMPT_EXCEEDED("422-01-03", "尝试次数过多，请重新获取验证码"),

    /** 验证码重发冷却中（429，message 由服务端补充剩余秒数） */
    AUTH_RESEND_COOLDOWN("429-01-01", "发送太频繁，请稍后再试"),

    /** 单邮箱每日发送量超限（429） */
    AUTH_EMAIL_SEND_LIMIT("429-01-02", "今日获取验证码次数已达上限，请明日再试"),

    /** 登录限流（429，message 由服务端补充重试时间） */
    AUTH_LOGIN_RATE_LIMITED("429-01-03", "登录尝试过于频繁，请稍后再试"),

    /** 注册 / 重置限流（429） */
    AUTH_REQUEST_RATE_LIMITED("429-01-04", "请求过于频繁，请稍后再试"),

    /** 密码强度不足（422） */
    AUTH_WEAK_PASSWORD("422-01-04", "密码长度需 8-64 位，且包含字母和数字"),

    /** 重置凭据（resetCredential）无效或过期（401） */
    AUTH_RESET_CREDENTIAL_INVALID("401-01-02", "重置链接已失效，请重新发起找回密码"),

    /** 未同意必需版本的协议（422） */
    AUTH_LEGAL_CONSENT_REQUIRED("422-01-05", "请阅读并同意最新版用户协议与隐私政策"),

    /** 邮件发送失败（503，可重试：客户端点击"重新发送"） */
    AUTH_MAIL_SEND_FAILED("503-01-01", "验证邮件发送失败，请稍后重试");

    private final String code;
    private final String message;

    /**
     * @param code    稳定错误码
     * @param message 对外默认提示
     */
    AuthErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    /** {@inheritDoc} */
    @Override
    public String getCode() {
        return code;
    }

    /** {@inheritDoc} */
    @Override
    public String getMessage() {
        return message;
    }
}
