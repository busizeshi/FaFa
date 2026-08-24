package com.fafa.infrastructure.config;

import com.fafa.domain.service.HealthRuleThresholds;
import com.fafa.domain.service.HealthRuleEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 将基础设施配置转换为领域规则所需的不可变参数。 */
@Configuration
public class HealthRuleConfig {

    @Bean
    public HealthRuleThresholds healthRuleThresholds(FaFaProperties properties) {
        FaFaProperties.HealthAlert config = properties.getHealthAlert();
        return new HealthRuleThresholds(config.getIncreasedWaterDays(),
                config.getAppetiteEnergyDeclineDays(), config.getDiarrheaDays(),
                config.getMissedMedicationCount());
    }

    @Bean
    public HealthRuleEngine healthRuleEngine(HealthRuleThresholds thresholds) {
        return new HealthRuleEngine(thresholds);
    }
}
