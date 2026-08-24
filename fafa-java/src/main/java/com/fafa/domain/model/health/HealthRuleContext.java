package com.fafa.domain.model.health;

import com.fafa.domain.model.medical.MedicationRecord;
import com.fafa.domain.model.record.DailyStatus;

import java.time.LocalDate;
import java.util.List;

/** 健康规则消费的只读事实上下文。 */
public record HealthRuleContext(Long userId, Long petId, LocalDate windowStart,
                                LocalDate windowEnd, List<DailyStatus> dailyStatuses,
                                List<MedicationRecord> medicationRecords) {
}
