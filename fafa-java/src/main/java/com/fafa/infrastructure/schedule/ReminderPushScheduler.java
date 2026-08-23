package com.fafa.infrastructure.schedule;

import com.fafa.application.service.ReminderPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 提醒推送定时任务
 *
 * 每小时整点扫描：到期提醒（含逾期）+ 明日重要提醒（疫苗/体检/驱虫提前 1 天）。
 * 同一提醒每日至多推送一次（last_push_time 去重，见 ReminderPushService）。
 *
 * @author FaFa Team
 * @since 3.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderPushScheduler {

    private final ReminderPushService reminderPushService;

    @Scheduled(cron = "0 0 * * * *")
    public void scanAndPush() {
        try {
            reminderPushService.scanAndPush();
        } catch (Exception ex) {
            // 调度异常不抛出，等下一轮
            log.error("提醒扫描推送异常", ex);
        }
    }
}
