package com.fafa.biz.interfaces.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 找回密码第一步：发送验证码（契约 AUTH /auth/password-reset-challenges）。
 *
 * <p>响应统一为"已受理"，不泄露邮箱是否已注册（设计稿 AUTH-04/1 语义）。</p>
 *
 * @param email 邮箱
 * @author FaFa 项目组
 * @version 1.0.0
 */
public record PasswordResetChallengeRequest(

        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        String email) {
}
