package com.fafa.infrastructure.scheduler;

import com.fafa.application.service.ContentScheduleApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContentScheduleScheduler {
    private final ContentScheduleApplicationService scheduleService;

    @Scheduled(cron = "${fafa.report.schedule-scan-cron:0 */10 * * * *}")
    public void scan() {
        try { scheduleService.runDueSchedules(); }
        catch (RuntimeException ex) { log.error("扫描内容周期配置失败", ex); }
    }
}
