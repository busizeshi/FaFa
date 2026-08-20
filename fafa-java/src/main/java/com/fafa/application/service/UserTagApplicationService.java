package com.fafa.application.service;

import com.fafa.domain.model.tag.UserTag;
import com.fafa.domain.model.tag.UserTagId;
import com.fafa.domain.repository.UserTagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 用户标签应用服务
 *
 * @author FaFa
 * @since 2026-08-20
 */
@Service
public class UserTagApplicationService {

    @Resource
    private UserTagRepository userTagRepository;

    /**
     * 创建标签
     */
    @Transactional(rollbackFor = Exception.class)
    public UserTag createTag(Long userId, String tagName, String category) {
        if (userTagRepository.findByUserIdAndTagName(userId, tagName).isPresent()) {
            throw new IllegalArgumentException("标签已存在");
        }

        UserTag userTag = UserTag.create(userId, tagName, category);
        return userTagRepository.save(userTag);
    }

    /**
     * 批量创建或获取标签
     */
    @Transactional(rollbackFor = Exception.class)
    public List<UserTag> getOrCreateTags(Long userId, List<String> tagNames, String category) {
        return tagNames.stream()
                .map(tagName -> {
                    return userTagRepository.findByUserIdAndTagName(userId, tagName)
                            .orElseGet(() -> {
                                UserTag newTag = UserTag.create(userId, tagName, category);
                                return userTagRepository.save(newTag);
                            });
                })
                .toList();
    }

    /**
     * 增加标签使用次数
     */
    @Transactional(rollbackFor = Exception.class)
    public void incrementTagUsage(Long userId, List<String> tagNames) {
        tagNames.forEach(tagName -> {
            userTagRepository.findByUserIdAndTagName(userId, tagName).ifPresent(tag -> {
                tag.incrementUsage();
                userTagRepository.save(tag);
            });
        });
    }

    /**
     * 更新标签
     */
    @Transactional(rollbackFor = Exception.class)
    public UserTag updateTag(Long tagId, String tagName, String category) {
        UserTag userTag = userTagRepository.findById(UserTagId.of(tagId))
                .orElseThrow(() -> new IllegalArgumentException("标签不存在"));

        userTag.updateInfo(tagName, category);
        return userTagRepository.save(userTag);
    }

    /**
     * 删除标签
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteTag(Long tagId) {
        userTagRepository.deleteById(UserTagId.of(tagId));
    }

    /**
     * 批量删除标签
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteTags(List<Long> tagIds) {
        List<UserTagId> userTagIds = tagIds.stream()
                .map(UserTagId::of)
                .toList();
        userTagRepository.deleteByIds(userTagIds);
    }

    /**
     * 查询用户所有标签
     */
    public List<UserTag> getUserTags(Long userId) {
        return userTagRepository.findByUserId(userId);
    }

    /**
     * 查询热门标签
     */
    public List<UserTag> getPopularTags(Long userId, int limit) {
        return userTagRepository.findByUserIdOrderByUsageCountDesc(userId, limit);
    }

    /**
     * 统计用户标签数量
     */
    public int countUserTags(Long userId) {
        return userTagRepository.countByUserId(userId);
    }
}
