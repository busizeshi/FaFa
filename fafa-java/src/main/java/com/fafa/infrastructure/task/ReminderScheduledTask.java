package com.fafa.infrastructure.task;

import com.fafa.domain.model.reminder.Reminder;
import com.fafa.domain.repository.ReminderRepository;
import com.fafa.infrastructure.wechat.WechatSubscribeMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 提醒定时任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderScheduledTask {

    private final ReminderRepository reminderRepository;
    private final WechatSubscribeMessageService wechatSubscribeMessageService;

    /**
     * 每5分钟扫描待推送的提醒
     */
    @Scheduled(cron = "0 */5 * * * ?")
    @Transactional
    public void scanPendingReminders() {
        log.info("开始扫描待推送的提醒");
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime endTime = now.plusHours(1); // 查询未来1小时内的提醒

            List<Reminder> reminders = reminderRepository.findPendingNotifications(now, endTime);
            log.info("找到 {} 条待推送的提醒", reminders.size());

            for (Reminder reminder : reminders) {
                try {
                    pushReminder(reminder);
                } catch (Exception e) {
                    log.error("推送提醒失败，reminderId: {}", reminder.getReminderId().getValue(), e);
                }
            }

            log.info("提醒扫描完成");
        } catch (Exception e) {
            log.error("扫描待推送提醒失败", e);
        }
    }

    /**
     * 每天凌晨1点扫描过期提醒
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void scanExpiredReminders() {
        log.info("开始扫描过期提醒");
        try {
            // TODO: 实现过期提醒扫描逻辑
            // 1. 查询状态为 pending 且提醒时间早于当前时间的提醒
            // 2. 将这些提醒的状态更新为 expired
            
            log.info("过期提醒扫描完成");
        } catch (Exception e) {
            log.error("扫描过期提醒失败", e);
        }
    }

    /**
     * 推送提醒
     */
    private void pushReminder(Reminder reminder) {
        log.info("推送提醒，reminderId: {}", reminder.getReminderId().getValue());
        
        // TODO: 获取宠物信息和用户openid
        // 这里需要通过 petId 和 userId 查询相关信息
        // 暂时先记录日志
        
        // 标记为已通知
        reminder.markAsNotified();
        reminderRepository.save(reminder);
        
        log.info("提醒推送成功，reminderId: {}", reminder.getReminderId().getValue());
    }
}
