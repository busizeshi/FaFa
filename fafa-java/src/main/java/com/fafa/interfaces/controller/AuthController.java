package com.fafa.interfaces.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fafa.application.service.AuthApplicationService;
import com.fafa.interfaces.common.Result;
import com.fafa.interfaces.dto.auth.LoginResponse;
import com.fafa.interfaces.dto.auth.UserInfoResponse;
import com.fafa.interfaces.dto.auth.WechatLoginRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口
 */
@Tag(name = "认证")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthApplicationService authApplicationService;

    @Operation(summary = "微信登录（自动注册）")
    @PostMapping("/wechat/login")
    public Result<LoginResponse> wechatLogin(@RequestBody @Valid WechatLoginRequest request) {
        return Result.ok(authApplicationService.wechatLogin(request.getCode()));
    }

    @Operation(summary = "当前用户信息")
    @GetMapping("/user/info")
    public Result<UserInfoResponse> userInfo() {
        return Result.ok(authApplicationService.getCurrentUserInfo());
    }

    @Operation(summary = "登出")
    @PostMapping("/logout")
    public Result<Void> logout() {
        authApplicationService.logout();
        return Result.ok();
    }

    @Operation(summary = "登录状态检查")
    @GetMapping("/check")
    public Result<Boolean> checkLogin() {
        return Result.ok(StpUtil.isLogin());
    }
}
