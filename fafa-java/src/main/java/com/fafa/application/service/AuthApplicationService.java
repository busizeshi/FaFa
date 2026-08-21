package com.fafa.application.service;

import cn.dev33.satoken.stp.StpUtil;
import com.fafa.domain.model.user.User;
import com.fafa.domain.repository.UserRepository;
import com.fafa.infrastructure.wechat.WeChatClient;
import com.fafa.interfaces.dto.auth.LoginResponse;
import com.fafa.interfaces.dto.auth.UserInfoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 认证应用服务
 *
 * 编排微信登录：code 换 openid -> 查/建用户 -> Sa-Token 登录
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthApplicationService {

    private final WeChatClient weChatClient;
    private final UserRepository userRepository;

    /**
     * 微信登录（不存在则自动注册）
     */
    @Transactional
    public LoginResponse wechatLogin(String code) {
        String openid = weChatClient.code2Session(code);

        boolean isNewUser = false;
        User user = userRepository.findByOpenid(openid).orElse(null);
        if (user == null) {
            user = userRepository.save(User.register(openid));
            isNewUser = true;
            log.info("新用户注册: userId={}", user.getId());
        }

        // Sa-Token 登录，token 会话存 Redis
        StpUtil.login(user.getId());
        log.info("用户登录成功: userId={}, isNewUser={}", user.getId(), isNewUser);

        return new LoginResponse(StpUtil.getTokenValue(), user.getId(), isNewUser);
    }

    /**
     * 查询当前登录用户信息
     */
    public UserInfoResponse getCurrentUserInfo() {
        long userId = StpUtil.getLoginIdAsLong();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new com.fafa.domain.exception.BusinessException(
                        com.fafa.domain.common.ErrorCode.USER_NOT_FOUND));

        UserInfoResponse response = new UserInfoResponse();
        response.setUserId(user.getId());
        response.setNickname(user.getNickname());
        response.setAvatarUrl(user.getAvatarUrl());
        response.setPhone(user.getPhone());
        response.setGender(user.getGender());
        response.setCity(user.getCity());
        response.setMemberLevel(user.getMemberLevel());
        return response;
    }

    /**
     * 登出
     */
    public void logout() {
        StpUtil.logout();
        log.info("用户登出: userId={}", StpUtil.getLoginIdDefaultNull());
    }
}
