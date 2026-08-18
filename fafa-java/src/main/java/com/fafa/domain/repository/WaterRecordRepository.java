package com.fafa.domain.repository;

import com.fafa.domain.model.record.WaterRecord;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 饮水记录仓储接口
 */
public interface WaterRecordRepository {
    
    /**
     * 保存饮水记录
     */
    WaterRecord save(WaterRecord waterRecord);
    
    /**
     * 根据ID查询
     */
    Optional<WaterRecord> findById(Long id);
    
    /**
     * 根据宠物ID查询记录列表
     */
    List<WaterRecord> findByPetId(Long petId, LocalDate startDate, LocalDate endDate);
    
    /**
     * 删除记录
     */
    void deleteById(Long id);
    
    /**
     * 统计日期范围内的饮水总量
     */
    int sumAmountByPetIdAndDateRange(Long petId, LocalDate startDate, LocalDate endDate);
}
