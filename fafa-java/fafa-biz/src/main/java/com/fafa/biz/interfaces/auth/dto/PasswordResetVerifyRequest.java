package com.fafa.biz.interfaces.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 找回密码第二步：校验验证码换取一次性重置凭据。
 *
 * @param code 邮箱验证码
 * @author FaFa 项目组
 * @version 1.0.0
 */
public record PasswordResetVerifyRequest(

        @NotBlank(message = "验证码不能为空")
        @Pattern(regexp = "\\d{6}", message = "验证码为 6 位数字")
        String code) {
}
