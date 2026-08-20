package com.fafa.domain.model.tag;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户标签聚合根
 *
 * @author FaFa
 * @since 2026-08-20
 */
@Data
@Builder
public class UserTag {

    private UserTagId id;
    private Long userId;
    private String tagName;
    private String category;
    private Integer usageCount;
    private LocalDateTime lastUsedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 工厂方法：创建标签
     */
    public static UserTag create(Long userId, String tagName, String category) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (tagName == null || tagName.trim().isEmpty()) {
            throw new IllegalArgumentException("标签名称不能为空");
        }
        if (tagName.length() > 50) {
            throw new IllegalArgumentException("标签名称不能超过50个字符");
        }

        return UserTag.builder()
                .userId(userId)
                .tagName(tagName.trim())
                .category(category != null ? category : "other")
                .usageCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 增加使用次数
     */
    public void incrementUsage() {
        this.usageCount++;
        this.lastUsedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 更新标签信息
     */
    public void updateInfo(String tagName, String category) {
        if (tagName != null && !tagName.trim().isEmpty()) {
            this.tagName = tagName.trim();
        }
        if (category != null) {
            this.category = category;
        }
        this.updatedAt = LocalDateTime.now();
    }
}
