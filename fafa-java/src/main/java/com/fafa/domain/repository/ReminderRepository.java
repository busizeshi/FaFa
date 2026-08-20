package com.fafa.domain.repository;

import com.fafa.domain.model.reminder.Reminder;
import com.fafa.domain.model.reminder.ReminderId;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 提醒仓储接口
 *
 * @author FaFa
 * @since 2026-08-18
 */
public interface ReminderRepository {
    
    /**
     * 保存提醒
     */
    Reminder save(Reminder reminder);
    
    /**
     * 根据 ID 查询提醒
     */
    Optional<Reminder> findById(ReminderId reminderId);
    
    /**
     * 根据宠物 ID 查询提醒列表
     */
    List<Reminder> findByPetId(Long petId, String status, Integer pageNum, Integer pageSize);
    
    /**
     * 根据用户 ID 查询提醒列表
     */
    List<Reminder> findByUserId(Long userId, String status, Integer pageNum, Integer pageSize);
    
    /**
     * 查询待推送的提醒（未通知且在指定时间范围内）
     */
    List<Reminder> findPendingNotifications(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 统计提醒数量
     */
    int countByPetId(Long petId, String status);
    
    /**
     * 统计用户提醒数量
     */
    int countByUserId(Long userId, String status);
    
    /**
     * 删除提醒
     */
    void deleteById(ReminderId reminderId);
    
    /**
     * 根据宠物 ID 删除所有提醒
     */
    void deleteByPetId(Long petId);
}
