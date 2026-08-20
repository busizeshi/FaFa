package com.fafa.infrastructure.persistence.converter;

import com.fafa.domain.model.tag.UserTag;
import com.fafa.domain.model.tag.UserTagId;
import com.fafa.infrastructure.persistence.dataobject.UserTagDO;

/**
 * 用户标签转换器
 *
 * @author FaFa
 * @since 2026-08-20
 */
public class UserTagConverter {

    /**
     * DO -> 领域模型
     */
    public static UserTag toDomain(UserTagDO userTagDO) {
        if (userTagDO == null) {
            return null;
        }

        return UserTag.builder()
                .id(UserTagId.of(userTagDO.getId()))
                .userId(userTagDO.getUserId())
                .tagName(userTagDO.getTagName())
                .category(userTagDO.getCategory())
                .usageCount(userTagDO.getUsageCount())
                .lastUsedAt(userTagDO.getLastUsedAt())
                .createdAt(userTagDO.getCreatedAt())
                .updatedAt(userTagDO.getUpdatedAt())
                .build();
    }

    /**
     * 领域模型 -> DO
     */
    public static UserTagDO toDO(UserTag userTag) {
        if (userTag == null) {
            return null;
        }

        UserTagDO userTagDO = new UserTagDO();
        if (userTag.getId() != null) {
            userTagDO.setId(userTag.getId().getValue());
        }
        userTagDO.setUserId(userTag.getUserId());
        userTagDO.setTagName(userTag.getTagName());
        userTagDO.setCategory(userTag.getCategory());
        userTagDO.setUsageCount(userTag.getUsageCount());
        userTagDO.setLastUsedAt(userTag.getLastUsedAt());
        userTagDO.setCreatedAt(userTag.getCreatedAt());
        userTagDO.setUpdatedAt(userTag.getUpdatedAt());

        return userTagDO;
    }
}
