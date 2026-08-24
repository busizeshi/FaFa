package com.fafa.domain.service;

import com.fafa.domain.model.food.FoodCard;
import com.fafa.domain.model.food.FoodType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FoodSafetyScoreServiceTest {

    private final FoodSafetyScoreService service = new FoodSafetyScoreService(
            new FoodScorePolicy("v1", 12, 5, new BigDecimal("1.0"), new BigDecimal("2.0")));

    @Test
    void calculate_shouldReturnScoreWhenFourDimensionsHaveEvidence() {
        FoodCard food = FoodCard.create(3L, 1L, "鸡肉粮", null, FoodType.MAIN_FOOD,
                null, null, null, null, null, null, null, "per_100g", new BigDecimal("38"),
                null, null, null, null, new BigDecimal("1.2"), new BigDecimal("0.9"), null,
                null, List.of("鸡肉"), List.of("鸡肉"), List.of(), "成猫", null, null, null);

        var result = service.calculate(food);

        assertThat(result.score()).isEqualTo(100);
        assertThat(result.missingFields()).isEmpty();
    }

    @Test
    void calculate_shouldNotTreatMissingNutritionAsZeroScore() {
        FoodCard food = FoodCard.create(3L, 1L, "自制餐", null, FoodType.HOMEMADE,
                null, null, null, null, null, null, null, "unknown", null, null, null, null,
                null, null, null, null, null, List.of(), List.of(), List.of(), null, null, null, null);

        var result = service.calculate(food);

        assertThat(result.score()).isNull();
        assertThat(result.label()).isEqualTo("数据不足");
    }
}
