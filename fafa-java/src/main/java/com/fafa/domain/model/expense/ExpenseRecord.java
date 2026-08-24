package com.fafa.domain.model.expense;

import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/** 开支记录聚合根。医疗来源记录由医疗档案同步，手工来源记录由用户维护。 */
@Getter
public final class ExpenseRecord {

    private static final int MAX_TITLE_LENGTH = 100;
    private static final int MAX_NOTES_LENGTH = 500;
    private Long id;
    private final Long userId;
    private final Long petId;
    private ExpenseCategory category;
    private Money money;
    private LocalDate expenseDate;
    private String title;
    private String notes;
    private Long receiptMediaId;
    private Long foodCardId;
    private String foodNameSnapshot;
    private final Long medicalVisitId;
    private final ExpenseSourceType sourceType;
    private final boolean systemGenerated;
    private boolean deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private ExpenseRecord(Long id, Long userId, Long petId, ExpenseCategory category, Money money,
                          LocalDate expenseDate, String title, String notes, Long receiptMediaId,
                          Long foodCardId, String foodNameSnapshot, Long medicalVisitId,
                          ExpenseSourceType sourceType, boolean systemGenerated, boolean deleted,
                          LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.petId = petId;
        this.category = category;
        this.money = money;
        this.expenseDate = expenseDate;
        this.title = title;
        this.notes = notes;
        this.receiptMediaId = receiptMediaId;
        this.foodCardId = foodCardId;
        this.foodNameSnapshot = foodNameSnapshot;
        this.medicalVisitId = medicalVisitId;
        this.sourceType = sourceType;
        this.systemGenerated = systemGenerated;
        this.deleted = deleted;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ExpenseRecord createManual(Long userId, Long petId, ExpenseCategory category,
                                             Money money, LocalDate expenseDate, String title,
                                             String notes, Long receiptMediaId, Long foodCardId,
                                             String foodNameSnapshot, LocalDate today) {
        validateIdentity(userId, petId, category, money, expenseDate, today);
        return new ExpenseRecord(null, userId, petId, category, money, expenseDate,
                normalizeTitle(title), normalize(notes, MAX_NOTES_LENGTH, "备注"), receiptMediaId,
                foodCardId, normalize(foodNameSnapshot, MAX_TITLE_LENGTH, "食品名称"), null,
                ExpenseSourceType.MANUAL, false, false, null, null);
    }

    public static ExpenseRecord createFromMedicalVisit(Long userId, Long petId, Long medicalVisitId,
                                                       Money money, LocalDate expenseDate,
                                                       String title, String notes, LocalDate today) {
        validateIdentity(userId, petId, ExpenseCategory.MEDICAL, money, expenseDate, today);
        if (medicalVisitId == null || medicalVisitId <= 0) {
            throw new IllegalArgumentException("来源就医记录不能为空");
        }
        return new ExpenseRecord(null, userId, petId, ExpenseCategory.MEDICAL, money, expenseDate,
                normalizeTitle(title), normalize(notes, MAX_NOTES_LENGTH, "备注"), null, null, null,
                medicalVisitId, ExpenseSourceType.MEDICAL_VISIT, true, false, null, null);
    }

    public static ExpenseRecord restore(Long id, Long userId, Long petId, ExpenseCategory category,
                                        Money money, LocalDate expenseDate, String title, String notes,
                                        Long receiptMediaId, Long foodCardId, String foodNameSnapshot,
                                        Long medicalVisitId, ExpenseSourceType sourceType,
                                        boolean systemGenerated, boolean deleted,
                                        LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new ExpenseRecord(id, userId, petId, category, money, expenseDate, title, notes,
                receiptMediaId, foodCardId, foodNameSnapshot, medicalVisitId, sourceType,
                systemGenerated, deleted, createdAt, updatedAt);
    }

    public void updateManual(ExpenseCategory category, Money money, LocalDate expenseDate,
                             String title, String notes, Long receiptMediaId, Long foodCardId,
                             String foodNameSnapshot, LocalDate today) {
        if (!isEditableByUser()) {
            throw new IllegalStateException("自动生成的医疗开支不可编辑");
        }
        validateIdentity(userId, petId, category, money, expenseDate, today);
        this.category = category;
        this.money = money;
        this.expenseDate = expenseDate;
        this.title = normalizeTitle(title);
        this.notes = normalize(notes, MAX_NOTES_LENGTH, "备注");
        this.receiptMediaId = receiptMediaId;
        this.foodCardId = foodCardId;
        this.foodNameSnapshot = normalize(foodNameSnapshot, MAX_TITLE_LENGTH, "食品名称");
    }

    public void syncMedicalVisit(Money money, LocalDate expenseDate, String title, String notes,
                                 LocalDate today) {
        if (sourceType != ExpenseSourceType.MEDICAL_VISIT || !systemGenerated) {
            throw new IllegalStateException("只有医疗来源开支可以同步");
        }
        validateIdentity(userId, petId, ExpenseCategory.MEDICAL, money, expenseDate, today);
        this.money = money;
        this.expenseDate = expenseDate;
        this.title = normalizeTitle(title);
        this.notes = normalize(notes, MAX_NOTES_LENGTH, "备注");
        this.category = ExpenseCategory.MEDICAL;
    }

    public void restoreDeleted() {
        if (sourceType != ExpenseSourceType.MEDICAL_VISIT) {
            throw new IllegalStateException("手工开支不支持自动恢复");
        }
        this.deleted = false;
    }

    public void delete() {
        if (sourceType != ExpenseSourceType.MANUAL) {
            throw new IllegalStateException("自动生成的医疗开支不可删除");
        }
        this.deleted = true;
    }

    public boolean isEditableByUser() {
        return !deleted && sourceType == ExpenseSourceType.MANUAL;
    }

    private static void validateIdentity(Long userId, Long petId, ExpenseCategory category,
                                         Money money, LocalDate expenseDate, LocalDate today) {
        if (userId == null || userId <= 0 || petId == null || petId <= 0) {
            throw new IllegalArgumentException("用户和宠物不能为空");
        }
        Objects.requireNonNull(category, "开支分类不能为空");
        Objects.requireNonNull(money, "金额不能为空");
        Objects.requireNonNull(expenseDate, "发生日期不能为空");
        Objects.requireNonNull(today, "当前日期不能为空");
        if (expenseDate.isAfter(today)) {
            throw new IllegalArgumentException("开支日期不能晚于今天");
        }
    }

    private static String normalizeTitle(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("开支标题不能为空");
        }
        return normalize(value, MAX_TITLE_LENGTH, "标题");
    }

    private static String normalize(String value, int maxLength, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(field + "不能超过 " + maxLength + " 字符");
        }
        return normalized;
    }
}
