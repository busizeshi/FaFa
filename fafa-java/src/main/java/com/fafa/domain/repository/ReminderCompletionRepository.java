package com.fafa.domain.repository;

import com.fafa.domain.model.reminder.ReminderCompletion;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 提醒完成历史仓储接口
 *
 * @author FaFa Team
 * @since 3.0
 */
public interface ReminderCompletionRepository {

    /**
     * 保存（追加只写）
     *
     * @return 保存后的完成记录（包含生成的 ID）
     */
    ReminderCompletion save(ReminderCompletion completion);

    /**
     * 单提醒完成历史（时间倒序）
     */
    List<ReminderCompletion> findByReminderId(Long reminderId);

    /**
     * 用户当日完成记录 [start, end)，首页「今日已完成」
     */
    List<ReminderCompletion> findByUserIdAndTimeBetween(Long userId, LocalDateTime start, LocalDateTime end);

    /** 查询指定宠物的提醒完成历史，供医疗时间线和健康摘要使用。 */
    List<ReminderCompletion> findByPetIdAndTimeBetween(Long userId, Long petId,
                                                       LocalDateTime start, LocalDateTime end);
}
