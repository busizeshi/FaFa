package com.fafa.interfaces.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fafa.application.service.UserApplicationService;
import com.fafa.common.result.Result;
import com.fafa.interfaces.dto.request.UpdateUserInfoRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户管理控制器
 * 
 * @author FaFa Team
 * @since 1.0
 */
@Slf4j
@Tag(name = "用户管理", description = "用户信息管理相关接口")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    
    private final UserApplicationService userApplicationService;
    
    @Operation(summary = "更新用户信息", description = "更新当前登录用户的基础信息（昵称、手机号、性别）")
    @PutMapping("/info")
    public Result<Void> updateUserInfo(@Valid @RequestBody UpdateUserInfoRequest request) {
        userApplicationService.updateUserInfo(
                request.getNickname(),
                request.getPhone(),
                request.getGender()
        );
        return Result.success();
    }
    
    @Operation(summary = "上传用户头像", description = "上传用户头像，会自动删除旧头像")
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        String avatarUrl = userApplicationService.uploadAvatar(file);
        return Result.success(avatarUrl);
    }
}
