package com.fafa.infrastructure.wechat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 微信 code2Session 响应
 *
 * 成功时返回 openid/session_key；失败时返回 errcode/errmsg
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WeChatSessionResponse {

    private String openid;

    private String sessionKey;

    /** 成功时该字段不存在（null） */
    private Integer errcode;

    private String errmsg;
}
