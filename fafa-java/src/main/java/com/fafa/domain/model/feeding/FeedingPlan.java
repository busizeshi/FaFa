package com.fafa.domain.model.feeding;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 喂食计划聚合根。 */
@Getter
public class FeedingPlan {

    private Long id;
    private final Long userId;
    private final Long petId;
    private String name;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private BigDecimal dailyTargetCalories;
    private BigDecimal dailyTargetAmount;
    private String unit;
    private String recommendationSource;
    private String policyVersion;
    private int version;
    private FeedingPlanStatus status;
    private LocalDateTime acceptedAt;
    private String notes;
    private final List<FeedingPlanItem> items;
    private boolean deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private FeedingPlan(Long id, Long userId, Long petId, String name, LocalDate startDate, LocalDate endDate,
                        BigDecimal dailyTargetCalories, BigDecimal dailyTargetAmount, String unit,
                        String recommendationSource, String policyVersion, int version,
                        FeedingPlanStatus status, LocalDateTime acceptedAt, String notes,
                        List<FeedingPlanItem> items, boolean deleted, LocalDateTime createdAt,
                        LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.petId = petId;
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.dailyTargetCalories = dailyTargetCalories;
        this.dailyTargetAmount = dailyTargetAmount;
        this.unit = unit;
        this.recommendationSource = recommendationSource;
        this.policyVersion = policyVersion;
        this.version = version;
        this.status = status;
        this.acceptedAt = acceptedAt;
        this.notes = notes;
        this.items = List.copyOf(items);
        this.deleted = deleted;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static FeedingPlan create(Long userId, Long petId, String name, LocalDate startDate,
                                     LocalDate endDate, BigDecimal dailyTargetCalories,
                                     BigDecimal dailyTargetAmount, String unit, String recommendationSource,
                                     String policyVersion, List<FeedingPlanItem> items) {
        return create(userId, petId, name, startDate, endDate, dailyTargetCalories, dailyTargetAmount,
                unit, recommendationSource, policyVersion, null, items);
    }

    public static FeedingPlan create(Long userId, Long petId, String name, LocalDate startDate,
                                     LocalDate endDate, BigDecimal dailyTargetCalories,
                                     BigDecimal dailyTargetAmount, String unit, String recommendationSource,
                                     String policyVersion, String notes, List<FeedingPlanItem> items) {
        validateBase(userId, petId, startDate, endDate, dailyTargetCalories, dailyTargetAmount, unit, items);
        return new FeedingPlan(null, userId, petId, normalize(name), startDate, endDate,
                dailyTargetCalories, dailyTargetAmount, normalizeUnit(unit), recommendationSource,
                policyVersion, 1, FeedingPlanStatus.ACTIVE, LocalDateTime.now(), notes, items,
                false, null, null);
    }

    public static FeedingPlan restore(Long id, Long userId, Long petId, String name, LocalDate startDate,
                                      LocalDate endDate, BigDecimal dailyTargetCalories,
                                      BigDecimal dailyTargetAmount, String unit, String recommendationSource,
                                      String policyVersion, int version, FeedingPlanStatus status,
                                      LocalDateTime acceptedAt, String notes, List<FeedingPlanItem> items,
                                      boolean deleted, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new FeedingPlan(id, userId, petId, name, startDate, endDate, dailyTargetCalories,
                dailyTargetAmount, unit, recommendationSource, policyVersion, version, status,
                acceptedAt, notes, items, deleted, createdAt, updatedAt);
    }

    public void pause() {
        requireStatus(FeedingPlanStatus.ACTIVE);
        status = FeedingPlanStatus.PAUSED;
    }

    public void resume() {
        requireStatus(FeedingPlanStatus.PAUSED);
        status = FeedingPlanStatus.ACTIVE;
    }

    public void complete() {
        if (status != FeedingPlanStatus.ACTIVE && status != FeedingPlanStatus.PAUSED) {
            throw new IllegalStateException("喂食计划当前状态不可完成");
        }
        status = FeedingPlanStatus.COMPLETED;
    }

    public boolean appliesOn(LocalDate date) {
        return !date.isBefore(startDate) && (endDate == null || !date.isAfter(endDate))
                && status == FeedingPlanStatus.ACTIVE;
    }

    private void requireStatus(FeedingPlanStatus expected) {
        if (status != expected) throw new IllegalStateException("喂食计划当前状态不支持操作: " + status.getCode());
    }

    private static void validateBase(Long userId, Long petId, LocalDate startDate, LocalDate endDate,
                                     BigDecimal calories, BigDecimal amount, String unit,
                                     List<FeedingPlanItem> items) {
        if (userId == null || petId == null || startDate == null || items == null || items.isEmpty()) {
            throw new IllegalArgumentException("喂食计划基础参数无效");
        }
        if (endDate != null && endDate.isBefore(startDate)) throw new IllegalArgumentException("计划结束日期无效");
        if (calories != null && calories.signum() <= 0) throw new IllegalArgumentException("目标能量必须大于0");
        if (amount != null && amount.signum() <= 0) throw new IllegalArgumentException("目标总量必须大于0");
        if (unit != null && !unit.equalsIgnoreCase("g") && !unit.equalsIgnoreCase("ml")) {
            throw new IllegalArgumentException("计划单位仅支持 g 或 ml");
        }
        Set<String> keys = new HashSet<>();
        Set<String> times = new HashSet<>();
        for (FeedingPlanItem item : items) {
            if (!keys.add(item.itemKey()) || !times.add(item.mealTime().toString())) {
                throw new IllegalArgumentException("计划项键和时段不能重复");
            }
        }
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String normalizeUnit(String value) {
        return value == null || value.isBlank() ? "g" : value.trim().toLowerCase();
    }
}
