package com.fafa.infrastructure.wechat;

import com.fafa.domain.common.ErrorCode;
import com.fafa.domain.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 微信 API 客户端
 *
 * code2Session：小程序 wx.login 的 code 换取 openid
 */
@Slf4j
@Component
public class WeChatClient {

    private static final String JSCODE2SESSION_URL =
            "https://api.weixin.qq.com/sns/jscode2session?appid={appid}&secret={secret}&js_code={code}&grant_type=authorization_code";

    private final RestClient restClient;
    private final String appid;
    private final String secret;

    public WeChatClient(RestClient.Builder builder,
                        @Value("${fafa.wechat.appid}") String appid,
                        @Value("${fafa.wechat.secret}") String secret) {
        this.restClient = builder.build();
        this.appid = appid;
        this.secret = secret;
    }

    /**
     * 用小程序登录 code 换取 openid
     *
     * @param code wx.login 获取的临时凭证
     * @return openid（用户在当前小程序下的唯一标识）
     */
    public String code2Session(String code) {
        WeChatSessionResponse response;
        try {
            response = restClient.get()
                    .uri(JSCODE2SESSION_URL, appid, secret, code)
                    .retrieve()
                    .body(WeChatSessionResponse.class);
        } catch (Exception e) {
            log.error("微信 code2Session 请求失败: code={}", code, e);
            throw new BusinessException(ErrorCode.WECHAT_LOGIN_FAILED);
        }
        if (response == null || response.getOpenid() == null) {
            int errcode = response == null || response.getErrcode() == null ? -1 : response.getErrcode();
            String errmsg = response == null ? "empty response" : response.getErrmsg();
            log.warn("微信 code2Session 失败: errcode={}, errmsg={}", errcode, errmsg);
            throw new BusinessException(ErrorCode.WECHAT_LOGIN_FAILED);
        }
        log.info("微信 code2Session 成功");
        return response.getOpenid();
    }
}
