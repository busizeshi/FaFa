package com.fafa.interfaces.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fafa.application.service.AuthApplicationService;
import com.fafa.common.result.Result;
import com.fafa.domain.model.user.User;
import com.fafa.interfaces.dto.request.WechatLoginRequest;
import com.fafa.interfaces.dto.response.LoginResponse;
import com.fafa.interfaces.dto.response.UserInfoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 * 
 * @author FaFa Team
 * @since 1.0
 */
@Slf4j
@Tag(name = "用户认证", description = "用户认证相关接口")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthApplicationService authApplicationService;
    
    @Operation(summary = "微信小程序登录", description = "使用微信 code 登录")
    @PostMapping("/wechat/login")
    public Result<LoginResponse> wechatLogin(@RequestBody WechatLoginRequest request) {
        AuthApplicationService.LoginResult loginResult = authApplicationService.wechatLogin(request.getCode());
        
        LoginResponse response = LoginResponse.builder()
                .token(loginResult.getToken())
                .userId(loginResult.getUserId())
                .nickname(loginResult.getNickname())
                .avatar(loginResult.getAvatar())
                .isNewUser(loginResult.getIsNewUser())
                .build();
        
        return Result.success(response);
    }
    
    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户信息")
    @GetMapping("/user/info")
    public Result<UserInfoResponse> getUserInfo() {
        User user = authApplicationService.getCurrentUser();
        
        UserInfoResponse response = UserInfoResponse.builder()
                .userId(user.getUserId().getValue())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .phone(user.getPhone())
                .gender(user.getGender())
                .registerDate(user.getRegisterDate())
                .build();
        
        return Result.success(response);
    }
    
    @Operation(summary = "登出", description = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout() {
        authApplicationService.logout();
        return Result.success();
    }
    
    @Operation(summary = "检查登录状态", description = "检查是否已登录")
    @GetMapping("/check")
    public Result<Boolean> checkLogin() {
        boolean isLogin = StpUtil.isLogin();
        return Result.success(isLogin);
    }
}
