package com.fafa.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fafa.domain.model.tag.TagCategory;
import com.fafa.domain.model.tag.UserTag;
import com.fafa.domain.repository.UserTagRepository;
import com.fafa.infrastructure.persistence.mapper.UserTagMapper;
import com.fafa.infrastructure.persistence.po.UserTagPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

/**
 * 用户标签仓储实现
 *
 * @author FaFa Team
 * @since 4.0
 */
@Repository
@RequiredArgsConstructor
public class UserTagRepositoryImpl implements UserTagRepository {

    private final UserTagMapper userTagMapper;

    @Override
    public Optional<UserTag> findById(Long id) {
        return Optional.ofNullable(toDomain(userTagMapper.selectById(id)));
    }

    @Override
    public Optional<UserTag> findByUserIdAndName(Long userId, String name) {
        LambdaQueryWrapper<UserTagPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserTagPO::getUserId, userId)
               .eq(UserTagPO::getTagName, name);
        return Optional.ofNullable(toDomain(userTagMapper.selectOne(wrapper)));
    }

    @Override
    public List<UserTag> findByUserIdOrderByUsage(Long userId) {
        LambdaQueryWrapper<UserTagPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserTagPO::getUserId, userId)
               .orderByDesc(UserTagPO::getUsageCount)
               .orderByDesc(UserTagPO::getLastUsedAt);
        return userTagMapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public UserTag save(UserTag tag) {
        UserTagPO po = toPO(tag);

        if (po.getId() == null) {
            userTagMapper.insert(po);
        } else {
            userTagMapper.updateById(po);
        }

        return toDomain(userTagMapper.selectById(po.getId()));
    }

    @Override
    public void deleteById(Long id) {
        // 字典数据物理删除（无 @TableLogic）
        userTagMapper.deleteById(id);
    }

    @Override
    public List<UserTag> upsertAndTouch(Long userId, List<String> tagNames, TagCategory category, LocalDateTime now) {
        if (tagNames == null || tagNames.isEmpty()) {
            return List.of();
        }
        String categoryCode = category == null ? null : category.getCode();
        List<UserTag> result = new ArrayList<>();
        for (String rawName : new LinkedHashSet<>(tagNames)) {
            String tagName = rawName == null ? null : rawName.trim();
            if (tagName == null || tagName.isBlank()) {
                continue;
            }
            // 原子 upsert：不存在插入（count=1），存在计数+1
            userTagMapper.upsertAndTouch(userId, tagName, categoryCode, now);
            findByUserIdAndName(userId, tagName).ifPresent(result::add);
        }
        return result;
    }

    /**
     * 持久化对象转领域对象
     */
    private UserTag toDomain(UserTagPO po) {
        if (po == null) {
            return null;
        }
        return UserTag.restore(
                po.getId(),
                po.getUserId(),
                po.getTagName(),
                po.getCategory() == null ? null : TagCategory.fromCode(po.getCategory()),
                po.getUsageCount() == null ? 0 : po.getUsageCount(),
                po.getLastUsedAt(),
                po.getCreatedAt(),
                po.getUpdatedAt()
        );
    }

    /**
     * 领域对象转持久化对象
     */
    private UserTagPO toPO(UserTag tag) {
        UserTagPO po = new UserTagPO();
        po.setId(tag.getId());
        po.setUserId(tag.getUserId());
        po.setTagName(tag.getTagName());
        po.setCategory(tag.getCategory() == null ? null : tag.getCategory().getCode());
        po.setUsageCount(tag.getUsageCount());
        po.setLastUsedAt(tag.getLastUsedAt());
        return po;
    }
}
