package com.fafa.interfaces.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新用户资料请求
 *
 * @author FaFa Team
 * @since 1.0
 */
@Data
@Schema(description = "更新用户资料请求")
public class UpdateProfileRequest {

    @Schema(description = "昵称")
    @Size(max = 50, message = "昵称不能超过50个字符")
    private String nickname;

    @Schema(description = "手机号")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Schema(description = "性别", allowableValues = {"男", "女", "未知"})
    private String gender;

    @Schema(description = "年龄")
    @Min(value = 0, message = "年龄不能小于0")
    @Max(value = 150, message = "年龄不能大于150")
    private Integer age;

    @Schema(description = "城市")
    @Size(max = 50, message = "城市不能超过50个字符")
    private String city;
}
