package com.fafa.domain.model.feeding;

import java.math.BigDecimal;
import java.time.LocalTime;

/** 喂食计划项。 */
public record FeedingPlanItem(
        Long id,
        String itemKey,
        Long foodId,
        LocalTime mealTime,
        BigDecimal amount,
        String unit,
        BigDecimal percentage,
        String cycleType,
        String cycleValue,
        String nutritionSnapshot,
        int sortOrder
) {
    public FeedingPlanItem {
        if (itemKey == null || itemKey.isBlank() || foodId == null || mealTime == null
                || amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("喂食计划项参数无效");
        }
        if (percentage != null && (percentage.signum() < 0
                || percentage.compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw new IllegalArgumentException("喂食比例必须在0到100之间");
        }
        if (unit == null || (!unit.equalsIgnoreCase("g") && !unit.equalsIgnoreCase("ml"))) {
            throw new IllegalArgumentException("喂食单位仅支持 g 或 ml");
        }
    }
}
