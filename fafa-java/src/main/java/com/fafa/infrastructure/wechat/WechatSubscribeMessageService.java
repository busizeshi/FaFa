package com.fafa.infrastructure.wechat;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 微信订阅消息服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WechatSubscribeMessageService {

    private static final String ACCESS_TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s";
    private static final String SEND_MESSAGE_URL = "https://api.weixin.qq.com/cgi-bin/message/subscribe/send?access_token=%s";
    private static final String REDIS_KEY_ACCESS_TOKEN = "wechat:access_token";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Value("${wechat.appid}")
    private String appid;

    @Value("${wechat.secret}")
    private String secret;

    @Value("${wechat.reminder-template-id}")
    private String reminderTemplateId;

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 发送提醒订阅消息
     */
    public boolean sendReminderMessage(String openid, Long petId, String petName, 
                                        String title, LocalDateTime remindTime) {
        try {
            log.info("发送提醒订阅消息，openid: {}, petId: {}, title: {}", openid, petId, title);

            // 1. 获取 Access Token
            String accessToken = getAccessToken();
            if (StrUtil.isBlank(accessToken)) {
                log.error("获取 Access Token 失败");
                return false;
            }

            // 2. 构建消息体
            Map<String, Object> message = new HashMap<>();
            message.put("touser", openid);
            message.put("template_id", reminderTemplateId);
            message.put("page", "pages/pet/detail?petId=" + petId);
            message.put("miniprogram_state", "formal"); // formal：正式版，trial：体验版，developer：开发版

            // 3. 消息数据
            Map<String, Object> data = new HashMap<>();
            data.put("thing1", createDataItem(title)); // 提醒标题
            data.put("thing2", createDataItem(petName)); // 宠物名称
            data.put("time3", createDataItem(formatDateTime(remindTime))); // 提醒时间

            message.put("data", data);

            // 4. 发送请求
            String url = String.format(SEND_MESSAGE_URL, accessToken);
            String response = HttpUtil.post(url, JSONUtil.toJsonStr(message));
            
            log.info("发送订阅消息响应：{}", response);

            JSONObject result = JSONUtil.parseObj(response);
            int errcode = result.getInt("errcode", -1);
            
            if (errcode == 0) {
                log.info("发送提醒订阅消息成功");
                return true;
            } else {
                log.error("发送提醒订阅消息失败，errcode: {}, errmsg: {}", 
                         errcode, result.getStr("errmsg"));
                return false;
            }
        } catch (Exception e) {
            log.error("发送提醒订阅消息异常", e);
            return false;
        }
    }

    /**
     * 获取 Access Token
     */
    private String getAccessToken() {
        // 1. 从 Redis 获取
        String accessToken = stringRedisTemplate.opsForValue().get(REDIS_KEY_ACCESS_TOKEN);
        if (StrUtil.isNotBlank(accessToken)) {
            return accessToken;
        }

        // 2. 从微信服务器获取
        try {
            String url = String.format(ACCESS_TOKEN_URL, appid, secret);
            String response = HttpUtil.get(url);
            
            log.info("获取 Access Token 响应：{}", response);

            JSONObject result = JSONUtil.parseObj(response);
            accessToken = result.getStr("access_token");
            Integer expiresIn = result.getInt("expires_in", 7200);

            if (StrUtil.isNotBlank(accessToken)) {
                // 提前5分钟过期
                stringRedisTemplate.opsForValue().set(
                    REDIS_KEY_ACCESS_TOKEN, 
                    accessToken, 
                    Duration.ofSeconds(expiresIn - 300)
                );
                return accessToken;
            } else {
                log.error("获取 Access Token 失败：{}", response);
                return null;
            }
        } catch (Exception e) {
            log.error("获取 Access Token 异常", e);
            return null;
        }
    }

    /**
     * 创建消息数据项
     */
    private Map<String, String> createDataItem(String value) {
        Map<String, String> item = new HashMap<>();
        item.put("value", value);
        return item;
    }

    /**
     * 格式化日期时间
     */
    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(DATE_TIME_FORMATTER);
    }
}
