package com.fafa.biz.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fafa.biz.domain.model.User;
import com.fafa.biz.domain.repository.UserRepositoryPort;
import com.fafa.biz.domain.UserStatus;
import com.fafa.biz.infrastructure.persistence.entity.UserDO;
import com.fafa.biz.infrastructure.persistence.mapper.UserMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 宠主账号仓储适配器：User 领域对象 ↔ UserDO 显式映射。
 *
 * @author FaFa 项目组
 * @version 1.0.0
 */
@Component
public class UserRepositoryAdapter implements UserRepositoryPort {

    private final UserMapper userMapper;

    /**
     * @param userMapper MyBatis-Plus Mapper
     */
    public UserRepositoryAdapter(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /** {@inheritDoc} */
    @Override
    public User save(User user) {
        UserDO userDO = toDO(user);
        if (userDO.getId() == null) {
            try {
                userMapper.insert(userDO);
            } catch (DuplicateKeyException e) {
                // 唯一约束兜底并发重复注册：由应用层转换为稳定业务错误
                throw e;
            }
        } else {
            userMapper.updateById(userDO);
        }
        return toDomain(userDO);
    }

    /** {@inheritDoc} */
    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(userMapper.selectById(id)).map(this::toDomain);
    }

    /** {@inheritDoc} */
    @Override
    public Optional<User> findByEmail(String email) {
        LambdaQueryWrapper<UserDO> wrapper = new LambdaQueryWrapper<UserDO>()
                .eq(UserDO::getEmail, email);
        return Optional.ofNullable(userMapper.selectOne(wrapper)).map(this::toDomain);
    }

    /**
     * 领域对象 → DO。
     */
    private UserDO toDO(User user) {
        UserDO userDO = new UserDO();
        userDO.setId(user.getId());
        userDO.setEmail(user.getEmail());
        userDO.setPasswordHash(user.getPasswordHash());
        userDO.setDisplayName(user.getDisplayName());
        userDO.setStatus(user.getStatus() == null ? null : user.getStatus().name());
        userDO.setCreatedAt(user.getCreatedAt());
        userDO.setUpdatedAt(user.getUpdatedAt());
        return userDO;
    }

    /**
     * DO → 领域对象。
     */
    private User toDomain(UserDO userDO) {
        User user = new User();
        user.setId(userDO.getId());
        user.setEmail(userDO.getEmail());
        user.setPasswordHash(userDO.getPasswordHash());
        user.setDisplayName(userDO.getDisplayName());
        user.setStatus(userDO.getStatus() == null ? null : UserStatus.valueOf(userDO.getStatus()));
        user.setCreatedAt(userDO.getCreatedAt());
        user.setUpdatedAt(userDO.getUpdatedAt());
        return user;
    }
}
