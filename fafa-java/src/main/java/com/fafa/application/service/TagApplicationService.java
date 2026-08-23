package com.fafa.application.service;

import cn.dev33.satoken.stp.StpUtil;
import com.fafa.application.dto.tag.CreateTagCommand;
import com.fafa.application.dto.tag.TagDTO;
import com.fafa.common.BusinessException;
import com.fafa.common.ErrorCode;
import com.fafa.domain.model.tag.TagCategory;
import com.fafa.domain.model.tag.UserTag;
import com.fafa.domain.repository.UserTagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 标签应用服务
 *
 * 标签字典 CRUD（打标联动在 MediaApplicationService / EventRecordApplicationService 编排）。
 * 标签为字典数据物理删除，删除不影响素材/事件上的 tags 快照。
 *
 * @author FaFa Team
 * @since 4.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TagApplicationService {

    private final UserTagRepository userTagRepository;

    /**
     * 标签列表（usage_count DESC, last_used_at DESC）
     */
    public List<TagDTO> list() {
        Long userId = getCurrentUserId();
        return userTagRepository.findByUserIdOrderByUsage(userId).stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * 创建标签
     */
    @Transactional
    public TagDTO create(CreateTagCommand command) {
        Long userId = getCurrentUserId();
        String name = command.name() == null ? null : command.name().trim();
        if (name == null || name.isEmpty() || name.length() > UserTag.MAX_NAME_LENGTH) {
            throw new BusinessException(ErrorCode.TAG_NAME_INVALID);
        }
        if (userTagRepository.findByUserIdAndName(userId, name).isPresent()) {
            throw new BusinessException(ErrorCode.TAG_DUPLICATE);
        }
        TagCategory category = parseCategory(command.category());
        UserTag tag = UserTag.create(userId, name, category, LocalDateTime.now());
        return toDTO(userTagRepository.save(tag));
    }

    /**
     * 删除标签定义（物理删除，不影响快照）
     */
    @Transactional
    public void delete(Long id) {
        Long userId = getCurrentUserId();
        UserTag tag = userTagRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.TAG_NOT_FOUND));
        if (!tag.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        userTagRepository.deleteById(id);
    }

    /**
     * 宠物创建联动：自动建宠物名标签（category=pet），已存在则跳过
     */
    public void createPetNameTagIfAbsent(Long userId, String petName) {
        if (petName == null || petName.isBlank()) {
            return;
        }
        String name = petName.trim();
        if (name.length() > UserTag.MAX_NAME_LENGTH) {
            name = name.substring(0, UserTag.MAX_NAME_LENGTH);
        }
        if (userTagRepository.findByUserIdAndName(userId, name).isPresent()) {
            return;
        }
        UserTag tag = UserTag.create(userId, name, TagCategory.PET, LocalDateTime.now());
        userTagRepository.save(tag);
        log.info("自动创建宠物名标签: userId={}, petName={}", userId, petName);
    }

    private TagCategory parseCategory(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return TagCategory.fromCode(code);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "标签分类取值非法: " + code);
        }
    }

    private TagDTO toDTO(UserTag tag) {
        return new TagDTO(
                tag.getId(),
                tag.getTagName(),
                tag.getCategory() == null ? null : tag.getCategory().getCode(),
                tag.getUsageCount(),
                tag.getLastUsedAt()
        );
    }

    private Long getCurrentUserId() {
        return StpUtil.getLoginIdAsLong();
    }
}
