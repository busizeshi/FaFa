package com.fafa.application.service;

import cn.dev33.satoken.stp.StpUtil;
import com.fafa.common.exception.BusinessException;
import com.fafa.domain.model.user.User;
import com.fafa.domain.model.user.UserId;
import com.fafa.domain.model.user.UserStatus;
import com.fafa.domain.repository.UserRepository;
import com.fafa.infrastructure.wechat.WechatMiniAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 用户认证应用服务
 * 
 * @author FaFa Team
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthApplicationService {
    
    private final UserRepository userRepository;
    private final WechatMiniAppService wechatMiniAppService;
    
    /**
     * 微信小程序登录
     * 
     * @param code 微信登录凭证
     * @return LoginResult
     */
    @Transactional(rollbackFor = Exception.class)
    public LoginResult wechatLogin(String code) {
        // 1. 调用微信接口，获取 openid
        WechatMiniAppService.Code2SessionResult sessionResult = wechatMiniAppService.code2Session(code);
        String openid = sessionResult.getOpenid();
        String unionid = sessionResult.getUnionid();
        
        log.info("微信登录: openid={}, unionid={}", openid, unionid);
        
        // 2. 查询用户是否存在
        Optional<User> userOpt = userRepository.findByOpenid(openid);
        
        User user;
        boolean isNewUser;
        
        if (userOpt.isPresent()) {
            // 已存在，更新最后登录时间
            user = userOpt.get();
            user.updateLastLogin();
            userRepository.update(user);
            isNewUser = false;
        } else {
            // 新用户，创建用户
            user = User.builder()
                    .openid(openid)
                    .unionid(unionid)
                    .status(UserStatus.NORMAL)
                    .registerDate(LocalDate.now())
                    .lastLoginAt(LocalDateTime.now())
                    .build();
            user = userRepository.save(user);
            isNewUser = true;
            log.info("新用户注册: userId={}, openid={}", user.getUserId().getValue(), openid);
        }
        
        // 3. 检查用户状态
        if (!user.isNormal()) {
            throw new BusinessException("账号已被禁用，请联系客服");
        }
        
        // 4. Sa-Token 登录
        StpUtil.login(user.getUserId().getValue());
        String token = StpUtil.getTokenValue();
        
        log.info("用户登录成功: userId={}, token={}", user.getUserId().getValue(), token);
        
        return LoginResult.builder()
                .token(token)
                .userId(user.getUserId().getValue())
                .openid(user.getOpenid())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .isNewUser(isNewUser)
                .build();
    }
    
    /**
     * 获取当前登录用户
     */
    public User getCurrentUser() {
        long userId = StpUtil.getLoginIdAsLong();
        return userRepository.findById(UserId.of(userId))
                .orElseThrow(() -> new BusinessException("用户不存在"));
    }
    
    /**
     * 登出
     */
    public void logout() {
        StpUtil.logout();
    }
    
    /**
     * 登录结果
     */
    @lombok.Data
    @lombok.Builder
    public static class LoginResult {
        private String token;
        private Long userId;
        private String openid;
        private String nickname;
        private String avatar;
        private Boolean isNewUser;
    }
}
