package com.fafa.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fafa.domain.model.media.MediaAnalysisStatus;
import com.fafa.domain.model.media.MediaAsset;
import com.fafa.domain.model.media.MediaType;
import com.fafa.domain.repository.MediaAssetRepository;
import com.fafa.infrastructure.persistence.mapper.MediaAssetMapper;
import com.fafa.infrastructure.persistence.po.MediaAssetPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 媒体素材仓储实现
 *
 * @author FaFa Team
 * @since 4.0
 */
@Repository
@RequiredArgsConstructor
public class MediaAssetRepositoryImpl implements MediaAssetRepository {

    private final MediaAssetMapper mediaAssetMapper;
    private final JsonListConverter jsonListConverter;

    @Override
    public Optional<MediaAsset> findById(Long id) {
        return Optional.ofNullable(toDomain(mediaAssetMapper.selectById(id)));
    }

    @Override
    public MediaAsset save(MediaAsset asset) {
        MediaAssetPO po = toPO(asset);

        if (po.getId() == null) {
            mediaAssetMapper.insert(po);
        } else {
            mediaAssetMapper.updateById(po);
        }

        return toDomain(mediaAssetMapper.selectById(po.getId()));
    }

    @Override
    public void deleteById(Long id) {
        // @TableLogic 自动转为逻辑删除
        mediaAssetMapper.deleteById(id);
    }

    @Override
    public List<MediaAsset> search(Long userId, String petTagName, List<String> tags,
                                   LocalDate startDate, LocalDate endDate, int offset, int limit) {
        Page<MediaAssetPO> page = new Page<>(offset / limit + 1, limit);
        Page<MediaAssetPO> result = mediaAssetMapper.selectPage(page,
                buildSearchWrapper(userId, petTagName, tags, startDate, endDate));
        return result.getRecords().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long countSearch(Long userId, String petTagName, List<String> tags,
                            LocalDate startDate, LocalDate endDate) {
        return mediaAssetMapper.selectCount(buildSearchWrapper(userId, petTagName, tags, startDate, endDate));
    }

    @Override
    public List<MediaAsset> findByIds(Long userId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<MediaAssetPO> wrapper = new LambdaQueryWrapper<>();
        // 语义检索补全必须过滤归属与逻辑删除（免疫 Qdrant 残留 point）
        wrapper.eq(MediaAssetPO::getUserId, userId)
               .in(MediaAssetPO::getId, ids);
        return mediaAssetMapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<MediaAsset> findUnconfirmed(Long userId, int limit) {
        LambdaQueryWrapper<MediaAssetPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MediaAssetPO::getUserId, userId)
               .isNotNull(MediaAssetPO::getPetId)
               .eq(MediaAssetPO::getRecognitionConfirmed, false)
               .orderByDesc(MediaAssetPO::getTakenAt)
               .last("LIMIT " + limit);
        return mediaAssetMapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public int clearPetReference(Long petId) {
        LambdaUpdateWrapper<MediaAssetPO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(MediaAssetPO::getPetId, petId)
               .set(MediaAssetPO::getPetId, null)
               .set(MediaAssetPO::getRecognitionConfirmed, false);
        return mediaAssetMapper.update(null, wrapper);
    }

    @Override
    public int replacePetNameTag(Long userId, String oldName, String newName) {
        if (userId == null || oldName == null || newName == null
                || oldName.isBlank() || newName.isBlank()) {
            return 0;
        }
        String normalizedOldName = oldName.trim();
        String normalizedNewName = newName.trim();
        if (normalizedOldName.equals(normalizedNewName)) {
            return 0;
        }
        return migratePetNameTag(userId, normalizedOldName, normalizedNewName);
    }

    private static final int TAG_RENAME_BATCH_SIZE = 200;

    private int migratePetNameTag(Long userId, String oldName, String newName) {
        int affected = 0;
        while (true) {
            LambdaQueryWrapper<MediaAssetPO> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(MediaAssetPO::getUserId, userId)
                    .apply("JSON_CONTAINS(tags, JSON_QUOTE({0}))", oldName)
                    .last("LIMIT " + TAG_RENAME_BATCH_SIZE);
            List<MediaAssetPO> rows = mediaAssetMapper.selectList(wrapper);
            if (rows.isEmpty()) {
                return affected;
            }
            for (MediaAssetPO row : rows) {
                MediaAsset asset = toDomain(row);
                if (asset.replaceTag(oldName, newName, LocalDateTime.now())) {
                    mediaAssetMapper.updateById(toPO(asset));
                    affected++;
                }
            }
        }
    }

    /**
     * 确定性检索条件组装（tags 任一命中：JSON_OVERLAPS，OR 语义）
     */
    private LambdaQueryWrapper<MediaAssetPO> buildSearchWrapper(Long userId, String petTagName, List<String> tags,
                                                                LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<MediaAssetPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MediaAssetPO::getUserId, userId);
        if (petTagName != null && !petTagName.isBlank()) {
            // 宠物归属由名称 tag 表达；精确匹配避免“豆”误命中“豆豆”。
            wrapper.apply("JSON_CONTAINS(tags, JSON_QUOTE({0}))", petTagName.trim());
        }
        if (tags != null && !tags.isEmpty()) {
            // {0} 占位符参数化，防注入；JSON 数组文本由 JsonListConverter 生成
            wrapper.apply("JSON_OVERLAPS(tags, {0})", jsonListConverter.toJson(tags));
        }
        if (startDate != null) {
            wrapper.ge(MediaAssetPO::getTakenAt, startDate.atStartOfDay());
        }
        if (endDate != null) {
            wrapper.le(MediaAssetPO::getTakenAt, endDate.atTime(LocalTime.MAX));
        }
        wrapper.orderByDesc(MediaAssetPO::getTakenAt)
               .orderByDesc(MediaAssetPO::getId);
        return wrapper;
    }

    /**
     * 持久化对象转领域对象
     */
    private MediaAsset toDomain(MediaAssetPO po) {
        if (po == null) {
            return null;
        }
        return MediaAsset.restore(
                po.getId(),
                po.getUserId(),
                po.getPetId(),
                MediaType.fromCode(po.getMediaType()),
                po.getFileUrl(),
                po.getFileName(),
                po.getFileSize(),
                po.getMimeType(),
                jsonListConverter.fromJson(po.getTags()),
                MediaAnalysisStatus.fromCode(po.getStatus()),
                po.getAiDescription(),
                jsonListConverter.fromJson(po.getAiTags()),
                po.getRecognitionScore(),
                po.getRecognitionConfirmed() != null && po.getRecognitionConfirmed(),
                po.getTakenAt(),
                po.getErrorMessage(),
                po.getIsDeleted() != null && po.getIsDeleted(),
                po.getOperatorId(),
                po.getCreatedAt(),
                po.getUpdatedAt()
        );
    }

    /**
     * 领域对象转持久化对象
     */
    private MediaAssetPO toPO(MediaAsset asset) {
        MediaAssetPO po = new MediaAssetPO();
        po.setId(asset.getId());
        po.setUserId(asset.getUserId());
        po.setOperatorId(asset.getOperatorId());
        po.setPetId(asset.getPetId());
        po.setMediaType(asset.getMediaType().getCode());
        po.setFileUrl(asset.getFileUrl());
        po.setFileName(asset.getFileName());
        po.setFileSize(asset.getFileSize());
        po.setMimeType(asset.getMimeType());
        po.setTags(jsonListConverter.toJson(asset.getTags()));
        po.setStatus(asset.getStatus().getCode());
        po.setAiDescription(asset.getAiDescription());
        po.setAiTags(jsonListConverter.toJson(asset.getAiTags()));
        po.setRecognitionScore(asset.getRecognitionScore());
        po.setRecognitionConfirmed(asset.isRecognitionConfirmed());
        po.setTakenAt(asset.getTakenAt());
        po.setErrorMessage(asset.getErrorMessage());
        po.setIsDeleted(asset.isDeleted());
        po.setUpdatedAt(asset.getUpdatedAt());
        return po;
    }
}
