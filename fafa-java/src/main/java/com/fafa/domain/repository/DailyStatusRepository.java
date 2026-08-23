package com.fafa.domain.repository;

import com.fafa.domain.model.record.DailyStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 每日状态记录仓储接口
 *
 * @author FaFa Team
 * @since 2.0
 */
public interface DailyStatusRepository {

    /**
     * 查询某宠物某天的记录（一天一格）
     */
    Optional<DailyStatus> findByPetIdAndDate(Long petId, LocalDate date);

    /**
     * 查询严格早于指定日期的最近一条记录（「同上次」用）
     */
    Optional<DailyStatus> findLatestBefore(Long petId, LocalDate exclusiveDate);

    /**
     * 查询日期范围内的记录（按日期升序，含起止两端）
     */
    List<DailyStatus> findByPetIdAndDateRange(Long petId, LocalDate start, LocalDate end);

    /**
     * upsert 保存：当天无记录则插入（回填 ID），有则覆盖更新
     *
     * @return 保存后的记录（含 ID 与时间戳）
     */
    DailyStatus save(DailyStatus status);

    /**
     * 物理删除某宠物某天的记录
     *
     * @return 是否存在并被删除
     */
    boolean deleteByPetIdAndDate(Long petId, LocalDate date);
}
