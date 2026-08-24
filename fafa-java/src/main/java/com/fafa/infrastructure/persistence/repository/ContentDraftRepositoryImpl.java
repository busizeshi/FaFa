package com.fafa.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fafa.domain.model.content.ContentDraft;
import com.fafa.domain.repository.ContentDraftRepository;
import com.fafa.infrastructure.persistence.mapper.ContentDraftMapper;
import com.fafa.infrastructure.persistence.po.ContentDraftPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ContentDraftRepositoryImpl implements ContentDraftRepository {
    private final ContentDraftMapper mapper;

    @Override public ContentDraft save(ContentDraft item) {
        ContentDraftPO po = toPO(item);
        if (po.getId() == null) mapper.insert(po); else mapper.updateById(po);
        return toDomain(mapper.selectById(po.getId()));
    }
    @Override public Optional<ContentDraft> findByIdAndUserId(Long id, Long userId) {
        return Optional.ofNullable(toDomain(mapper.selectOne(new LambdaQueryWrapper<ContentDraftPO>()
                .eq(ContentDraftPO::getId, id).eq(ContentDraftPO::getUserId, userId))));
    }
    @Override public Optional<ContentDraft> findByIdempotencyKey(Long userId, String key) {
        return Optional.ofNullable(toDomain(mapper.selectOne(new LambdaQueryWrapper<ContentDraftPO>()
                .eq(ContentDraftPO::getUserId, userId).eq(ContentDraftPO::getIdempotencyKey, key))));
    }
    @Override public List<ContentDraft> findByUserIdAndPetId(Long userId, Long petId, int offset, int limit) {
        return mapper.selectList(new LambdaQueryWrapper<ContentDraftPO>().eq(ContentDraftPO::getUserId, userId)
                .eq(ContentDraftPO::getPetId, petId).orderByDesc(ContentDraftPO::getUpdatedAt)
                .last("LIMIT " + offset + "," + limit)).stream().map(this::toDomain).toList();
    }
    @Override public void delete(ContentDraft item) { mapper.deleteById(item.getId()); }

    private ContentDraftPO toPO(ContentDraft item) {
        ContentDraftPO po = new ContentDraftPO(); po.setId(item.getId()); po.setUserId(item.getUserId());
        po.setPetId(item.getPetId()); po.setContentType(item.getContentType()); po.setTopicType(item.getTopicType());
        po.setCustomTopic(item.getCustomTopic()); po.setPeriodType(item.getPeriodType()); po.setPeriodStart(item.getPeriodStart());
        po.setPeriodEnd(item.getPeriodEnd()); po.setSelectedMediaJson(item.getSelectedMediaJson());
        po.setFactSnapshotJson(item.getFactSnapshotJson()); po.setDraftJson(item.getDraftJson()); po.setStatus(item.getStatus());
        po.setContentMode(item.getContentMode()); po.setGenerationTrigger(item.getGenerationTrigger());
        po.setIdempotencyKey(item.getIdempotencyKey()); po.setGenerationVersion(item.getGenerationVersion());
        po.setModelName(item.getModelName()); po.setPromptVersion(item.getPromptVersion());
        po.setLastErrorCode(item.getLastErrorCode()); po.setLastErrorMessage(item.getLastErrorMessage());
        po.setIsDeleted(item.isDeleted()); po.setCreatedAt(item.getCreatedAt()); po.setUpdatedAt(item.getUpdatedAt());
        return po;
    }
    private ContentDraft toDomain(ContentDraftPO po) {
        if (po == null) return null;
        return ContentDraft.restore(po.getId(), po.getUserId(), po.getPetId(), po.getContentType(), po.getTopicType(),
                po.getCustomTopic(), po.getPeriodType(), po.getPeriodStart(), po.getPeriodEnd(), po.getSelectedMediaJson(),
                po.getFactSnapshotJson(), po.getDraftJson(), po.getStatus(), po.getContentMode(), po.getGenerationTrigger(),
                po.getIdempotencyKey(), po.getGenerationVersion() == null ? 1 : po.getGenerationVersion(), po.getModelName(),
                po.getPromptVersion(), po.getLastErrorCode(), po.getLastErrorMessage(), Boolean.TRUE.equals(po.getIsDeleted()),
                po.getCreatedAt(), po.getUpdatedAt());
    }
}
