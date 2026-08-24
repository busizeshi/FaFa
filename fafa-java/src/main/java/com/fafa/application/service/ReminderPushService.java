package com.fafa.application.service;

import com.fafa.domain.model.pet.Pet;
import com.fafa.domain.model.reminder.Reminder;
import com.fafa.domain.model.reminder.ReminderNotificationDelivery;
import com.fafa.domain.repository.PetRepository;
import com.fafa.domain.repository.PetMemberRepository;
import com.fafa.domain.repository.ReminderRepository;
import com.fafa.domain.repository.ReminderNotificationDeliveryRepository;
import com.fafa.infrastructure.config.FaFaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private final PetMemberRepository memberRepository;
    private final ReminderNotificationDeliveryRepository deliveryRepository;
    private final FaFaProperties properties;

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
            pushAccordingToMode(reminder, "到期提醒", today + ":due", today, now);
        }

        // 2. 明日重要提醒（疫苗/体检/驱虫提前 1 天）：nextRemindTime ∈ 明日
        LocalDate tomorrow = today.plusDays(1);
        List<Reminder> tomorrowReminders = reminderRepository.findPendingBetween(
                tomorrow.atStartOfDay(), tomorrow.atTime(23, 59, 59));
        for (Reminder reminder : tomorrowReminders) {
            if (!reminder.getType().isImportant()) {
                continue;
            }
            pushAccordingToMode(reminder, "明日提醒", today + ":tomorrow", today, now);
        }
    }

    /**
     * 同一提醒每日至多推送一次（lastPushTime 与今日同日期则跳过）
     */
    private boolean pushedToday(Reminder reminder, LocalDate today) {
        return reminder.getLastPushTime() != null
                && reminder.getLastPushTime().toLocalDate().equals(today);
    }

    private void pushAccordingToMode(Reminder reminder, String kind, String deliveryWindow,
                                     LocalDate today, LocalDateTime now) {
        if (!properties.getCollaboration().isEnabled()) {
            if (pushedToday(reminder, today)) return;
            push(reminder, kind, reminder.getUserId());
            reminderRepository.updateLastPushTime(reminder.getId(), now);
            return;
        }

        List<Long> recipients = new ArrayList<>();
        recipients.add(reminder.getUserId());
        memberRepository.findByPetId(reminder.getPetId()).stream()
                .filter(member -> member.isActiveAt(now))
                .map(member -> member.getUserId())
                .forEach(recipients::add);
        for (Long recipient : recipients) {
            ReminderNotificationDelivery delivery = deliveryRepository
                    .findByBusinessKey(reminder.getId(), recipient, deliveryWindow)
                    .orElseGet(() -> ReminderNotificationDelivery.pending(
                            reminder.getId(), reminder.getPetId(), recipient, deliveryWindow, now));
            if (delivery.getId() != null) {
                if (!delivery.canRetry(properties.getCollaboration().getReminderMaxRetries())) continue;
                delivery.prepareRetry(now);
            } else if (!deliveryRepository.createPendingIfAbsent(delivery)) {
                continue;
            }
            try {
                push(reminder, kind, recipient);
                delivery.markSent(null, now);
            } catch (RuntimeException ex) {
                delivery.markFailed("通知适配器异常", now);
                log.error("共享提醒推送失败: reminderId={}, recipientUserId={}", reminder.getId(), recipient, ex);
            }
            deliveryRepository.save(delivery);
        }
        reminderRepository.updateLastPushTime(reminder.getId(), now);
    }

    /**
     * 组装推送内容并发送
     *
     * 微信订阅消息接入点：小程序发布流程就绪后，在此替换/追加真实发送
     * （失败仅告警，不影响扫描主流程，见实施文档 8.3）。
     */
    private void push(Reminder reminder, String kind, Long recipientUserId) {
        String petName = petRepository.findById(reminder.getPetId())
                .map(Pet::getName)
                .orElse(null);
        // 宠物已删除（理论不该发生——删除宠物级联删提醒）则降级用标题
        String content = petName != null
                ? petName + "：" + reminder.getTitle()
                : reminder.getTitle();

        log.info("[提醒推送-{}] reminderId={}, petId={}, recipientUserId={}, type={}, content={}, remindTime={}",
                kind, reminder.getId(), reminder.getPetId(),
                recipientUserId, reminder.getType().getCode(), content, reminder.getNextRemindTime());
    }
}
