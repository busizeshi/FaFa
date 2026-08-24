package com.fafa.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fafa.domain.model.content.ContentSchedule;
import com.fafa.domain.repository.ContentScheduleRepository;
import com.fafa.infrastructure.persistence.mapper.ContentScheduleMapper;
import com.fafa.infrastructure.persistence.po.ContentSchedulePO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ContentScheduleRepositoryImpl implements ContentScheduleRepository {
    private final ContentScheduleMapper mapper;
    @Override public ContentSchedule save(ContentSchedule item) {
        ContentSchedulePO po = toPO(item); if (po.getId() == null) mapper.insert(po); else mapper.updateById(po);
        return toDomain(mapper.selectById(po.getId()));
    }
    @Override public Optional<ContentSchedule> findByIdAndUserId(Long id, Long userId) {
        return Optional.ofNullable(toDomain(mapper.selectOne(new LambdaQueryWrapper<ContentSchedulePO>()
                .eq(ContentSchedulePO::getId, id).eq(ContentSchedulePO::getUserId, userId))));
    }
    @Override public Optional<ContentSchedule> findByUserPetType(Long userId, Long petId, String type) {
        return Optional.ofNullable(toDomain(mapper.selectOne(new LambdaQueryWrapper<ContentSchedulePO>()
                .eq(ContentSchedulePO::getUserId, userId).eq(ContentSchedulePO::getPetId, petId)
                .eq(ContentSchedulePO::getContentType, type))));
    }
    @Override public List<ContentSchedule> findByUserId(Long userId, Long petId) {
        LambdaQueryWrapper<ContentSchedulePO> w = new LambdaQueryWrapper<ContentSchedulePO>().eq(ContentSchedulePO::getUserId, userId);
        if (petId != null) w.eq(ContentSchedulePO::getPetId, petId);
        return mapper.selectList(w.orderByDesc(ContentSchedulePO::getUpdatedAt)).stream().map(this::toDomain).toList();
    }
    @Override public List<ContentSchedule> findDue(LocalDateTime now, int limit) {
        return mapper.selectList(new LambdaQueryWrapper<ContentSchedulePO>().eq(ContentSchedulePO::getEnabled, true)
                .le(ContentSchedulePO::getNextRunAt, now).orderByAsc(ContentSchedulePO::getNextRunAt)
                .last("LIMIT 0," + limit)).stream().map(this::toDomain).toList();
    }
    @Override public void delete(ContentSchedule item) { mapper.deleteById(item.getId()); }

    private ContentSchedulePO toPO(ContentSchedule item) {
        ContentSchedulePO po = new ContentSchedulePO(); po.setId(item.getId()); po.setUserId(item.getUserId());
        po.setPetId(item.getPetId()); po.setContentType(item.getContentType()); po.setTopicType(item.getTopicType());
        po.setIntervalDays(item.getIntervalDays()); po.setNextRunAt(item.getNextRunAt()); po.setLastRunAt(item.getLastRunAt());
        po.setEnabled(item.isEnabled()); po.setIsDeleted(item.isDeleted()); po.setCreatedAt(item.getCreatedAt()); po.setUpdatedAt(item.getUpdatedAt());
        return po;
    }
    private ContentSchedule toDomain(ContentSchedulePO po) {
        if (po == null) return null;
        return ContentSchedule.restore(po.getId(), po.getUserId(), po.getPetId(), po.getContentType(), po.getTopicType(),
                po.getIntervalDays(), po.getNextRunAt(), po.getLastRunAt(), Boolean.TRUE.equals(po.getEnabled()),
                Boolean.TRUE.equals(po.getIsDeleted()), po.getCreatedAt(), po.getUpdatedAt());
    }
}
