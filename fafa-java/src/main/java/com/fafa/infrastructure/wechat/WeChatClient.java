package com.fafa.infrastructure.wechat;

import com.fafa.common.BusinessException;
import com.fafa.common.ErrorCode;
import com.fafa.infrastructure.config.FaFaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 微信小程序服务端接口客户端
 *
 * 文档：<a href="https://developers.weixin.qq.com/miniprogram/dev/OpenApiDoc/user-login/code2Session.html">...</a>
 *
 * @author FaFa Team
 * @since 1.0
 */
@Slf4j
@Component
public class WeChatClient {

    private static final String CODE2SESSION_URL = "https://api.weixin.qq.com/sns/jscode2session";

    private final FaFaProperties properties;
    private final RestClient wechatRestClient;

    public WeChatClient(FaFaProperties properties, RestClient wechatRestClient) {
        this.properties = properties;
        this.wechatRestClient = wechatRestClient;
    }

    /**
     * 以 wx.login() 的 code 换取 openid / session_key
     *
     * @throws BusinessException code 无效或微信接口异常
     */
    public WeChatSession code2Session(String code) {
        WeChatSession session;
        try {
            session = wechatRestClient.get()
                    .uri(CODE2SESSION_URL + "?appid={appid}&secret={secret}&js_code={code}&grant_type=authorization_code",
                            properties.getWechat().getAppid(),
                            properties.getWechat().getSecret(),
                            code)
                    .retrieve()
                    .body(WeChatSession.class);
        } catch (Exception ex) {
            // 微信 code 不落日志，避免敏感信息泄露
            log.error("调用微信 code2Session 失败", ex);
            throw new BusinessException(ErrorCode.USER_LOGIN_FAILED);
        }

        if (session == null || session.openid() == null || session.errcode() != null && session.errcode() != 0) {
            log.warn("微信 code2Session 返回异常: errcode={}, errmsg={}",
                    session == null ? null : session.errcode(),
                    session == null ? null : session.errmsg());
            throw new BusinessException(ErrorCode.WECHAT_CODE_INVALID);
        }
        return session;
    }
}
