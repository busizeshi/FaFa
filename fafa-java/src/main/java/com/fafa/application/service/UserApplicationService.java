package com.fafa.application.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.fafa.common.exception.BusinessException;
import com.fafa.domain.model.pet.Pet;
import com.fafa.domain.model.pet.PetId;
import com.fafa.domain.model.photo.Photo;
import com.fafa.domain.model.user.User;
import com.fafa.domain.model.user.UserId;
import com.fafa.domain.repository.*;
import com.fafa.infrastructure.oss.OssService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 用户应用服务
 * 
 * @author FaFa Team
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserApplicationService {
    
    private final UserRepository userRepository;
    private final PetRepository petRepository;
    private final PhotoRepository photoRepository;
    private final FeedRecordRepository feedRecordRepository;
    private final WaterRecordRepository waterRecordRepository;
    private final ExcretionRecordRepository excretionRecordRepository;
    private final WeightRecordRepository weightRecordRepository;
    private final EventRecordRepository eventRecordRepository;
    private final ReminderRepository reminderRepository;
    private final OssService ossService;
    
    /**
     * 更新用户信息
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateUserInfo(String nickname, String phone, Integer gender) {
        long userId = StpUtil.getLoginIdAsLong();
        User user = userRepository.findById(UserId.of(userId))
                .orElseThrow(() -> new BusinessException("用户不存在"));
        
        // 更新昵称
        if (StrUtil.isNotBlank(nickname)) {
            user.setNickname(nickname.trim());
        }
        
        // 更新手机号
        if (StrUtil.isNotBlank(phone)) {
            user.setPhone(phone.trim());
        }
        
        // 更新性别
        if (gender != null && gender >= 0 && gender <= 2) {
            user.setGender(gender);
        }
        
        userRepository.update(user);
        
        log.info("更新用户信息成功，userId={}", userId);
    }
    
    /**
     * 上传用户头像
     */
    @Transactional(rollbackFor = Exception.class)
    public String uploadAvatar(MultipartFile file) {
        long userId = StpUtil.getLoginIdAsLong();
        User user = userRepository.findById(UserId.of(userId))
                .orElseThrow(() -> new BusinessException("用户不存在"));
        
        // 验证文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("只能上传图片文件");
        }
        
        // 验证文件大小（最大 5MB）
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new BusinessException("图片大小不能超过 5MB");
        }
        
        // 删除旧头像（如果存在）
        String oldAvatar = user.getAvatar();
        if (StrUtil.isNotBlank(oldAvatar)) {
            try {
                // 从 URL 中提取文件名
                String oldFileName = extractFileNameFromUrl(oldAvatar);
                if (StrUtil.isNotBlank(oldFileName)) {
                    ossService.deleteFile(oldFileName);
                    log.info("删除旧头像成功，userId={}, fileName={}", userId, oldFileName);
                }
            } catch (Exception e) {
                log.warn("删除旧头像失败，userId={}, error={}", userId, e.getMessage());
                // 不影响主流程，继续上传新头像
            }
        }
        
        // 上传新头像
        String avatarUrl = ossService.uploadFile(file, "avatars");
        
        // 更新用户头像
        user.setAvatar(avatarUrl);
        userRepository.update(user);
        
        log.info("上传用户头像成功，userId={}, avatarUrl={}", userId, avatarUrl);
        
        return avatarUrl;
    }
    
    /**
     * 从 MinIO 预签名 URL 中提取文件名
     * 例如：http://192.168.1.14:9000/fafa/avatars/202608/abc123.jpg?...
     * 提取：avatars/202608/abc123.jpg
     */
    private String extractFileNameFromUrl(String url) {
        if (StrUtil.isBlank(url)) {
            return null;
        }
        
        try {
            // 找到 bucketName 之后的路径部分
            String bucketName = "fafa"; // 可以从配置读取
            int bucketIndex = url.indexOf("/" + bucketName + "/");
            if (bucketIndex == -1) {
                return null;
            }
            
            // 提取文件路径（去掉查询参数）
            String path = url.substring(bucketIndex + bucketName.length() + 2);
            int queryIndex = path.indexOf("?");
            if (queryIndex > 0) {
                path = path.substring(0, queryIndex);
            }
            
            return path;
        } catch (Exception e) {
            log.warn("提取文件名失败，url={}, error={}", url, e.getMessage());
            return null;
        }
    }
    
    /**
     * 注销用户（进入冷静期）
     * 用户进入待注销状态，7天后才真正删除
     * 在此期间用户可以随时恢复账号
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser() {
        long userId = StpUtil.getLoginIdAsLong();
        User user = userRepository.findById(UserId.of(userId))
                .orElseThrow(() -> new BusinessException("用户不存在"));
        
        log.info("用户请求注销账号，进入冷静期，userId={}", userId);
        
        // 设置为待注销状态
        user.requestDeletion();
        userRepository.update(user);
        
        // 登出
        StpUtil.logout(userId);
        
        log.info("用户已进入注销冷静期（7天），userId={}", userId);
    }
    
    /**
     * 取消注销（恢复账号）
     * 用户在冷静期内可以随时恢复账号
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelDeletion() {
        long userId = StpUtil.getLoginIdAsLong();
        User user = userRepository.findById(UserId.of(userId))
                .orElseThrow(() -> new BusinessException("用户不存在"));
        
        if (!user.isPendingDeletion()) {
            throw new BusinessException("账号未处于待注销状态");
        }
        
        log.info("用户取消注销，恢复账号，userId={}", userId);
        
        // 恢复为正常状态
        user.cancelDeletion();
        userRepository.update(user);
        
        log.info("账号已恢复正常，userId={}", userId);
    }
}
