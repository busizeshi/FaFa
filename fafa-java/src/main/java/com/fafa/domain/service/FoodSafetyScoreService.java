package com.fafa.domain.service;

import com.fafa.domain.model.food.FoodCard;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 食品配方信息提示分服务。
 *
 * 该服务只根据食品卡已保存的字段计算可解释信息，不调用模型，也不宣称食品安全认证。
 */
@RequiredArgsConstructor
public class FoodSafetyScoreService {

    private static final int DIMENSION_MAX = 25;
    private static final String STATUS_KNOWN = "known";
    private static final String STATUS_PARTIAL = "partial";
    private static final String STATUS_UNKNOWN = "unknown";

    private final FoodScorePolicy policy;

    public FoodSafetyScore calculate(FoodCard foodCard) {
        List<Dimension> dimensions = List.of(
                proteinSource(foodCard), additive(foodCard), calciumPhosphorus(foodCard), lifeStage(foodCard));
        boolean complete = dimensions.stream().allMatch(item -> !STATUS_UNKNOWN.equals(item.status()));
        Integer score = complete ? dimensions.stream().mapToInt(Dimension::score).sum() : null;
        List<String> missing = dimensions.stream().filter(item -> STATUS_UNKNOWN.equals(item.status()))
                .map(Dimension::label).toList();
        List<String> warnings = new ArrayList<>();
        if (!complete) {
            warnings.add("营养信息不完整，暂不生成总分");
        }
        warnings.add("该结果是配方信息提示，不是食品安全认证或医疗建议");
        return new FoodSafetyScore(score, complete ? "信息较完整" : "数据不足",
                policy.version(), dimensions, missing, warnings);
    }

    private Dimension proteinSource(FoodCard food) {
        if (food.getProteinSources() != null && !food.getProteinSources().isEmpty()) {
            return new Dimension("protein_source", "蛋白来源清晰度", DIMENSION_MAX, DIMENSION_MAX,
                    STATUS_KNOWN, food.getProteinSources());
        }
        if (food.getIngredients() != null && !food.getIngredients().isEmpty()) {
            return new Dimension("protein_source", "蛋白来源清晰度",
                    policy.proteinSourcePartialScore(), DIMENSION_MAX,
                    STATUS_PARTIAL, List.of("已录入配料表，但未确认明确蛋白来源"));
        }
        return unknown("protein_source", "蛋白来源清晰度");
    }

    private Dimension additive(FoodCard food) {
        if (food.getIngredients() == null || food.getIngredients().isEmpty()) {
            return unknown("additives", "添加物提示");
        }
        List<String> flags = food.getAdditiveFlags() == null ? List.of() : food.getAdditiveFlags();
        int score = Math.max(0, DIMENSION_MAX - flags.size() * policy.additiveFlagPenalty());
        return new Dimension("additives", "添加物提示", score, DIMENSION_MAX,
                flags.isEmpty() ? STATUS_KNOWN : STATUS_PARTIAL,
                flags.isEmpty() ? List.of() : flags);
    }

    private Dimension calciumPhosphorus(FoodCard food) {
        if (food.getCalciumPercent() == null || food.getPhosphorusPercent() == null
                || food.getPhosphorusPercent().signum() <= 0) {
            return unknown("calcium_phosphorus", "钙磷信息");
        }
        BigDecimal ratio = food.getCalciumPercent().divide(food.getPhosphorusPercent(), 2, RoundingMode.HALF_UP);
        BigDecimal min = policy.calciumPhosphorusMinRatio();
        BigDecimal max = policy.calciumPhosphorusMaxRatio();
        boolean inRange = ratio.compareTo(min) >= 0 && ratio.compareTo(max) <= 0;
        return new Dimension("calcium_phosphorus", "钙磷信息", inRange ? DIMENSION_MAX : 12,
                DIMENSION_MAX, inRange ? STATUS_KNOWN : STATUS_PARTIAL,
                List.of("Ca/P=" + ratio.toPlainString()));
    }

    private Dimension lifeStage(FoodCard food) {
        if (food.getLifeStageLabel() == null || food.getLifeStageLabel().isBlank()) {
            return unknown("life_stage", "阶段标注");
        }
        return new Dimension("life_stage", "阶段标注", DIMENSION_MAX, DIMENSION_MAX,
                STATUS_KNOWN, List.of(food.getLifeStageLabel()));
    }

    private Dimension unknown(String key, String label) {
        return new Dimension(key, label, 0, DIMENSION_MAX, STATUS_UNKNOWN, List.of());
    }

    public record FoodSafetyScore(Integer score, String label, String policyVersion,
                                  List<Dimension> dimensions, List<String> missingFields,
                                  List<String> warnings) {
    }

    public record Dimension(String key, String label, int score, int maxScore,
                            String status, List<String> evidence) {
    }
}
