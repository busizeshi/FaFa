package com.fafa.domain.service;

/** 健康规则所需的阈值，避免领域层依赖配置实现。 */
public record HealthRuleThresholds(
        int increasedWaterDays,
        int appetiteEnergyDeclineDays,
        int diarrheaDays,
        int missedMedicationCount) {

    public HealthRuleThresholds {
        if (increasedWaterDays < 1 || appetiteEnergyDeclineDays < 1
                || diarrheaDays < 1 || missedMedicationCount < 1) {
            throw new IllegalArgumentException("健康规则阈值必须为正数");
        }
    }
}
