package com.fafa.application.service;

import com.fafa.domain.model.pet.Pet;
import com.fafa.domain.model.reminder.Reminder;
import com.fafa.domain.repository.PetRepository;
import com.fafa.domain.repository.ReminderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 提醒推送服务（扫描 + 去重 + 内容组装）
 *
 * 推送策略：
 * 1. 到期提醒（含逾期）：pending 且 nextRemindTime ≤ now，每日至多推送一次
 * 2. 明日重要提醒：疫苗/体检/驱虫提前 1 天推送
 *
 * 当前网关为日志输出；微信订阅消息接入点见 push() 方法注释与实施文档 8.3。
 *
 * @author FaFa Team
 * @since 3.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderPushService {

    /** 到期扫描回溯窗口：覆盖长期逾期未完成的提醒 */
    private static final int OVERDUE_LOOKBACK_DAYS = 366;

    private final ReminderRepository reminderRepository;
    private final PetRepository petRepository;

    /**
     * 定时扫描入口（由 ReminderPushScheduler 每小时调用）
     */
    public void scanAndPush() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        // 1. 到期提醒（含逾期）：nextRemindTime ≤ now
        List<Reminder> due = reminderRepository.findPendingBetween(
                now.minusDays(OVERDUE_LOOKBACK_DAYS), now);
        for (Reminder reminder : due) {
            if (pushedToday(reminder, today)) {
                continue;
            }
            push(reminder, "到期提醒");
            reminderRepository.updateLastPushTime(reminder.getId(), now);
        }

        // 2. 明日重要提醒（疫苗/体检/驱虫提前 1 天）：nextRemindTime ∈ 明日
        LocalDate tomorrow = today.plusDays(1);
        List<Reminder> tomorrowReminders = reminderRepository.findPendingBetween(
                tomorrow.atStartOfDay(), tomorrow.atTime(23, 59, 59));
        for (Reminder reminder : tomorrowReminders) {
            if (!reminder.getType().isImportant()) {
                continue;
            }
            if (pushedToday(reminder, today)) {
                continue;
            }
            push(reminder, "明日提醒");
            reminderRepository.updateLastPushTime(reminder.getId(), now);
        }
    }

    /**
     * 同一提醒每日至多推送一次（lastPushTime 与今日同日期则跳过）
     */
    private boolean pushedToday(Reminder reminder, LocalDate today) {
        return reminder.getLastPushTime() != null
                && reminder.getLastPushTime().toLocalDate().equals(today);
    }

    /**
     * 组装推送内容并发送
     *
     * 微信订阅消息接入点：小程序发布流程就绪后，在此替换/追加真实发送
     * （失败仅告警，不影响扫描主流程，见实施文档 8.3）。
     */
    private void push(Reminder reminder, String kind) {
        String petName = petRepository.findById(reminder.getPetId())
                .map(Pet::getName)
                .orElse(null);
        // 宠物已删除（理论不该发生——删除宠物级联删提醒）则降级用标题
        String content = petName != null
                ? petName + "：" + reminder.getTitle()
                : reminder.getTitle();

        log.info("[提醒推送-{}] reminderId={}, petId={}, type={}, content={}, remindTime={}",
                kind, reminder.getId(), reminder.getPetId(),
                reminder.getType().getCode(), content, reminder.getNextRemindTime());
    }
}
