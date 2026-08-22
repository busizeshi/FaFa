package com.fafa.interfaces.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 会员信息响应
 *
 * @author FaFa Team
 * @since 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "会员信息响应")
public class VipInfoResponse {

    @Schema(description = "是否会员")
    private boolean isVip;

    @Schema(description = "会员是否有效")
    private boolean isVipValid;

    @Schema(description = "会员到期时间")
    private LocalDateTime vipExpireTime;
}
