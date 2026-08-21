package com.fafa.interfaces.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 微信登录请求
 */
@Data
public class WechatLoginRequest {

    /** wx.login 获取的临时登录凭证 */
    @NotBlank(message = "code 不能为空")
    private String code;
}
