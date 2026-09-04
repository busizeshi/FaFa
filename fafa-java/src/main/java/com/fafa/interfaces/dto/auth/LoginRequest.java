package com.fafa.interfaces.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 微信登录请求
 *
 * @author FaFa Team
 * @since 1.0
 */
@Data
@Schema(description = "微信登录请求")
public class LoginRequest {

    /** wx.login() 返回的临时登录凭证 */
    @NotBlank(message = "code 不能为空")
    @Schema(description = "微信 wx.login() 返回的临时登录凭证", example = "071abc123456")
    private String code;
}
