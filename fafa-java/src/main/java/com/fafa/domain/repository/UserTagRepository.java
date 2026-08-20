package com.fafa.domain.repository;

import com.fafa.domain.model.tag.UserTag;
import com.fafa.domain.model.tag.UserTagId;

import java.util.List;
import java.util.Optional;

/**
 * 用户标签仓储接口
 *
 * @author FaFa
 * @since 2026-08-20
 */
public interface UserTagRepository {

    /**
     * 保存标签
     */
    UserTag save(UserTag userTag);

    /**
     * 根据ID查询
     */
    Optional<UserTag> findById(UserTagId id);

    /**
     * 查询用户的所有标签
     */
    List<UserTag> findByUserId(Long userId);

    /**
     * 按使用频率查询
     */
    List<UserTag> findByUserIdOrderByUsageCountDesc(Long userId, int limit);

    /**
     * 根据用户ID和标签名查询
     */
    Optional<UserTag> findByUserIdAndTagName(Long userId, String tagName);

    /**
     * 删除标签
     */
    void deleteById(UserTagId id);

    /**
     * 批量删除
     */
    void deleteByIds(List<UserTagId> ids);

    /**
     * 统计用户标签数量
     */
    int countByUserId(Long userId);
}
