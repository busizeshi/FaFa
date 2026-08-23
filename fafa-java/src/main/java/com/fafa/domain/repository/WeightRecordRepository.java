package com.fafa.domain.repository;

import com.fafa.domain.model.record.WeightRecord;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 体重记录仓储接口
 *
 * @author FaFa Team
 * @since 2.0
 */
public interface WeightRecordRepository {

    /**
     * 根据 ID 查询（不含已删除）
     */
    Optional<WeightRecord> findById(Long id);

    /**
     * 保存（新建或更新）
     *
     * @return 保存后的记录（新建时包含生成的 ID）
     */
    WeightRecord save(WeightRecord record);

    /**
     * 查询最近 N 条记录（按日期倒序）
     */
    List<WeightRecord> findLatest(Long petId, int limit);

    /**
     * 查询日期范围内的记录（按日期升序，含起止两端）
     */
    List<WeightRecord> findByPetIdAndDateRange(Long petId, LocalDate start, LocalDate end);

    /**
     * 查询最近一条记录
     */
    Optional<WeightRecord> findLatestByPetId(Long petId);

    /**
     * 查询严格早于指定日期的最近一条记录（趋势首条计算 change 用）
     */
    Optional<WeightRecord> findLatestBefore(Long petId, LocalDate exclusiveDate);

    /**
     * 逻辑删除
     */
    void deleteById(Long id);
}
