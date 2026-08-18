package com.fafa.domain.repository;

import com.fafa.domain.model.user.User;
import com.fafa.domain.model.user.UserId;

import java.util.Optional;

/**
 * 用户仓储接口
 * 
 * @author FaFa Team
 * @since 1.0
 */
public interface UserRepository {
    
    /**
     * 根据 ID 查询用户
     */
    Optional<User> findById(UserId userId);
    
    /**
     * 根据 OpenID 查询用户
     */
    Optional<User> findByOpenid(String openid);
    
    /**
     * 保存用户
     */
    User save(User user);
    
    /**
     * 更新用户
     */
    void update(User user);
}
