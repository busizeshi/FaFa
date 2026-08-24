package com.fafa.domain.model.health;

import java.time.LocalDate;
import java.util.List;

/** 规则计算结果，尚未持久化。 */
public record HealthAlertDraft(String ruleCode, HealthAlertSeverity severity, String title,
                               String message, List<HealthEvidence> evidence,
                               LocalDate observedFrom, LocalDate observedTo,
                               String fingerprint) {
}
