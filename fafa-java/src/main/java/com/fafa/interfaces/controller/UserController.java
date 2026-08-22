package com.fafa.interfaces.controller;

import com.fafa.application.dto.user.UpdateProfileCommand;
import com.fafa.application.dto.user.UserInfoDTO;
import com.fafa.application.dto.user.VipInfoDTO;
import com.fafa.application.service.UserApplicationService;
import com.fafa.common.Result;
import com.fafa.interfaces.assembler.UserAssembler;
import com.fafa.interfaces.dto.user.UpdateProfileRequest;
import com.fafa.interfaces.dto.user.UserInfoResponse;
import com.fafa.interfaces.dto.user.VipInfoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户控制器
 *
 * @author FaFa Team
 * @since 1.0
 */
@Tag(name = "用户")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserApplicationService userApplicationService;

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/me")
    public Result<UserInfoResponse> getCurrentUserInfo() {
        UserInfoDTO dto = userApplicationService.getCurrentUserInfo();
        return Result.ok(UserAssembler.toUserInfoResponse(dto));
    }

    @Operation(summary = "更新用户资料", description = "更新昵称、手机号、性别、年龄、城市等基本信息")
    @PutMapping("/profile")
    public Result<UserInfoResponse> updateProfile(@RequestBody @Valid UpdateProfileRequest request) {
        UpdateProfileCommand command = new UpdateProfileCommand(
            request.getNickname(),
            request.getPhone(),
            request.getGender(),
            request.getAge(),
            request.getCity()
        );
        UserInfoDTO dto = userApplicationService.updateProfile(command);
        return Result.ok(UserAssembler.toUserInfoResponse(dto));
    }

    @Operation(summary = "上传头像", description = "上传用户头像图片")
    @PostMapping("/avatar")
    public Result<UserInfoResponse> uploadAvatar(@RequestParam("file") MultipartFile file) {
        UserInfoDTO dto = userApplicationService.uploadAvatar(file);
        return Result.ok(UserAssembler.toUserInfoResponse(dto));
    }

    @Operation(summary = "获取会员信息")
    @GetMapping("/vip")
    public Result<VipInfoResponse> getVipInfo() {
        VipInfoDTO dto = userApplicationService.getVipInfo();
        return Result.ok(UserAssembler.toVipInfoResponse(dto));
    }

    @Operation(summary = "升级为会员", description = "开通或续费会员")
    @PostMapping("/vip/upgrade")
    public Result<UserInfoResponse> upgradeToVip(@RequestParam("days") int days) {
        UserInfoDTO dto = userApplicationService.upgradeToVip(days);
        return Result.ok(UserAssembler.toUserInfoResponse(dto));
    }

    @Operation(summary = "续费会员")
    @PostMapping("/vip/renew")
    public Result<UserInfoResponse> renewVip(@RequestParam("days") int days) {
        UserInfoDTO dto = userApplicationService.renewVip(days);
        return Result.ok(UserAssembler.toUserInfoResponse(dto));
    }
}
