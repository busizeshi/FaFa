package com.fafa.domain.model.food;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 换粮过渡计划聚合根。 */
@Getter
public class FoodTransitionPlan {

    private Long id;
    private final Long userId;
    private final Long petId;
    private final Long oldFoodId;
    private final Long newFoodId;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final BigDecimal dailyTargetAmount;
    private final String unit;
    private final List<RatioStep> ratioSteps;
    private LocalDate reactionDate;
    private String reactionCode;
    private String reactionNote;
    private FoodTransitionStatus status;
    private boolean deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private FoodTransitionPlan(Long id, Long userId, Long petId, Long oldFoodId, Long newFoodId,
                               LocalDate startDate, LocalDate endDate, BigDecimal dailyTargetAmount,
                               String unit, List<RatioStep> ratioSteps, LocalDate reactionDate, String reactionCode,
                               String reactionNote, FoodTransitionStatus status, boolean deleted,
                               LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.petId = petId;
        this.oldFoodId = oldFoodId;
        this.newFoodId = newFoodId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.dailyTargetAmount = dailyTargetAmount;
        this.unit = unit;
        this.ratioSteps = List.copyOf(ratioSteps);
        this.reactionDate = reactionDate;
        this.reactionCode = reactionCode;
        this.reactionNote = reactionNote;
        this.status = status;
        this.deleted = deleted;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static FoodTransitionPlan create(Long userId, Long petId, Long oldFoodId, Long newFoodId,
                                            LocalDate startDate, LocalDate endDate,
                                            BigDecimal dailyTargetAmount, String unit,
                                            List<RatioStep> ratioSteps, FoodTransitionStatus status) {
        if (userId == null || petId == null || oldFoodId == null || newFoodId == null
                || oldFoodId.equals(newFoodId) || startDate == null || endDate == null
                || startDate.isAfter(endDate) || ratioSteps == null || ratioSteps.isEmpty()) {
            throw new IllegalArgumentException("换粮计划基础参数无效");
        }
        if (dailyTargetAmount != null && dailyTargetAmount.signum() < 0) {
            throw new IllegalArgumentException("每日目标量不能为负");
        }
        validateUnit(unit);
        validateSteps(startDate, endDate, ratioSteps);
        return new FoodTransitionPlan(null, userId, petId, oldFoodId, newFoodId, startDate, endDate,
                dailyTargetAmount, normalizeUnit(unit), ratioSteps, null, null, null,
                status == null ? FoodTransitionStatus.DRAFT : status, false, null, null);
    }

    public static FoodTransitionPlan restore(Long id, Long userId, Long petId, Long oldFoodId,
                                             Long newFoodId, LocalDate startDate, LocalDate endDate,
                                             BigDecimal dailyTargetAmount, String unit,
                                             List<RatioStep> ratioSteps, LocalDate reactionDate, String reactionCode,
                                             String reactionNote, FoodTransitionStatus status,
                                             boolean deleted, LocalDateTime createdAt,
                                             LocalDateTime updatedAt) {
        return new FoodTransitionPlan(id, userId, petId, oldFoodId, newFoodId, startDate, endDate,
                dailyTargetAmount, unit, ratioSteps, reactionDate, reactionCode, reactionNote, status, deleted,
                createdAt, updatedAt);
    }

    public void updateReaction(LocalDate reactionDate, String reactionCode, String reactionNote) {
        if (reactionDate == null || reactionDate.isBefore(startDate) || reactionDate.isAfter(endDate)) {
            throw new IllegalArgumentException("反应日期不在换粮计划范围内");
        }
        if (reactionCode == null || reactionCode.isBlank()) {
            throw new IllegalArgumentException("反应不能为空");
        }
        if (reactionNote != null && reactionNote.length() > 500) {
            throw new IllegalArgumentException("反应备注不能超过500字符");
        }
        this.reactionDate = reactionDate;
        this.reactionCode = reactionCode.trim();
        this.reactionNote = reactionNote;
    }

    public void complete() {
        requireActive();
        status = FoodTransitionStatus.COMPLETED;
    }

    public void cancel() {
        if (status == FoodTransitionStatus.COMPLETED) {
            throw new IllegalStateException("已完成的换粮计划不能取消");
        }
        status = FoodTransitionStatus.CANCELLED;
    }

    private void requireActive() {
        if (status != FoodTransitionStatus.ACTIVE) {
            throw new IllegalStateException("换粮计划当前状态不可操作: " + status.getCode());
        }
    }

    private static void validateSteps(LocalDate start, LocalDate end, List<RatioStep> steps) {
        if (steps.size() != java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1) {
            throw new IllegalArgumentException("换粮比例必须覆盖每天");
        }
        LocalDate current = start;
        for (RatioStep step : steps) {
            if (!current.equals(step.date())) {
                throw new IllegalArgumentException("换粮比例日期必须连续");
            }
            if (step.oldPercent().add(step.newPercent()).compareTo(BigDecimal.valueOf(100)) != 0) {
                throw new IllegalArgumentException("每天新旧食品比例之和必须为100");
            }
            if (step.oldPercent().signum() < 0 || step.newPercent().signum() < 0
                    || step.oldPercent().compareTo(BigDecimal.valueOf(100)) > 0
                    || step.newPercent().compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new IllegalArgumentException("换粮比例必须在0到100之间");
            }
            current = current.plusDays(1);
        }
    }

    private static void validateUnit(String unit) {
        if (unit != null && !unit.equalsIgnoreCase("g") && !unit.equalsIgnoreCase("ml")) {
            throw new IllegalArgumentException("单位仅支持 g 或 ml");
        }
    }

    private static String normalizeUnit(String unit) {
        return unit == null || unit.isBlank() ? "g" : unit.trim().toLowerCase();
    }

    public record RatioStep(LocalDate date, BigDecimal oldPercent, BigDecimal newPercent) {
        public RatioStep {
            if (date == null || oldPercent == null || newPercent == null) {
                throw new IllegalArgumentException("比例步骤不能为空");
            }
        }
    }
}
