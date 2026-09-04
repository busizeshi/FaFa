package com.fafa.interfaces.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 登录响应
 *
 * @author FaFa Team
 * @since 1.0
 */
@Data
@Schema(description = "登录响应")
public class LoginResponse {

    /** 会话令牌，后续请求放入 Authorization 头 */
    @Schema(description = "会话令牌，后续请求放入 Authorization 请求头", example = "satoken-example")
    private String token;

    /** 用户 ID */
    @Schema(description = "用户ID", example = "1")
    private Long userId;

    /** 是否新注册用户（前端可借此引导填写资料） */
    @Schema(description = "是否为首次注册用户", example = "true")
    private boolean newUser;
}
