package com.fafa.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fafa.domain.model.user.User;
import com.fafa.domain.repository.UserRepository;
import com.fafa.infrastructure.persistence.converter.UserConverter;
import com.fafa.infrastructure.persistence.dataobject.UserDO;
import com.fafa.infrastructure.persistence.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户仓储实现
 */
@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserMapper userMapper;

    @Override
    public Optional<User> findByOpenid(String openid) {
        UserDO userDO = userMapper.selectOne(
                new LambdaQueryWrapper<UserDO>().eq(UserDO::getOpenid, openid));
        return Optional.ofNullable(userDO).map(UserConverter::toDomain);
    }

    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(userMapper.selectById(id))
                .map(UserConverter::toDomain);
    }

    @Override
    public User save(User user) {
        UserDO userDO = UserConverter.toDO(user);
        if (userDO.getId() == null) {
            userMapper.insert(userDO);
        } else {
            userMapper.updateById(userDO);
        }
        return UserConverter.toDomain(userDO);
    }
}
