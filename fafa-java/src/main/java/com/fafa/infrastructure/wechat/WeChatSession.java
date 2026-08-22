package com.fafa.infrastructure.wechat;

import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

/**
 * 微信 code2Session 响应
 *
 * @author FaFa Team
 * @since 1.0
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record WeChatSession(
        String openid,
        String sessionKey,
        String unionid,
        Integer errcode,
        String errmsg
) {
}
