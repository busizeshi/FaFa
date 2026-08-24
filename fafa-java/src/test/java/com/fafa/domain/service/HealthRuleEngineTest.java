package com.fafa.domain.service;

import com.fafa.domain.model.health.HealthAlertDraft;
import com.fafa.domain.model.health.HealthRuleContext;
import com.fafa.domain.model.record.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HealthRuleEngineTest {

    private final HealthRuleEngine engine = new HealthRuleEngine(new HealthRuleThresholds(3, 2, 2, 2));

    @Test
    void evaluate_shouldDetectRefusalAndVomitingAsRedAlert() {
        DailyStatus status = status(LocalDate.of(2026, 8, 23), Appetite.REFUSED,
                Energy.NORMAL, WaterIntake.NORMAL, Defecation.NORMAL, Vomiting.ONCE);

        List<HealthAlertDraft> alerts = engine.evaluate(new HealthRuleContext(1L, 2L,
                status.getRecordDate(), status.getRecordDate(), List.of(status), List.of()));

        assertThat(alerts).anyMatch(item -> item.ruleCode().equals("REFUSAL_WITH_VOMITING")
                && item.severity().getCode().equals("red"));
    }

    @Test
    void evaluate_shouldRequireConsecutiveDays() {
        DailyStatus first = status(LocalDate.of(2026, 8, 22), Appetite.DECREASED,
                Energy.LETHARGIC, WaterIntake.NORMAL, Defecation.NORMAL, Vomiting.NONE);
        DailyStatus second = status(LocalDate.of(2026, 8, 23), Appetite.DECREASED,
                Energy.LETHARGIC, WaterIntake.NORMAL, Defecation.NORMAL, Vomiting.NONE);

        List<HealthAlertDraft> alerts = engine.evaluate(new HealthRuleContext(1L, 2L,
                first.getRecordDate(), second.getRecordDate(), List.of(first, second), List.of()));

        assertThat(alerts).anyMatch(item -> item.ruleCode().equals("APPETITE_ENERGY_DECLINE_2D"));
    }

    private DailyStatus status(LocalDate date, Appetite appetite, Energy energy,
                               WaterIntake water, Defecation defecation, Vomiting vomiting) {
        return DailyStatus.create(2L, date, appetite, energy, water, defecation, vomiting,
                Urination.NORMAL, null, null, null, 1L);
    }
}
