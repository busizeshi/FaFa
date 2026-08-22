package com.fafa.application.service;

import cn.dev33.satoken.stp.StpUtil;
import com.fafa.application.dto.LoginCommand;
import com.fafa.application.dto.LoginResult;
import com.fafa.common.BusinessException;
import com.fafa.common.ErrorCode;
import com.fafa.domain.model.user.User;
import com.fafa.domain.repository.UserRepository;
import com.fafa.infrastructure.wechat.WeChatClient;
import com.fafa.infrastructure.wechat.WeChatSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 认证应用服务
 *
 * 负责协调微信登录流程：code 换取 openid → 查找或注册用户 → 建立 Sa-Token 会话。
 *
 * @author FaFa Team
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthAppService {

    private final WeChatClient weChatClient;
    private final UserRepository userRepository;

    /**
     * 微信一键登录
     *
     * 业务规则：
     * 1. code 只能使用一次，失效则要求小程序重新 wx.login()
     * 2. 首次登录自动注册，不强制填写资料
     * 3. 登录成功后返回 Sa-Token，小程序存入本地并在后续请求携带
     */
    @Transactional
    public LoginResult login(LoginCommand command) {
        // 1. code 换取 openid
        WeChatSession session = weChatClient.code2Session(command.code());

        // 2. 查找用户，不存在则注册
        User user = userRepository.findByOpenId(session.openid()).orElse(null);
        boolean newUser = false;
        if (user == null) {
            user = User.register(session.openid());
            userRepository.save(user);
            newUser = true;
            log.info("新用户注册: userId={}", user.getId());
        }

        // 3. 建立 Sa-Token 会话
        StpUtil.login(user.getId());
        log.info("用户登录成功: userId={}, newUser={}", user.getId(), newUser);
        return new LoginResult(StpUtil.getTokenValue(), user.getId(), newUser);
    }

    /**
     * 登出
     */
    public void logout() {
        long userId = StpUtil.getLoginIdAsLong();
        StpUtil.logout();
        log.info("用户登出: userId={}", userId);
    }

    /**
     * 获取当前登录用户 ID，未登录抛出业务异常
     */
    public long currentUserId() {
        try {
            return StpUtil.getLoginIdAsLong();
        } catch (Exception ex) {
            log.warn("获取当前用户失败，未登录");
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }
}
