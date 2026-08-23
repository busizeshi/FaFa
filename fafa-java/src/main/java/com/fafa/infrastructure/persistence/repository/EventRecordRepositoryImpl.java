package com.fafa.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fafa.domain.model.event.EventRecord;
import com.fafa.domain.model.event.EventSource;
import com.fafa.domain.model.event.EventType;
import com.fafa.domain.repository.EventRecordRepository;
import com.fafa.infrastructure.persistence.mapper.EventRecordMapper;
import com.fafa.infrastructure.persistence.po.EventRecordPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 事件记录仓储实现
 *
 * @author FaFa Team
 * @since 3.0
 */
@Repository
@RequiredArgsConstructor
public class EventRecordRepositoryImpl implements EventRecordRepository {

    private final EventRecordMapper eventRecordMapper;
    private final JsonListConverter jsonListConverter;

    @Override
    public EventRecord save(EventRecord record) {
        EventRecordPO po = toPO(record);

        if (po.getId() == null) {
            eventRecordMapper.insert(po);
        } else {
            eventRecordMapper.updateById(po);
        }

        return toDomain(eventRecordMapper.selectById(po.getId()));
    }

    @Override
    public Optional<EventRecord> findById(Long id) {
        return Optional.ofNullable(toDomain(eventRecordMapper.selectById(id)));
    }

    @Override
    public void deleteById(Long id) {
        eventRecordMapper.deleteById(id);
    }

    @Override
    public List<EventRecord> findByPetIdAndDateRange(Long petId, LocalDate start, LocalDate end) {
        LambdaQueryWrapper<EventRecordPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EventRecordPO::getPetId, petId)
               .ge(EventRecordPO::getEventDate, start)
               .le(EventRecordPO::getEventDate, end)
               .orderByAsc(EventRecordPO::getEventDate);
        return eventRecordMapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .toList();
    }

    /**
     * 持久化对象转领域对象
     */
    private EventRecord toDomain(EventRecordPO po) {
        if (po == null) {
            return null;
        }
        return EventRecord.restore(
                po.getId(),
                po.getPetId(),
                po.getUserId(),
                EventSource.fromCode(po.getSource()),
                po.getSourceId(),
                EventType.fromCode(po.getEventType()),
                po.getTitle(),
                po.getContent(),
                po.getEventDate(),
                jsonListConverter.fromJson(po.getMaterialUrls()),
                jsonListConverter.fromJson(po.getTags()),
                po.getOperatorId(),
                po.getIsDeleted() != null && po.getIsDeleted(),
                po.getCreatedAt(),
                po.getUpdatedAt()
        );
    }

    /**
     * 领域对象转持久化对象
     */
    private EventRecordPO toPO(EventRecord record) {
        EventRecordPO po = new EventRecordPO();
        po.setId(record.getId());
        po.setPetId(record.getPetId());
        po.setUserId(record.getUserId());
        po.setSource(record.getSource().getCode());
        po.setSourceId(record.getSourceId());
        po.setEventType(record.getEventType().getCode());
        po.setTitle(record.getTitle());
        po.setContent(record.getContent());
        po.setEventDate(record.getEventDate());
        po.setMaterialUrls(jsonListConverter.toJson(record.getMaterialUrls()));
        po.setTags(jsonListConverter.toJson(record.getTags()));
        po.setOperatorId(record.getOperatorId());
        po.setIsDeleted(record.isDeleted());
        return po;
    }
}
