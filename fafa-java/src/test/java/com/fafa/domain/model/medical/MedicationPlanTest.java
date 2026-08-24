package com.fafa.domain.model.medical;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MedicationPlanTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 23);

    @Test
    void create_shouldAllowAsNeededWithoutFixedTimes() {
        MedicationPlan plan = MedicationPlan.create(1L, 2L, null, "益生菌", null,
                "肠胃护理", BigDecimal.ONE, "粒", MedicationFrequencyType.AS_NEEDED,
                null, List.of(), MealRelation.ANY, TODAY, null, null, TODAY);

        assertThat(plan.getStatus()).isEqualTo(MedicationPlanStatus.ACTIVE);
        assertThat(plan.getAdministrationTimes()).isEmpty();
    }

    @Test
    void create_shouldRejectFixedFrequencyWithoutTimes() {
        assertThatThrownBy(() -> MedicationPlan.create(1L, 2L, null, "药", null, null,
                BigDecimal.ONE, "片", MedicationFrequencyType.DAILY, 1, List.of(),
                MealRelation.ANY, TODAY, null, null, TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("执行时间");
    }

    @Test
    void pauseAndComplete_shouldRespectStateMachine() {
        MedicationPlan plan = MedicationPlan.create(1L, 2L, null, "药", null, null,
                BigDecimal.ONE, "片", MedicationFrequencyType.DAILY, 1,
                List.of(LocalTime.of(9, 0)), MealRelation.ANY, TODAY, null, null, TODAY);

        plan.pause();
        assertThat(plan.getStatus()).isEqualTo(MedicationPlanStatus.PAUSED);
        plan.complete();
        assertThat(plan.getStatus()).isEqualTo(MedicationPlanStatus.COMPLETED);
    }
}
