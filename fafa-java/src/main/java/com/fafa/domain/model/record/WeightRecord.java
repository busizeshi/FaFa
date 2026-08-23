package com.fafa.domain.model.record;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 体重记录（周期测量）
 *
 * 独立入口，不入每日打卡；同日可多条。逻辑删除。
 *
 * 业务规则：
 * 1. petId 与 recordDate 必填
 * 2. weight 必须为正数（kg），与 pet.weight 精度一致
 * 3. notes 最长 500 字符
 *
 * @author FaFa Team
 * @since 2.0
 */
@Getter
public class WeightRecord {

    private static final int MAX_NOTES_LENGTH = 500;

    private Long id;
    private final Long petId;
    private final LocalDate recordDate;
    /** 体重（kg） */
    private final BigDecimal weight;
    private final String notes;
    private final Long operatorId;
    private boolean deleted;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private WeightRecord(Long id, Long petId, LocalDate recordDate, BigDecimal weight,
                         String notes, Long operatorId, boolean deleted,
                         LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.petId = petId;
        this.recordDate = recordDate;
        this.weight = weight;
        this.notes = notes;
        this.operatorId = operatorId;
        this.deleted = deleted;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 创建体重记录
     */
    public static WeightRecord create(Long petId, LocalDate recordDate, BigDecimal weight,
                                      String notes, Long operatorId) {
        Objects.requireNonNull(petId, "petId 不能为空");
        Objects.requireNonNull(recordDate, "recordDate 不能为空");
        if (weight == null || weight.signum() <= 0) {
            throw new IllegalArgumentException("体重必须为正数");
        }
        if (notes != null && notes.length() > MAX_NOTES_LENGTH) {
            throw new IllegalArgumentException("备注不能超过 " + MAX_NOTES_LENGTH + " 字符");
        }

        return new WeightRecord(null, petId, recordDate, weight, notes, operatorId,
                false, null, null);
    }

    /**
     * 仓储重建
     */
    public static WeightRecord restore(Long id, Long petId, LocalDate recordDate, BigDecimal weight,
                                       String notes, Long operatorId, boolean deleted,
                                       LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new WeightRecord(id, petId, recordDate, weight, notes, operatorId,
                deleted, createdAt, updatedAt);
    }

    /**
     * 较上一条记录的变化值（本次 - 上次），趋势接口复用
     *
     * @param previous 时间上更早的一条记录，可为 null（首条返回 null）
     */
    public BigDecimal getWeightChange(WeightRecord previous) {
        if (previous == null) {
            return null;
        }
        return this.weight.subtract(previous.weight);
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
}
