package com.fafa.infrastructure.scheduler;

import com.fafa.application.service.HealthAlertApplicationService;
import com.fafa.domain.repository.PetRepository;
import com.fafa.infrastructure.config.FaFaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/** 定期扫描每日状态和用药记录，生成或关闭规则预警。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HealthAlertScheduler {

    private final PetRepository petRepository;
    private final HealthAlertApplicationService healthAlertApplicationService;
    private final FaFaProperties properties;

    @Scheduled(cron = "${fafa.health-alert.scan-cron:0 */30 * * * *}")
    public void scan() {
        if (!properties.getHealthAlert().isEnabled()) {
            return;
        }
        petRepository.findAllActive().forEach(pet -> {
            try {
                healthAlertApplicationService.evaluateForUser(pet.getUserId(), pet.getId(), LocalDate.now());
            } catch (RuntimeException ex) {
                log.error("健康预警扫描失败: petId={}", pet.getId(), ex);
            }
        });
    }
}
