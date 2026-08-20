package com.fafa.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fafa.domain.model.record.EventRecord;
import com.fafa.domain.repository.EventRecordRepository;
import com.fafa.infrastructure.persistence.converter.EventRecordConverter;
import com.fafa.infrastructure.persistence.dataobject.EventRecordDO;
import com.fafa.infrastructure.persistence.mapper.EventRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 事件记录仓储实现
 */
@Repository
@RequiredArgsConstructor
public class EventRecordRepositoryImpl implements EventRecordRepository {
    
    private final EventRecordMapper eventRecordMapper;
    
    @Override
    public EventRecord save(EventRecord eventRecord) {
        EventRecordDO eventRecordDO = EventRecordConverter.toDO(eventRecord);
        
        if (eventRecordDO.getId() == null) {
            eventRecordMapper.insert(eventRecordDO);
        } else {
            eventRecordMapper.updateById(eventRecordDO);
        }
        
        return EventRecordConverter.toDomain(eventRecordDO);
    }
    
    @Override
    public Optional<EventRecord> findById(Long id) {
        EventRecordDO eventRecordDO = eventRecordMapper.selectById(id);
        return Optional.ofNullable(EventRecordConverter.toDomain(eventRecordDO));
    }
    
    @Override
    public List<EventRecord> findByPetId(Long petId, String eventType, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<EventRecordDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EventRecordDO::getPetId, petId);
        
        if (eventType != null && !eventType.isEmpty()) {
            wrapper.eq(EventRecordDO::getEventType, eventType);
        }
        
        if (startDate != null) {
            wrapper.ge(EventRecordDO::getEventTime, LocalDateTime.of(startDate, LocalTime.MIN));
        }
        if (endDate != null) {
            wrapper.le(EventRecordDO::getEventTime, LocalDateTime.of(endDate, LocalTime.MAX));
        }
        
        wrapper.orderByDesc(EventRecordDO::getEventTime);
        
        return eventRecordMapper.selectList(wrapper).stream()
                .map(EventRecordConverter::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public void deleteById(Long id) {
        eventRecordMapper.deleteById(id);
    }
    
    @Override
    public void deleteByPetId(Long petId) {
        LambdaQueryWrapper<EventRecordDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EventRecordDO::getPetId, petId);
        eventRecordMapper.delete(wrapper);
    }
}
