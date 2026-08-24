package com.fafa.domain.model.feeding;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FeedingPlanTest {

    @Test
    void create_shouldStartActiveAndKeepItems() {
        FeedingPlanItem item = new FeedingPlanItem(null, "meal-1", 10L, LocalTime.of(8, 0),
                new BigDecimal("30"), "g", null, "daily", null, null, 0);
        FeedingPlan plan = FeedingPlan.create(3L, 1L, "日常计划", LocalDate.of(2026, 8, 1), null,
                null, new BigDecimal("30"), "g", "user_input", "v1", List.of(item));

        assertThat(plan.getStatus()).isEqualTo(FeedingPlanStatus.ACTIVE);
        assertThat(plan.appliesOn(LocalDate.of(2026, 8, 2))).isTrue();
    }

    @Test
    void create_shouldRejectDuplicateMealTime() {
        FeedingPlanItem first = new FeedingPlanItem(null, "meal-1", 10L, LocalTime.of(8, 0),
                new BigDecimal("30"), "g", null, "daily", null, null, 0);
        FeedingPlanItem second = new FeedingPlanItem(null, "meal-2", 11L, LocalTime.of(8, 0),
                new BigDecimal("20"), "g", null, "daily", null, null, 1);

        assertThatThrownBy(() -> FeedingPlan.create(3L, 1L, null, LocalDate.of(2026, 8, 1), null,
                null, null, "g", "user_input", "v1", List.of(first, second)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不能重复");
    }
}
