package com.fafa.domain.repository;

import com.fafa.domain.model.record.FeedingRecord;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 喂食记录仓储接口
 *
 * @author FaFa Team
 * @since 2.0
 */
public interface FeedingRecordRepository {

    /**
     * 根据 ID 查询（不含已删除）
     */
    Optional<FeedingRecord> findById(Long id);

    /**
     * 保存（新建或更新）
     *
     * @return 保存后的记录（新建时包含生成的 ID）
     */
    FeedingRecord save(FeedingRecord record);

    /**
     * 查询某宠物某天的喂食记录（按时间升序）
     */
    List<FeedingRecord> findByPetIdAndDate(Long petId, LocalDate date);

    /**
     * 查询日期范围内的记录（按时间升序，含起止两端）
     */
    List<FeedingRecord> findByPetIdAndDateRange(Long petId, LocalDate start, LocalDate end);

    /**
     * 逻辑删除
     */
    void deleteById(Long id);
}
