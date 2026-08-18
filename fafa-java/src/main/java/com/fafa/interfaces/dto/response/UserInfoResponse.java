package com.fafa.interfaces.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * 用户信息响应
 * 
 * @author FaFa Team
 * @since 1.0
 */
@Data
@Builder
@Schema(description = "用户信息响应")
public class UserInfoResponse {
    
    @Schema(description = "用户 ID")
    private Long userId;
    
    @Schema(description = "昵称")
    private String nickname;
    
    @Schema(description = "头像")
    private String avatar;
    
    @Schema(description = "手机号")
    private String phone;
    
    @Schema(description = "性别：0-未知, 1-男, 2-女")
    private Integer gender;
    
    @Schema(description = "注册日期")
    private LocalDate registerDate;
}
