package com.fafa.interfaces.controller;

import com.fafa.application.dto.LoginCommand;
import com.fafa.application.dto.LoginResult;
import com.fafa.application.service.AuthAppService;
import com.fafa.common.Result;
import com.fafa.interfaces.assembler.AuthAssembler;
import com.fafa.interfaces.dto.auth.LoginRequest;
import com.fafa.interfaces.dto.auth.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器
 *
 * @author FaFa Team
 * @since 1.0
 */
@Tag(name = "认证")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthAppService authAppService;

    @Operation(summary = "微信一键登录", description = "小程序 wx.login() 获取 code 后调用，首次登录自动注册")
    @PostMapping("/wechat/login")
    public Result<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        LoginResult result = authAppService.login(new LoginCommand(request.getCode()));
        return Result.ok(AuthAssembler.toResponse(result));
    }

    @Operation(summary = "登出")
    @PostMapping("/logout")
    public Result<Void> logout() {
        authAppService.logout();
        return Result.ok();
    }
}
