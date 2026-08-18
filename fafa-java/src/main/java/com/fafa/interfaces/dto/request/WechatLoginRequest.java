package com.fafa.interfaces.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 微信登录请求
 * 
 * @author FaFa Team
 * @since 1.0
 */
@Data
@Schema(description = "微信登录请求")
public class WechatLoginRequest {
    
    @Schema(description = "微信登录凭证 code", required = true)
    @NotBlank(message = "code 不能为空")
    private String code;
}
