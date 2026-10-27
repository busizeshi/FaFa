package com.fafa.biz.interfaces.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求（契约 AUTH /auth/login）。
 *
 * @param email    登录邮箱
 * @param password 密码
 * @author FaFa 项目组
 * @version 1.0.0
 */
public record LoginRequest(

        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        String email,

        @NotBlank(message = "密码不能为空")
        String password) {
}
