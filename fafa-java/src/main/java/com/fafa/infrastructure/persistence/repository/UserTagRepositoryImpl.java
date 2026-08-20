package com.fafa.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fafa.domain.model.tag.UserTag;
import com.fafa.domain.model.tag.UserTagId;
import com.fafa.domain.repository.UserTagRepository;
import com.fafa.infrastructure.persistence.converter.UserTagConverter;
import com.fafa.infrastructure.persistence.dataobject.UserTagDO;
import com.fafa.infrastructure.persistence.mapper.UserTagMapper;
import org.springframework.stereotype.Repository;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 用户标签仓储实现
 *
 * @author FaFa
 * @since 2026-08-20
 */
@Repository
public class UserTagRepositoryImpl implements UserTagRepository {

    @Resource
    private UserTagMapper userTagMapper;

    @Override
    public UserTag save(UserTag userTag) {
        UserTagDO userTagDO = UserTagConverter.toDO(userTag);
        
        if (userTagDO.getId() == null) {
            userTagMapper.insert(userTagDO);
        } else {
            userTagMapper.updateById(userTagDO);
        }
        
        return UserTagConverter.toDomain(userTagMapper.selectById(userTagDO.getId()));
    }

    @Override
    public Optional<UserTag> findById(UserTagId id) {
        UserTagDO userTagDO = userTagMapper.selectById(id.getValue());
        return Optional.ofNullable(UserTagConverter.toDomain(userTagDO));
    }

    @Override
    public List<UserTag> findByUserId(Long userId) {
        List<UserTagDO> userTagDOList = userTagMapper.selectByUserId(userId);
        return userTagDOList.stream()
                .map(UserTagConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserTag> findByUserIdOrderByUsageCountDesc(Long userId, int limit) {
        List<UserTagDO> userTagDOList = userTagMapper.selectByUserIdOrderByUsage(userId, limit);
        return userTagDOList.stream()
                .map(UserTagConverter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<UserTag> findByUserIdAndTagName(Long userId, String tagName) {
        UserTagDO userTagDO = userTagMapper.selectByUserIdAndTagName(userId, tagName);
        return Optional.ofNullable(UserTagConverter.toDomain(userTagDO));
    }

    @Override
    public void deleteById(UserTagId id) {
        userTagMapper.deleteById(id.getValue());
    }

    @Override
    public void deleteByIds(List<UserTagId> ids) {
        List<Long> idValues = ids.stream()
                .map(UserTagId::getValue)
                .collect(Collectors.toList());
        userTagMapper.deleteBatchIds(idValues);
    }

    @Override
    public int countByUserId(Long userId) {
        return userTagMapper.countByUserId(userId);
    }
}
