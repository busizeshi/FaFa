package com.fafa.domain.repository;

import com.fafa.domain.model.record.FeedRecord;
import com.fafa.domain.model.record.RecordId;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 喂食记录仓储接口
 * 
 * @author FaFa Team
 * @since 1.0
 */
public interface FeedRecordRepository {
    
    /**
     * 保存喂食记录
     */
    FeedRecord save(FeedRecord feedRecord);
    
    /**
     * 根据 ID 查询
     */
    Optional<FeedRecord> findById(RecordId recordId);
    
    /**
     * 根据宠物 ID 查询列表
     */
    List<FeedRecord> findByPetId(Long petId, LocalDate startDate, LocalDate endDate, int page, int size);
    
    /**
     * 获取最近一次喂食记录
     */
    Optional<FeedRecord> findLatestByPetId(Long petId);
    
    /**
     * 删除记录
     */
    void deleteById(RecordId recordId);
    
    /**
     * 根据宠物 ID 删除所有记录
     */
    void deleteByPetId(Long petId);
    
    /**
     * 统计喂食次数
     */
    int countByPetId(Long petId, LocalDate startDate, LocalDate endDate);
}
