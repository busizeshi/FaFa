package com.fafa.interfaces.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 登录响应
 * 
 * @author FaFa Team
 * @since 1.0
 */
@Data
@Builder
@Schema(description = "登录响应")
public class LoginResponse {
    
    @Schema(description = "认证 Token")
    private String token;
    
    @Schema(description = "用户 ID")
    private Long userId;
    
    @Schema(description = "昵称")
    private String nickname;
    
    @Schema(description = "头像")
    private String avatar;
    
    @Schema(description = "是否新用户")
    private Boolean isNewUser;
}
