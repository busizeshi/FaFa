package com.fafa.domain.repository;

import com.fafa.domain.model.user.User;

import java.util.Optional;

/**
 * 用户仓储接口（领域层定义，基础设施层实现）
 */
public interface UserRepository {

    Optional<User> findByOpenid(String openid);

    Optional<User> findById(Long id);

    User save(User user);
}
