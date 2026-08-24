package com.fafa.domain.model.record;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 喂食记录（照护动作，事件流）
 *
 * 一天可多条，精确到分钟。逻辑删除。
 *
 * 业务规则：
 * 1. petId 与 feedingTime 必填，foodType 必填
 * 2. amount 不可为负；unit 仅支持 g/ml
 * 3. notes 最长 500 字符
 *
 * @author FaFa Team
 * @since 2.0
 */
@Getter
public class FeedingRecord {

    private static final int MAX_NOTES_LENGTH = 500;
    public static final String UNIT_GRAM = "g";
    public static final String UNIT_MILLILITER = "ml";

    private Long id;
    private final Long petId;
    private LocalDateTime feedingTime;
    private FoodType foodType;
    /** 喂食量数值（选填） */
    private BigDecimal amount;
    /** 单位：g/ml */
    private String unit;
    /** 关联食品管理卡ID（Phase 7 预留） */
    private Long foodId;
    /** 关联计划，可空；手工喂食不需要填写。 */
    private Long feedingPlanId;
    private Long feedingPlanItemId;
    private LocalDateTime plannedAt;
    private String foodNameSnapshot;
    private String notes;
    private final Long operatorId;
    private boolean deleted;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private FeedingRecord(Long id, Long petId, LocalDateTime feedingTime, FoodType foodType,
                          BigDecimal amount, String unit, Long foodId, Long feedingPlanId,
                          Long feedingPlanItemId, LocalDateTime plannedAt, String foodNameSnapshot, String notes,
                          Long operatorId, boolean deleted,
                          LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.petId = petId;
        this.feedingTime = feedingTime;
        this.foodType = foodType;
        this.amount = amount;
        this.unit = unit;
        this.foodId = foodId;
        this.feedingPlanId = feedingPlanId;
        this.feedingPlanItemId = feedingPlanItemId;
        this.plannedAt = plannedAt;
        this.foodNameSnapshot = foodNameSnapshot;
        this.notes = notes;
        this.operatorId = operatorId;
        this.deleted = deleted;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 创建喂食记录
     */
    public static FeedingRecord create(Long petId, LocalDateTime feedingTime, FoodType foodType,
                                       BigDecimal amount, String unit, Long foodId,
                                       String notes, Long operatorId) {
        return create(petId, feedingTime, foodType, amount, unit, foodId, null, null, null, null,
                notes, operatorId);
    }

    public static FeedingRecord create(Long petId, LocalDateTime feedingTime, FoodType foodType,
                                       BigDecimal amount, String unit, Long foodId, Long feedingPlanId,
                                       Long feedingPlanItemId, LocalDateTime plannedAt,
                                       String foodNameSnapshot, String notes, Long operatorId) {
        Objects.requireNonNull(petId, "petId 不能为空");
        Objects.requireNonNull(feedingTime, "feedingTime 不能为空");
        Objects.requireNonNull(foodType, "foodType 不能为空");
        validateAmount(amount);
        validateNotes(notes);

        return new FeedingRecord(null, petId, feedingTime, foodType,
                amount, normalizeUnit(unit), foodId, feedingPlanId, feedingPlanItemId, plannedAt,
                foodNameSnapshot, notes, operatorId,
                false, null, null);
    }

    /**
     * 仓储重建
     */
    public static FeedingRecord restore(Long id, Long petId, LocalDateTime feedingTime, FoodType foodType,
                                        BigDecimal amount, String unit, Long foodId, String notes,
                                        Long operatorId, boolean deleted,
                                        LocalDateTime createdAt, LocalDateTime updatedAt) {
        return restore(id, petId, feedingTime, foodType, amount, unit, foodId, null, null, null, null,
                notes, operatorId, deleted, createdAt, updatedAt);
    }

    public static FeedingRecord restore(Long id, Long petId, LocalDateTime feedingTime, FoodType foodType,
                                        BigDecimal amount, String unit, Long foodId, Long feedingPlanId,
                                        Long feedingPlanItemId, LocalDateTime plannedAt,
                                        String foodNameSnapshot, String notes, Long operatorId,
                                        boolean deleted, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new FeedingRecord(id, petId, feedingTime, foodType,
                amount, unit, foodId, feedingPlanId, feedingPlanItemId, plannedAt, foodNameSnapshot,
                notes, operatorId, deleted, createdAt, updatedAt);
    }

    /**
     * 覆盖更新
     */
    public void update(LocalDateTime feedingTime, FoodType foodType,
                       BigDecimal amount, String unit, Long foodId, String notes) {
        update(feedingTime, foodType, amount, unit, foodId, null, null, null, null, notes);
    }

    public void update(LocalDateTime feedingTime, FoodType foodType,
                       BigDecimal amount, String unit, Long foodId, Long feedingPlanId,
                       Long feedingPlanItemId, LocalDateTime plannedAt, String foodNameSnapshot,
                       String notes) {
        Objects.requireNonNull(feedingTime, "feedingTime 不能为空");
        Objects.requireNonNull(foodType, "foodType 不能为空");
        validateAmount(amount);
        validateNotes(notes);

        this.feedingTime = feedingTime;
        this.foodType = foodType;
        this.amount = amount;
        this.unit = normalizeUnit(unit);
        this.foodId = foodId;
        this.feedingPlanId = feedingPlanId;
        this.feedingPlanItemId = feedingPlanItemId;
        this.plannedAt = plannedAt;
        this.foodNameSnapshot = foodNameSnapshot;
        this.notes = notes;
    }

    /**
     * 逻辑删除
     */
    public void delete() {
        this.deleted = true;
    }

    /**
     * 仓储保存后回填 ID
     */
    public void assignId(Long id) {
        this.id = id;
    }

    public void assignCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void assignUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /** 单位归一化：空值默认 g，仅接受 g/ml */
    private static String normalizeUnit(String unit) {
        if (unit == null || unit.isBlank()) {
            return UNIT_GRAM;
        }
        String normalized = unit.trim().toLowerCase();
        if (!UNIT_GRAM.equals(normalized) && !UNIT_MILLILITER.equals(normalized)) {
            throw new IllegalArgumentException("单位仅支持 g 或 ml");
        }
        return normalized;
    }

    private static void validateAmount(BigDecimal amount) {
        if (amount != null && amount.signum() < 0) {
            throw new IllegalArgumentException("喂食量不能为负");
        }
    }

    private static void validateNotes(String notes) {
        if (notes != null && notes.length() > MAX_NOTES_LENGTH) {
            throw new IllegalArgumentException("备注不能超过 " + MAX_NOTES_LENGTH + " 字符");
        }
    }
}
