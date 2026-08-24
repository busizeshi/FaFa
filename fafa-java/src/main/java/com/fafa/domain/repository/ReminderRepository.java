package com.fafa.domain.repository;

import com.fafa.domain.model.reminder.Reminder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 提醒仓储接口
 *
 * @author FaFa Team
 * @since 3.0
 */
public interface ReminderRepository {

    /**
     * 保存（新建或更新）
     *
     * @return 保存后的提醒（新建时包含生成的 ID）
     */
    Reminder save(Reminder reminder);

    /**
     * 根据 ID 查询（不含已删除）
     */
    Optional<Reminder> findById(Long id);

    /**
     * 宠物的全部提醒（提醒管理页，含 paused/finished，按 nextRemindTime 升序）
     */
    List<Reminder> findByPetId(Long petId);

    /** 查询一个用药计划生成的提醒。 */
    List<Reminder> findByMedicationPlanId(Long medicationPlanId);

    /**
     * 用户待办：status=pending 且 nextRemindTime < endExclusive（今日+逾期，首页今日提醒）
     */
    List<Reminder> findDueByUserId(Long userId, LocalDateTime endExclusive);

    /**
     * 定时扫描：status=pending 且 nextRemindTime ∈ [start, end]（全用户）
     */
    List<Reminder> findPendingBetween(LocalDateTime start, LocalDateTime end);

    /**
     * 逻辑删除
     */
    void deleteById(Long id);

    /**
     * 宠物删除级联：逻辑删除该宠物全部提醒
     */
    void deleteByPetId(Long petId);

    /** 逻辑删除一个用药计划生成的全部提醒。 */
    void deleteByMedicationPlanId(Long medicationPlanId);

    /**
     * 推送去重标记回写（独立小更新，避免整行 update 竞争）
     */
    void updateLastPushTime(Long id, LocalDateTime pushTime);
}
