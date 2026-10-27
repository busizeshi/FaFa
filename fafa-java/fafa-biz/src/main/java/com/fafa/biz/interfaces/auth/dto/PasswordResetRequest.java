package com.fafa.biz.interfaces.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 找回密码第三步：使用一次性凭据设置新密码（契约 /auth/password-resets）。
 *
 * <p>成功后服务端撤销该用户全部会话，客户端跳转登录页（设计稿 AUTH-04/2 语义）。</p>
 *
 * @param resetCredential verify 步骤签发的一次性凭据
 * @param newPassword     新密码
 * @author FaFa 项目组
 * @version 1.0.0
 */
public record PasswordResetRequest(

        @NotBlank(message = "重置凭据不能为空")
        String resetCredential,

        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 64, message = "密码长度需 8-64 位")
        String newPassword) {
}
