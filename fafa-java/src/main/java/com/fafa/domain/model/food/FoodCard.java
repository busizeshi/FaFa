package com.fafa.domain.model.food;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 食品管理卡聚合根。
 *
 * AI 识别结果只能通过应用层确认后进入该聚合，聚合本身只保证食品事实的字段不变式。
 */
@Getter
public class FoodCard {

    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_TEXT_LENGTH = 100;
    private static final int MAX_NOTES_LENGTH = 500;

    private Long id;
    private final Long userId;
    private final Long petId;
    private String name;
    private String brand;
    private FoodType foodType;
    private String specification;
    private BigDecimal packageWeight;
    private String packageWeightUnit;
    private BigDecimal price;
    private String currency;
    private LocalDate openedAt;
    private LocalDate expiresAt;
    private String nutritionBasis;
    private BigDecimal proteinPercent;
    private BigDecimal fatPercent;
    private BigDecimal fiberPercent;
    private BigDecimal moisturePercent;
    private BigDecimal ashPercent;
    private BigDecimal calciumPercent;
    private BigDecimal phosphorusPercent;
    private BigDecimal energyKcal;
    private String energyUnit;
    private List<String> proteinSources;
    private List<String> ingredients;
    private List<String> additiveFlags;
    private String lifeStageLabel;
    private Long sourceMediaId;
    private Long sourceRecognitionTaskId;
    private FoodStatus status;
    private String notes;
    private boolean deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private FoodCard(Long id, Long userId, Long petId, String name, String brand, FoodType foodType,
                     String specification, BigDecimal packageWeight, String packageWeightUnit,
                     BigDecimal price, String currency, LocalDate openedAt, LocalDate expiresAt,
                     String nutritionBasis, BigDecimal proteinPercent, BigDecimal fatPercent,
                     BigDecimal fiberPercent, BigDecimal moisturePercent, BigDecimal ashPercent,
                     BigDecimal calciumPercent, BigDecimal phosphorusPercent, BigDecimal energyKcal,
                     String energyUnit, List<String> proteinSources, List<String> ingredients,
                     List<String> additiveFlags, String lifeStageLabel, Long sourceMediaId,
                     Long sourceRecognitionTaskId, FoodStatus status, String notes, boolean deleted,
                     LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.petId = petId;
        this.name = name;
        this.brand = brand;
        this.foodType = foodType;
        this.specification = specification;
        this.packageWeight = packageWeight;
        this.packageWeightUnit = packageWeightUnit;
        this.price = price;
        this.currency = currency;
        this.openedAt = openedAt;
        this.expiresAt = expiresAt;
        this.nutritionBasis = nutritionBasis;
        this.proteinPercent = proteinPercent;
        this.fatPercent = fatPercent;
        this.fiberPercent = fiberPercent;
        this.moisturePercent = moisturePercent;
        this.ashPercent = ashPercent;
        this.calciumPercent = calciumPercent;
        this.phosphorusPercent = phosphorusPercent;
        this.energyKcal = energyKcal;
        this.energyUnit = energyUnit;
        this.proteinSources = immutableList(proteinSources);
        this.ingredients = immutableList(ingredients);
        this.additiveFlags = immutableList(additiveFlags);
        this.lifeStageLabel = lifeStageLabel;
        this.sourceMediaId = sourceMediaId;
        this.sourceRecognitionTaskId = sourceRecognitionTaskId;
        this.status = status;
        this.notes = notes;
        this.deleted = deleted;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static FoodCard create(Long userId, Long petId, String name, String brand, FoodType foodType,
                                  String specification, BigDecimal packageWeight, String packageWeightUnit,
                                  BigDecimal price, String currency, LocalDate openedAt, LocalDate expiresAt,
                                  String nutritionBasis, BigDecimal proteinPercent, BigDecimal fatPercent,
                                  BigDecimal fiberPercent, BigDecimal moisturePercent, BigDecimal ashPercent,
                                  BigDecimal calciumPercent, BigDecimal phosphorusPercent, BigDecimal energyKcal,
                                  String energyUnit, List<String> proteinSources, List<String> ingredients,
                                  List<String> additiveFlags, String lifeStageLabel, Long sourceMediaId,
                                  Long sourceRecognitionTaskId, String notes) {
        validateOwner(userId, petId);
        validateFields(name, brand, foodType, specification, packageWeight, packageWeightUnit, price,
                openedAt, expiresAt, nutritionBasis, proteinPercent, fatPercent, fiberPercent,
                moisturePercent, ashPercent, calciumPercent, phosphorusPercent, energyKcal, energyUnit,
                notes);
        return new FoodCard(null, userId, petId, name.trim(), trim(brand), foodType, trim(specification),
                packageWeight, normalizeUnit(packageWeightUnit), price, normalizeCurrency(currency),
                openedAt, expiresAt, normalizeBasis(nutritionBasis), proteinPercent, fatPercent,
                fiberPercent, moisturePercent, ashPercent, calciumPercent, phosphorusPercent, energyKcal,
                trim(energyUnit), proteinSources, ingredients, additiveFlags, trim(lifeStageLabel),
                sourceMediaId, sourceRecognitionTaskId, FoodStatus.ACTIVE, notes, false, null, null);
    }

    public static FoodCard restore(Long id, Long userId, Long petId, String name, String brand,
                                   FoodType foodType, String specification, BigDecimal packageWeight,
                                   String packageWeightUnit, BigDecimal price, String currency,
                                   LocalDate openedAt, LocalDate expiresAt, String nutritionBasis,
                                   BigDecimal proteinPercent, BigDecimal fatPercent, BigDecimal fiberPercent,
                                   BigDecimal moisturePercent, BigDecimal ashPercent,
                                   BigDecimal calciumPercent, BigDecimal phosphorusPercent,
                                   BigDecimal energyKcal, String energyUnit, List<String> proteinSources,
                                   List<String> ingredients, List<String> additiveFlags, String lifeStageLabel,
                                   Long sourceMediaId, Long sourceRecognitionTaskId, FoodStatus status,
                                   String notes, boolean deleted, LocalDateTime createdAt,
                                   LocalDateTime updatedAt) {
        return new FoodCard(id, userId, petId, name, brand, foodType, specification, packageWeight,
                packageWeightUnit, price, currency, openedAt, expiresAt, nutritionBasis, proteinPercent,
                fatPercent, fiberPercent, moisturePercent, ashPercent, calciumPercent, phosphorusPercent,
                energyKcal, energyUnit, proteinSources, ingredients, additiveFlags, lifeStageLabel,
                sourceMediaId, sourceRecognitionTaskId, status, notes, deleted, createdAt, updatedAt);
    }

    public void update(String name, String brand, FoodType foodType, String specification,
                       BigDecimal packageWeight, String packageWeightUnit, BigDecimal price, String currency,
                       LocalDate openedAt, LocalDate expiresAt, String nutritionBasis,
                       BigDecimal proteinPercent, BigDecimal fatPercent, BigDecimal fiberPercent,
                       BigDecimal moisturePercent, BigDecimal ashPercent, BigDecimal calciumPercent,
                       BigDecimal phosphorusPercent, BigDecimal energyKcal, String energyUnit,
                       List<String> proteinSources, List<String> ingredients, List<String> additiveFlags,
                       String lifeStageLabel, String notes) {
        validateFields(name, brand, foodType, specification, packageWeight, packageWeightUnit, price,
                openedAt, expiresAt, nutritionBasis, proteinPercent, fatPercent, fiberPercent,
                moisturePercent, ashPercent, calciumPercent, phosphorusPercent, energyKcal, energyUnit,
                notes);
        this.name = name.trim();
        this.brand = trim(brand);
        this.foodType = foodType;
        this.specification = trim(specification);
        this.packageWeight = packageWeight;
        this.packageWeightUnit = normalizeUnit(packageWeightUnit);
        this.price = price;
        this.currency = normalizeCurrency(currency);
        this.openedAt = openedAt;
        this.expiresAt = expiresAt;
        this.nutritionBasis = normalizeBasis(nutritionBasis);
        this.proteinPercent = proteinPercent;
        this.fatPercent = fatPercent;
        this.fiberPercent = fiberPercent;
        this.moisturePercent = moisturePercent;
        this.ashPercent = ashPercent;
        this.calciumPercent = calciumPercent;
        this.phosphorusPercent = phosphorusPercent;
        this.energyKcal = energyKcal;
        this.energyUnit = trim(energyUnit);
        this.proteinSources = immutableList(proteinSources);
        this.ingredients = immutableList(ingredients);
        this.additiveFlags = immutableList(additiveFlags);
        this.lifeStageLabel = trim(lifeStageLabel);
        this.notes = notes;
    }

    public void archive() {
        ensureNotDeleted();
        this.status = FoodStatus.ARCHIVED;
    }

    public void activate() {
        ensureNotDeleted();
        this.status = FoodStatus.ACTIVE;
    }

    public boolean canBeUsedForNewPlan() {
        return !deleted && status == FoodStatus.ACTIVE;
    }

    private void ensureNotDeleted() {
        if (deleted || status == FoodStatus.DELETED) {
            throw new IllegalStateException("已删除食品卡不可操作");
        }
    }

    private static void validateOwner(Long userId, Long petId) {
        if (userId == null || userId <= 0 || petId == null || petId <= 0) {
            throw new IllegalArgumentException("用户和宠物不能为空");
        }
    }

    private static void validateFields(String name, String brand, FoodType foodType, String specification,
                                       BigDecimal packageWeight, String packageWeightUnit, BigDecimal price,
                                       LocalDate openedAt, LocalDate expiresAt, String nutritionBasis,
                                       BigDecimal proteinPercent, BigDecimal fatPercent, BigDecimal fiberPercent,
                                       BigDecimal moisturePercent, BigDecimal ashPercent,
                                       BigDecimal calciumPercent, BigDecimal phosphorusPercent,
                                       BigDecimal energyKcal, String energyUnit, String notes) {
        if (name == null || name.isBlank() || name.length() > MAX_NAME_LENGTH || foodType == null) {
            throw new IllegalArgumentException("食品名称和类型不能为空且长度合法");
        }
        validateLength(brand, MAX_TEXT_LENGTH, "品牌");
        validateLength(specification, MAX_TEXT_LENGTH, "规格");
        validateLength(notes, MAX_NOTES_LENGTH, "备注");
        if (packageWeight != null && packageWeight.signum() <= 0) {
            throw new IllegalArgumentException("包装重量必须大于0");
        }
        if (packageWeight != null && packageWeightUnit == null) {
            throw new IllegalArgumentException("包装重量单位不能为空");
        }
        if (price != null && price.signum() < 0) {
            throw new IllegalArgumentException("价格不能为负");
        }
        if (openedAt != null && expiresAt != null && expiresAt.isBefore(openedAt)) {
            throw new IllegalArgumentException("保质期不能早于开封日期");
        }
        validatePercent(proteinPercent, "蛋白质");
        validatePercent(fatPercent, "脂肪");
        validatePercent(fiberPercent, "纤维");
        validatePercent(moisturePercent, "水分");
        validatePercent(ashPercent, "灰分");
        validatePercent(calciumPercent, "钙");
        validatePercent(phosphorusPercent, "磷");
        if (energyKcal != null && energyKcal.signum() <= 0) {
            throw new IllegalArgumentException("能量必须大于0");
        }
        normalizeUnit(packageWeightUnit);
        normalizeBasis(nutritionBasis);
        if (energyKcal != null && (energyUnit == null || energyUnit.isBlank())) {
            throw new IllegalArgumentException("能量单位不能为空");
        }
    }

    private static void validatePercent(BigDecimal value, String label) {
        if (value != null && (value.signum() < 0 || value.compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw new IllegalArgumentException(label + "百分比必须在0到100之间");
        }
    }

    private static void validateLength(String value, int max, String label) {
        if (value != null && value.length() > max) {
            throw new IllegalArgumentException(label + "长度不能超过" + max);
        }
    }

    private static String normalizeUnit(String unit) {
        if (unit == null || unit.isBlank()) {
            return null;
        }
        String value = unit.trim().toLowerCase();
        if (!value.equals("g") && !value.equals("ml")) {
            throw new IllegalArgumentException("单位仅支持 g 或 ml");
        }
        return value;
    }

    private static String normalizeBasis(String basis) {
        String value = basis == null || basis.isBlank() ? "unknown" : basis.trim().toLowerCase();
        if (!List.of("per_100g", "per_100ml", "serving", "unknown").contains(value)) {
            throw new IllegalArgumentException("营养基准不支持: " + basis);
        }
        return value;
    }

    private static String normalizeCurrency(String value) {
        return value == null || value.isBlank() ? "CNY" : value.trim().toUpperCase();
    }

    private static String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static List<String> immutableList(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
