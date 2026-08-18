package com.fafa.domain.repository;

import com.fafa.domain.model.record.WeightRecord;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 体重记录仓储接口
 */
public interface WeightRecordRepository {
    
    /**
     * 保存体重记录
     */
    WeightRecord save(WeightRecord weightRecord);
    
    /**
     * 根据ID查询
     */
    Optional<WeightRecord> findById(Long id);
    
    /**
     * 根据宠物ID查询记录列表
     */
    List<WeightRecord> findByPetId(Long petId, LocalDate startDate, LocalDate endDate);
    
    /**
     * 查询宠物最近一次体重记录
     */
    Optional<WeightRecord> findLatestByPetId(Long petId);
    
    /**
     * 删除记录
     */
    void deleteById(Long id);
    
    /**
     * 根据宠物ID和日期查询（用于防止重复）
     */
    Optional<WeightRecord> findByPetIdAndDate(Long petId, LocalDate recordDate);
}
