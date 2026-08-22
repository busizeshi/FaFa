package com.fafa.interfaces.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 微信登录请求
 *
 * @author FaFa Team
 * @since 1.0
 */
@Data
public class LoginRequest {

    /** wx.login() 返回的临时登录凭证 */
    @NotBlank(message = "code 不能为空")
    private String code;
}
