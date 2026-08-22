package com.fafa.application.dto.user;

import java.time.LocalDateTime;

/**
 * 会员信息 DTO
 *
 * @author FaFa Team
 * @since 1.0
 */
public record VipInfoDTO(
    boolean isVip,
    boolean isVipValid,
    LocalDateTime vipExpireTime
) {
}
