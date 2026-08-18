package com.fafa.infrastructure.persistence.converter;

import com.fafa.domain.model.user.User;
import com.fafa.domain.model.user.UserId;
import com.fafa.domain.model.user.UserStatus;
import com.fafa.infrastructure.persistence.dataobject.UserDO;
import org.springframework.stereotype.Component;

/**
 * 用户转换器
 * 
 * @author FaFa Team
 * @since 1.0
 */
@Component
public class UserConverter {
    
    /**
     * DO 转 领域模型
     */
    public User toDomain(UserDO userDO) {
        if (userDO == null) {
            return null;
        }
        
        return User.builder()
                .userId(UserId.of(userDO.getId()))
                .openid(userDO.getOpenid())
                .unionid(userDO.getUnionid())
                .nickname(userDO.getNickname())
                .avatar(userDO.getAvatar())
                .phone(userDO.getPhone())
                .gender(userDO.getGender())
                .status(UserStatus.fromCode(userDO.getStatus()))
                .registerDate(userDO.getRegisterDate())
                .lastLoginAt(userDO.getLastLoginAt())
                .createdAt(userDO.getCreatedAt())
                .updatedAt(userDO.getUpdatedAt())
                .build();
    }
    
    /**
     * 领域模型 转 DO
     */
    public UserDO toDO(User user) {
        if (user == null) {
            return null;
        }
        
        return UserDO.builder()
                .id(user.getUserId() != null ? user.getUserId().getValue() : null)
                .openid(user.getOpenid())
                .unionid(user.getUnionid())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .phone(user.getPhone())
                .gender(user.getGender())
                .status(user.getStatus() != null ? user.getStatus().getCode() : null)
                .registerDate(user.getRegisterDate())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
