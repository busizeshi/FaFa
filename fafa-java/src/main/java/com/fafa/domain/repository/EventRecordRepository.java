package com.fafa.domain.repository;

import com.fafa.domain.model.event.EventRecord;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 事件记录仓储接口
 *
 * @author FaFa Team
 * @since 3.0
 */
public interface EventRecordRepository {

    /**
     * 保存（新建）
     *
     * @return 保存后的事件记录（包含生成的 ID）
     */
    EventRecord save(EventRecord record);

    Optional<EventRecord> findById(Long id);

    /** 逻辑删除事件记录 */
    void deleteById(Long id);

    /**
     * 宠物事件时间轴（日期升序，验证/预览接口用）
     */
    List<EventRecord> findByPetIdAndDateRange(Long petId, LocalDate start, LocalDate end);
}
