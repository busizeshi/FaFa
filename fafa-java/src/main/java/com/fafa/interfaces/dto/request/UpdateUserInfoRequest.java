package com.fafa.interfaces.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新用户信息请求
 * 
 * @author FaFa Team
 * @since 1.0
 */
@Data
@Schema(description = "更新用户信息请求")
public class UpdateUserInfoRequest {
    
    @Schema(description = "昵称", example = "小明的猫咪")
    @Size(max = 50, message = "昵称长度不能超过50个字符")
    private String nickname;
    
    @Schema(description = "手机号", example = "13800138000")
    @Size(max = 20, message = "手机号长度不能超过20个字符")
    private String phone;
    
    @Schema(description = "性别：0-未知, 1-男, 2-女", example = "1")
    @Min(value = 0, message = "性别值必须在0-2之间")
    @Max(value = 2, message = "性别值必须在0-2之间")
    private Integer gender;
}
