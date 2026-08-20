package com.fafa.infrastructure.wechat;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.fafa.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 微信小程序服务
 * 
 * @author FaFa Team
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WechatMiniAppService {
    
    private final WechatMiniAppConfig config;
    
    /**
     * 通过 code 换取 openid 和 session_key
     * 
     * @param code 微信登录凭证
     * @return Code2SessionResult
     */
    public Code2SessionResult code2Session(String code) {
        try {
            String url = config.getCode2SessionUrl()
                    .replace("{appid}", config.getAppid())
                    .replace("{secret}", config.getSecret())
                    .replace("{code}", code);
            
            String response = HttpUtil.get(url);
            log.debug("微信 code2Session 响应: {}", response);
            
            JSONObject json = JSONUtil.parseObj(response);
            
            // 检查错误
            if (json.containsKey("errcode") && json.getInt("errcode") != 0) {
                String errmsg = json.getStr("errmsg");
                log.error("微信 code2Session 失败: errcode={}, errmsg={}", json.getInt("errcode"), errmsg);
                throw new BusinessException("微信登录失败: " + errmsg);
            }
            
            return Code2SessionResult.builder()
                    .openid(json.getStr("openid"))
                    .sessionKey(json.getStr("session_key"))
                    .unionid(json.getStr("unionid"))
                    .build();
            
        } catch (Exception e) {
            log.error("调用微信 code2Session 失败", e);
            throw new BusinessException("微信登录失败，请重试");
        }
    }
    
    /**
     * Code2Session 结果
     */
    @lombok.Data
    @lombok.Builder
    public static class Code2SessionResult {
        private String openid;
        private String sessionKey;
        private String unionid;
    }
}
