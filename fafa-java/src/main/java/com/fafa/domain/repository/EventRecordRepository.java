package com.fafa.domain.repository;

import com.fafa.domain.model.record.EventRecord;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 事件记录仓储接口
 */
public interface EventRecordRepository {
    
    /**
     * 保存事件记录
     */
    EventRecord save(EventRecord eventRecord);
    
    /**
     * 根据ID查询
     */
    Optional<EventRecord> findById(Long id);
    
    /**
     * 根据宠物ID查询记录列表
     */
    List<EventRecord> findByPetId(Long petId, String eventType, LocalDate startDate, LocalDate endDate);
    
    /**
     * 删除记录
     */
    void deleteById(Long id);
    
    /**
     * 根据宠物 ID 删除所有记录
     */
    void deleteByPetId(Long petId);
}
