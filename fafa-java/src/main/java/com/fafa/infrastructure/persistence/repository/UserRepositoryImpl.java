package com.fafa.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fafa.domain.model.user.User;
import com.fafa.domain.model.user.UserId;
import com.fafa.domain.repository.UserRepository;
import com.fafa.infrastructure.persistence.converter.UserConverter;
import com.fafa.infrastructure.persistence.dataobject.UserDO;
import com.fafa.infrastructure.persistence.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户仓储实现
 * 
 * @author FaFa Team
 * @since 1.0
 */
@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {
    
    private final UserMapper userMapper;
    private final UserConverter userConverter;
    
    @Override
    public Optional<User> findById(UserId userId) {
        UserDO userDO = userMapper.selectById(userId.getValue());
        return Optional.ofNullable(userConverter.toDomain(userDO));
    }
    
    @Override
    public Optional<User> findByOpenid(String openid) {
        LambdaQueryWrapper<UserDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserDO::getOpenid, openid);
        UserDO userDO = userMapper.selectOne(wrapper);
        return Optional.ofNullable(userConverter.toDomain(userDO));
    }
    
    @Override
    public User save(User user) {
        UserDO userDO = userConverter.toDO(user);
        userMapper.insert(userDO);
        return userConverter.toDomain(userDO);
    }
    
    @Override
    public void update(User user) {
        UserDO userDO = userConverter.toDO(user);
        userMapper.updateById(userDO);
    }
    
    @Override
    public void delete(Long userId) {
        userMapper.deleteById(userId);
    }
}
