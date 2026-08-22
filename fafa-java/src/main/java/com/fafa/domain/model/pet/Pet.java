package com.fafa.domain.model.pet;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 宠物聚合根
 *
 * @author FaFa Team
 * @since 1.0
 */
@Getter
public class Pet {

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
    private Integer sortOrder;
    private boolean deleted;

    private Pet(Long id, Long userId, String name, PetType type, String breed, 
                Gender gender, LocalDate birthDate, boolean neutered, LocalDate adoptDate,
                BigDecimal weight, String avatarUrl, String frontPhotoUrl, 
                String sidePhotoUrl, Integer sortOrder, boolean deleted) {
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
        this.sortOrder = sortOrder;
        this.deleted = deleted;
    }

    /**
     * 注册新宠物
     */
    public static Pet register(Long userId, String name, PetType type, String breed,
                               Gender gender, LocalDate birthDate, boolean neutered,
                               LocalDate adoptDate) {
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

        return new Pet(null, userId, name, type, breed, gender, birthDate, 
                      neutered, adoptDate, null, null, null, null, 0, false);
    }

    /**
     * 重建已有宠物（仓储加载时使用）
     */
    public static Pet restore(Long id, Long userId, String name, PetType type, 
                             String breed, Gender gender, LocalDate birthDate, 
                             boolean neutered, LocalDate adoptDate, BigDecimal weight,
                             String avatarUrl, String frontPhotoUrl, String sidePhotoUrl,
                             Integer sortOrder, boolean deleted) {
        return new Pet(id, userId, name, type, breed, gender, birthDate, 
                      neutered, adoptDate, weight, avatarUrl, frontPhotoUrl, 
                      sidePhotoUrl, sortOrder, deleted);
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
     * 更新头像
     */
    public void updateAvatar(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    /**
     * 更新三视图照片（正面照+侧面照必须同时提供或同时为空）
     */
    public void updateProfilePhotos(String frontPhotoUrl, String sidePhotoUrl) {
        if ((frontPhotoUrl == null && sidePhotoUrl != null) 
            || (frontPhotoUrl != null && sidePhotoUrl == null)) {
            throw new IllegalArgumentException(
                "Front photo and side photo must be provided together or both null");
        }
        this.frontPhotoUrl = frontPhotoUrl;
        this.sidePhotoUrl = sidePhotoUrl;
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
     * 是否有三视图
     */
    public boolean hasProfilePhotos() {
        return frontPhotoUrl != null && sidePhotoUrl != null;
    }
}
