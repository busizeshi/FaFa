package com.fafa.domain.repository;

import com.fafa.domain.model.tag.TagCategory;
import com.fafa.domain.model.tag.UserTag;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 用户标签仓储接口
 *
 * @author FaFa Team
 * @since 4.0
 */
public interface UserTagRepository {

    Optional<UserTag> findById(Long id);

    Optional<UserTag> findByUserIdAndName(Long userId, String name);

    /**
     * 标签选择列表（usage_count DESC, last_used_at DESC）
     */
    List<UserTag> findByUserIdOrderByUsage(Long userId);

    UserTag save(UserTag tag);

    /**
     * 物理删除（字典数据）
     */
    void deleteById(Long id);

    /**
     * 批量打标联动：不存在则插入（count=1），存在则 usage_count+1 并刷新 last_used_at。
     * 单语句原子完成，避免先查后写竞态。
     *
     * @param category 新插入行的分类（用户手动标签传 null，宠物名自动标签传 PET）
     */
    void upsertAndTouch(Long userId, List<String> tagNames, TagCategory category, LocalDateTime now);
}
