package com.fafa.infrastructure.wechat;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 微信小程序配置
 * 
 * @author FaFa Team
 * @since 1.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "wechat.miniapp")
public class WechatMiniAppConfig {
    
    /**
     * 小程序 AppID
     */
    private String appid;
    
    /**
     * 小程序 AppSecret
     */
    private String secret;
}
