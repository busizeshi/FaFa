package com.fafa.domain.model.food;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FoodCardTest {

    @Test
    void create_shouldKeepNutritionFacts() {
        FoodCard food = createFood(new BigDecimal("38"), List.of("鸡肉"), List.of("鸡肉", "鱼油"),
                new BigDecimal("1.2"), new BigDecimal("0.9"), "成猫");

        assertThat(food.getFoodType()).isEqualTo(FoodType.MAIN_FOOD);
        assertThat(food.getProteinPercent()).isEqualByComparingTo("38");
        assertThat(food.canBeUsedForNewPlan()).isTrue();
    }

    @Test
    void create_shouldRejectInvalidNutritionPercent() {
        assertThatThrownBy(() -> createFood(new BigDecimal("101"), List.of(), List.of(), null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("百分比");
    }

    @Test
    void archive_shouldPreventNewPlan() {
        FoodCard food = createFood(null, List.of(), List.of(), null, null, null);
        food.archive();
        assertThat(food.canBeUsedForNewPlan()).isFalse();
    }

    private FoodCard createFood(BigDecimal protein, List<String> sources, List<String> ingredients,
                                BigDecimal calcium, BigDecimal phosphorus, String lifeStage) {
        return FoodCard.create(3L, 1L, "鸡肉粮", null, FoodType.MAIN_FOOD,
                null, null, null, null, null, null, null, "per_100g", protein, null,
                null, null, null, calcium, phosphorus, null, null, sources, ingredients,
                List.of(), lifeStage, null, null, null);
    }
}
