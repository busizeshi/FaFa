package com.fafa.domain.model.food;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FoodTransitionPlanTest {

    @Test
    void create_shouldRejectNonContinuousRatios() {
        assertThatThrownBy(() -> FoodTransitionPlan.create(3L, 1L, 10L, 11L,
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 2), new BigDecimal("80"), "g",
                List.of(new FoodTransitionPlan.RatioStep(LocalDate.of(2026, 8, 1),
                        new BigDecimal("50"), new BigDecimal("50"))), FoodTransitionStatus.ACTIVE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("覆盖每天");
    }

    @Test
    void create_shouldRejectRatioSumNot100() {
        assertThatThrownBy(() -> FoodTransitionPlan.create(3L, 1L, 10L, 11L,
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 1), null, "g",
                List.of(new FoodTransitionPlan.RatioStep(LocalDate.of(2026, 8, 1),
                        new BigDecimal("40"), new BigDecimal("40"))), FoodTransitionStatus.ACTIVE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("之和");
    }
}
