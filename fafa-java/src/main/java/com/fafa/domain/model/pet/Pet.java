package com.fafa.domain.model.pet;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

/**
 * 宠物聚合根
 * <p>
 * 三视图照片为宠物档案的关键组成：
 * - 创建宠物必须提供三张照片（位1=头像，位2=正面照，位3=侧面照）；
 * - 任何照片变更都会把向量化状态重置为 PENDING，等待 Python 重新向量化。
 *
 * @author FaFa Team
 * @since 1.0
 */
@Getter
public class Pet {

    /** 每只宠物必须持有的照片数量：位1头像 / 位2正面 / 位3侧面 */
    public static final int REQUIRED_PHOTO_COUNT = 3;

    private Long id;
    private Long userId;
    private String name;
    private PetType type;
    private String breed;
    private Gender gender;
    private LocalDate birthDate;
    private boolean neutered;
    private LocalDate adoptDate;
    private BigDecimal weight;
    private String avatarUrl;
    private String frontPhotoUrl;
    private String sidePhotoUrl;
    private PetVectorizationStatus vectorizationStatus;
    private Integer sortOrder;
    private boolean deleted;

    private Pet(Long id, Long userId, String name, PetType type, String breed,
                Gender gender, LocalDate birthDate, boolean neutered, LocalDate adoptDate,
                BigDecimal weight, String avatarUrl, String frontPhotoUrl,
                String sidePhotoUrl, PetVectorizationStatus vectorizationStatus,
                Integer sortOrder, boolean deleted) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.type = type;
        this.breed = breed;
        this.gender = gender;
        this.birthDate = birthDate;
        this.neutered = neutered;
        this.adoptDate = adoptDate;
        this.weight = weight;
        this.avatarUrl = avatarUrl;
        this.frontPhotoUrl = frontPhotoUrl;
        this.sidePhotoUrl = sidePhotoUrl;
        this.vectorizationStatus = vectorizationStatus;
        this.sortOrder = sortOrder;
        this.deleted = deleted;
    }

    /**
     * 注册新宠物（三张照片为必填：位1头像、位2正面照、位3侧面照）
     */
    public static Pet register(Long userId, String name, PetType type, String breed,
                               Gender gender, LocalDate birthDate, boolean neutered,
                               LocalDate adoptDate, BigDecimal weight, List<String> photos) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User ID is required");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Pet name is required");
        }
        if (type == null) {
            throw new IllegalArgumentException("Pet type is required");
        }
        if (gender == null) {
            throw new IllegalArgumentException("Gender is required");
        }
        requireThreePhotos(photos);

        return new Pet(null, userId, name, type, breed, gender, birthDate,
                neutered, adoptDate, weight,
                photos.get(0), photos.get(1), photos.get(2),
                PetVectorizationStatus.PENDING, 0, false);
    }

    /**
     * 重建已有宠物（仓储加载时使用）
     */
    public static Pet restore(Long id, Long userId, String name, PetType type,
                             String breed, Gender gender, LocalDate birthDate,
                             boolean neutered, LocalDate adoptDate, BigDecimal weight,
                             String avatarUrl, String frontPhotoUrl, String sidePhotoUrl,
                             PetVectorizationStatus vectorizationStatus,
                             Integer sortOrder, boolean deleted) {
        return new Pet(id, userId, name, type, breed, gender, birthDate,
                neutered, adoptDate, weight, avatarUrl, frontPhotoUrl,
                sidePhotoUrl, vectorizationStatus, sortOrder, deleted);
    }

    /**
     * 更新基本资料
     */
    public void updateProfile(String name, String breed, Gender gender,
                             LocalDate birthDate, boolean neutered, LocalDate adoptDate) {
        if (name != null && !name.trim().isEmpty()) {
            this.name = name;
        }
        this.breed = breed;
        if (gender != null) {
            this.gender = gender;
        }
        this.birthDate = birthDate;
        this.neutered = neutered;
        this.adoptDate = adoptDate;
    }

    /**
     * 更新体重
     */
    public void updateWeight(BigDecimal weight) {
        if (weight != null && weight.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Weight must be positive");
        }
        this.weight = weight;
    }

    /**
     * 更新三张照片（按位替换：位1头像、位2正面照、位3侧面照）
     * <p>
     * 三张缺一不可（更新时也必须校验三张照片齐全），
     * 照片变化后向量化状态重置为 PENDING，等待重新派发向量化。
     */
    public void updatePhotos(String avatarUrl, String frontPhotoUrl, String sidePhotoUrl) {
        requireThreePhotos(List.of(avatarUrl, frontPhotoUrl, sidePhotoUrl));
        this.avatarUrl = avatarUrl;
        this.frontPhotoUrl = frontPhotoUrl;
        this.sidePhotoUrl = sidePhotoUrl;
        this.vectorizationStatus = PetVectorizationStatus.PENDING;
    }

    /**
     * 调整排序
     */
    public void adjustSortOrder(Integer sortOrder) {
        if (sortOrder == null || sortOrder < 0) {
            throw new IllegalArgumentException("Sort order must be non-negative");
        }
        this.sortOrder = sortOrder;
    }

    /**
     * 标记向量化完成（Python 成功回调）。
     * 成功是最终事实，允许从任意状态覆盖（含失败重试后的翻转）。
     *
     * @return 状态是否发生变化
     */
    public boolean markVectorizationCompleted() {
        if (this.vectorizationStatus == PetVectorizationStatus.COMPLETED) {
            return false;
        }
        this.vectorizationStatus = PetVectorizationStatus.COMPLETED;
        return true;
    }

    /**
     * 标记向量化失败（Python 失败回调）。
     * 已完成的宠物不因迟到的失败回调降级（防止乱序回调回退状态）。
     *
     * @return 是否应用了失败标记
     */
    public boolean markVectorizationFailed() {
        if (this.vectorizationStatus == PetVectorizationStatus.COMPLETED) {
            return false;
        }
        this.vectorizationStatus = PetVectorizationStatus.FAILED;
        return true;
    }

    /**
     * 重置向量化状态为 PENDING（手动重试入口）。
     * 三张照片不齐全或已完成向量化时拒绝重置（完成即终态，无需重试）。
     *
     * @return 是否执行了重置
     */
    public boolean resetVectorizationToPending() {
        if (!hasThreePhotos()) {
            return false;
        }
        if (this.vectorizationStatus == PetVectorizationStatus.COMPLETED) {
            return false;
        }
        this.vectorizationStatus = PetVectorizationStatus.PENDING;
        return true;
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

    /**
     * 三张照片是否齐全（头像 + 正面 + 侧面）
     */
    public boolean hasThreePhotos() {
        return isNotBlank(avatarUrl) && isNotBlank(frontPhotoUrl) && isNotBlank(sidePhotoUrl);
    }

    /**
     * 按位序返回三张照片：[头像, 正面, 侧面]（位1即头像）
     * <p>
     * 存量宠物可能存在缺失照片（null），使用 Stream.toList 保留 null 元素。
     */
    public List<String> getPhotosInOrder() {
        return Stream.of(avatarUrl, frontPhotoUrl, sidePhotoUrl).toList();
    }

    private static void requireThreePhotos(List<String> photos) {
        if (photos == null || photos.size() != REQUIRED_PHOTO_COUNT) {
            throw new IllegalArgumentException("创建宠物必须提供三张照片（头像、正面照、侧面照）");
        }
        boolean anyBlank = photos.stream().anyMatch(url -> !isNotBlank(url));
        if (anyBlank) {
            throw new IllegalArgumentException("三张照片均不能为空");
        }
    }

    private static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}
