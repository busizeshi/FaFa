package com.fafa.domain.model.tag;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 用户标签实体
 *
 * 字典数据（物理删除）；usage_count 表示历史使用热度，取消打标不回退。
 *
 * @author FaFa Team
 * @since 4.0
 */
@Getter
public class UserTag {

    /** 标签名最大长度（对齐 user_tag.tag_name 列宽） */
    public static final int MAX_NAME_LENGTH = 50;

    private Long id;
    private final Long userId;
    private final String tagName;
    private TagCategory category;
    private int usageCount;
    private LocalDateTime lastUsedAt;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private UserTag(Long id, Long userId, String tagName, TagCategory category,
                    int usageCount, LocalDateTime lastUsedAt,
                    LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.tagName = tagName;
        this.category = category;
        this.usageCount = usageCount;
        this.lastUsedAt = lastUsedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 创建标签
     *
     * @param category 分类，可空（用户手动标签为 null，宠物名自动标签为 PET）
     */
    public static UserTag create(Long userId, String tagName, TagCategory category, LocalDateTime now) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User ID is required");
        }
        String name = tagName == null ? null : tagName.trim();
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Tag name is required");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Tag name must not exceed " + MAX_NAME_LENGTH + " characters");
        }
        return new UserTag(null, userId, name, category, 0, null, now, now);
    }

    /**
     * 仓储反序列化
     */
    public static UserTag restore(Long id, Long userId, String tagName, TagCategory category,
                                  int usageCount, LocalDateTime lastUsedAt,
                                  LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new UserTag(id, userId, tagName, category, usageCount, lastUsedAt, createdAt, updatedAt);
    }

    /**
     * 打标命中：计数+1 并刷新最后使用时间
     */
    public UserTag touch(LocalDateTime now) {
        return new UserTag(id, userId, tagName, category, usageCount + 1, now, createdAt, now);
    }

    public void assignId(Long id) {
        this.id = id;
    }
}
