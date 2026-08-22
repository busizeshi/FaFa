package com.fafa.domain.repository;

import com.fafa.domain.model.user.User;

import java.util.Optional;

/**
 * 用户仓储接口
 *
 * @author FaFa Team
 * @since 1.0
 */
public interface UserRepository {

    /** 按微信 openid 查找用户 */
    Optional<User> findByOpenId(String openId);

    /** 根据用户ID查找用户 */
    Optional<User> findById(Long userId);

    /** 保存用户（新增或更新），新增时回填雪花 ID */
    User save(User user);
}
