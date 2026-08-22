package com.fafa.application.dto.user;

import java.time.LocalDateTime;

/**
 * 用户信息 DTO
 *
 * @author FaFa Team
 * @since 1.0
 */
public record UserInfoDTO(
    Long id,
    String openId,
    String nickname,
    String phone,
    String gender,
    Integer age,
    String avatarUrl,
    String city,
    boolean vip,
    LocalDateTime vipExpireTime,
    String displayName
) {
}
