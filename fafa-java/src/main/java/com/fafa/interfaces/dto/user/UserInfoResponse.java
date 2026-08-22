package com.fafa.interfaces.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户信息响应
 *
 * @author FaFa Team
 * @since 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户信息响应")
public class UserInfoResponse {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "微信 openId")
    private String openId;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "性别")
    private String gender;

    @Schema(description = "年龄")
    private Integer age;

    @Schema(description = "头像URL")
    private String avatarUrl;

    @Schema(description = "城市")
    private String city;

    @Schema(description = "是否会员")
    private boolean vip;

    @Schema(description = "会员到期时间")
    private LocalDateTime vipExpireTime;

    @Schema(description = "显示名称")
    private String displayName;
}
