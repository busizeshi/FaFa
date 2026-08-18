package com.fafa.domain.repository;

import com.fafa.domain.model.record.ExcretionRecord;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 排便记录仓储接口
 */
public interface ExcretionRecordRepository {
    
    /**
     * 保存排便记录
     */
    ExcretionRecord save(ExcretionRecord excretionRecord);
    
    /**
     * 根据ID查询
     */
    Optional<ExcretionRecord> findById(Long id);
    
    /**
     * 根据宠物ID查询记录列表
     */
    List<ExcretionRecord> findByPetId(Long petId, LocalDate startDate, LocalDate endDate);
    
    /**
     * 删除记录
     */
    void deleteById(Long id);
    
    /**
     * 统计日期范围内的排便次数
     */
    int countByPetIdAndTypeAndDateRange(Long petId, String type, LocalDate startDate, LocalDate endDate);
}
