package com.fafa.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fafa.domain.model.user.User;
import com.fafa.domain.repository.UserRepository;
import com.fafa.infrastructure.persistence.mapper.UserMapper;
import com.fafa.infrastructure.persistence.po.UserPO;
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

    @Override
    public User save(User user) {
        UserPO po = toUserPO(user);
        
        if (po.getId() == null) {
            userMapper.insert(po);
            po = userMapper.selectById(po.getId());
        } else {
            userMapper.updateById(po);
        }
        
        return toDomain(po);
    }

    @Override
    public Optional<User> findById(Long userId) {
        UserPO po = userMapper.selectById(userId);
        return Optional.ofNullable(toDomain(po));
    }

    @Override
    public Optional<User> findByOpenId(String openId) {
        LambdaQueryWrapper<UserPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPO::getOpenId, openId);
        UserPO po = userMapper.selectOne(wrapper);
        return Optional.ofNullable(toDomain(po));
    }

    /**
     * 持久化对象转领域对象
     */
    private User toDomain(UserPO po) {
        if (po == null) {
            return null;
        }

        return User.restore(
            po.getId(),
            po.getOpenId(),
            po.getNickname(),
            po.getPhone(),
            po.getGender(),
            po.getAge(),
            po.getAvatarUrl(),
            po.getCity(),
            po.getIsVip() != null && po.getIsVip(),
            po.getVipExpireTime(),
            po.getCreatedAt(),
            po.getUpdatedAt()
        );
    }

    /**
     * 领域对象转持久化对象
     */
    private UserPO toUserPO(User user) {
        if (user == null) {
            return null;
        }

        UserPO po = new UserPO();
        po.setId(user.getId());
        po.setOpenId(user.getOpenId());
        po.setNickname(user.getNickname());
        po.setPhone(user.getPhone());
        po.setGender(user.getGender());
        po.setAge(user.getAge());
        po.setAvatarUrl(user.getAvatarUrl());
        po.setCity(user.getCity());
        po.setIsVip(user.isVip());
        po.setVipExpireTime(user.getVipExpireTime());

        return po;
    }
}
