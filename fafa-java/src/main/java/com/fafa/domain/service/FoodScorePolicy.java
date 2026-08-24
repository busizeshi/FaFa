package com.fafa.domain.service;

import java.math.BigDecimal;

/** 食品提示分所需的不可变策略参数。 */
public record FoodScorePolicy(
        String version,
        int proteinSourcePartialScore,
        int additiveFlagPenalty,
        BigDecimal calciumPhosphorusMinRatio,
        BigDecimal calciumPhosphorusMaxRatio
) {
}
