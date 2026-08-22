package com.fafa.application.service;

import com.fafa.application.dto.user.*;
import com.fafa.common.BusinessException;
import com.fafa.common.ErrorCode;
import com.fafa.domain.model.user.User;
import com.fafa.domain.repository.UserRepository;
import com.fafa.infrastructure.oss.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

/**
 * 用户应用服务
 *
 * 负责协调用户资料管理、会员管理等业务流程
 *
 * 业务规则：
 * 1. 用户资料可多次修改
 * 2. 头像上传到对象存储（MinIO/OSS）
 * 3. 会员到期自动降级为普通用户
 *
 * @author FaFa Team
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserApplicationService {

    private final UserRepository userRepository;
    private final StorageService storageService;
    private final AuthAppService authAppService;

    /**
     * 获取当前用户信息
     */
    public UserInfoDTO getCurrentUserInfo() {
        long userId = authAppService.currentUserId();
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        // 检查会员是否过期
        user.expireVip();
        if (user.isVip() && !user.isVipValid()) {
            userRepository.save(user);
        }
        
        return toUserInfoDTO(user);
    }

    /**
     * 更新用户基本资料
     *
     * 业务规则：
     * - 昵称、手机号可多次修改
     * - 性别、年龄、城市可选填
     */
    @Transactional
    public UserInfoDTO updateProfile(UpdateProfileCommand command) {
        long userId = authAppService.currentUserId();
        
        log.info("更新用户资料: userId={}, command={}", userId, command);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        user.updateProfile(
            command.nickname(),
            command.phone(),
            command.gender(),
            command.age(),
            command.city()
        );
        
        userRepository.save(user);
        
        log.info("用户资料更新成功: userId={}", userId);
        
        return toUserInfoDTO(user);
    }

    /**
     * 上传并更新用户头像
     *
     * 业务规则：
     * - 头像存储到对象存储
     * - 返回可访问的 URL
     */
    @Transactional
    public UserInfoDTO uploadAvatar(MultipartFile file) {
        long userId = authAppService.currentUserId();
        
        log.info("上传用户头像: userId={}, filename={}", userId, file.getOriginalFilename());
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        // 上传头像到对象存储
        String avatarUrl = storageService.upload(file, "avatar/user/" + userId);
        
        user.updateAvatar(avatarUrl);
        userRepository.save(user);
        
        log.info("用户头像上传成功: userId={}, avatarUrl={}", userId, avatarUrl);
        
        return toUserInfoDTO(user);
    }

    /**
     * 升级为会员
     *
     * 业务规则：
     * - 会员有效期从当前时间开始计算
     * - 如果已是会员，则延长会员时间
     *
     * @param days 会员天数
     */
    @Transactional
    public UserInfoDTO upgradeToVip(int days) {
        long userId = authAppService.currentUserId();
        
        log.info("用户升级会员: userId={}, days={}", userId, days);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        if (user.isVipValid()) {
            // 已是会员，续费
            user.renewVip(days);
        } else {
            // 新开通会员
            LocalDateTime expireTime = LocalDateTime.now().plusDays(days);
            user.upgradeToVip(expireTime);
        }
        
        userRepository.save(user);
        
        log.info("用户会员升级成功: userId={}, vipExpireTime={}", 
                userId, user.getVipExpireTime());
        
        return toUserInfoDTO(user);
    }

    /**
     * 续费会员
     *
     * @param days 续费天数
     */
    @Transactional
    public UserInfoDTO renewVip(int days) {
        long userId = authAppService.currentUserId();
        
        log.info("用户续费会员: userId={}, days={}", userId, days);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        user.renewVip(days);
        userRepository.save(user);
        
        log.info("用户会员续费成功: userId={}, vipExpireTime={}", 
                userId, user.getVipExpireTime());
        
        return toUserInfoDTO(user);
    }

    /**
     * 获取会员信息
     */
    public VipInfoDTO getVipInfo() {
        long userId = authAppService.currentUserId();
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        // 检查会员是否过期
        user.expireVip();
        if (user.isVip() && !user.isVipValid()) {
            userRepository.save(user);
        }
        
        return new VipInfoDTO(
            user.isVip(),
            user.isVipValid(),
            user.getVipExpireTime()
        );
    }

    /**
     * 领域对象转换为 DTO
     */
    private UserInfoDTO toUserInfoDTO(User user) {
        return new UserInfoDTO(
            user.getId(),
            user.getOpenId(),
            user.getNickname(),
            user.getPhone(),
            user.getGender(),
            user.getAge(),
            user.getAvatarUrl(),
            user.getCity(),
            user.isVip(),
            user.getVipExpireTime(),
            user.getDisplayName()
        );
    }
}
