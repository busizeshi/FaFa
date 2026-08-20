package com.fafa.domain.model.pet;

import com.fafa.domain.event.PetCreatedEvent;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 宠物聚合根
 * 
 * 职责：
 * 1. 封装宠物相关的业务逻辑
 * 2. 维护宠物数据的一致性
 * 3. 发布领域事件
 * 
 * @author FaFa Team
 * @since 1.0
 */
@Getter
public class Pet {

    /**
     * 宠物 ID
     */
    private PetId id;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 宠物昵称
     */
    private String name;

    /**
     * 宠物头像
     */
    private String avatar;

    /**
     * 正面照URL
     */
    private String frontViewUrl;

    /**
     * 侧面照URL
     */
    private String sideViewUrl;

    /**
     * 俯视照URL
     */
    private String topViewUrl;

    /**
     * 三视图向量ID数组
     */
    private String profileEmbeddingIds;

    /**
     * 种类
     */
    private PetSpecies species;

    /**
     * 品种
     */
    private String breed;

    /**
     * 性别
     */
    private PetGender gender;

    /**
     * 出生日期
     */
    private LocalDate birthDate;

    /**
     * 到家日期
     */
    private LocalDate adoptDate;

    /**
     * 当前体重（kg）
     */
    private Double weight;

    /**
     * 是否绝育
     */
    private Boolean isNeutered;

    /**
     * 毛色
     */
    private String coatColor;

    /**
     * 备注
     */
    private String remarks;

    /**
     * 状态
     */
    private PetStatus status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 私有构造函数，防止外部直接 new
     */
    private Pet() {
    }

    /**
     * 创建新宠物（工厂方法）
     * 
     * @param userId 用户 ID
     * @param name 宠物名称
     * @param species 种类
     * @return 新创建的宠物实体
     */
    public static Pet create(Long userId, String name, PetSpecies species) {
        // 业务规则校验
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("用户 ID 不能为空");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("宠物名称不能为空");
        }
        if (species == null) {
            throw new IllegalArgumentException("宠物种类不能为空");
        }

        Pet pet = new Pet();
        pet.userId = userId;
        pet.name = name.trim();
        pet.species = species;
        pet.status = PetStatus.NORMAL;
        pet.createdAt = LocalDateTime.now();
        pet.updatedAt = LocalDateTime.now();

        // 发布领域事件（创建宠物）
        // DomainEventPublisher.publish(new PetCreatedEvent(pet.id, pet.userId));

        return pet;
    }

    /**
     * 更新宠物基础信息
     * 
     * @param name 宠物名称
     * @param breed 品种
     * @param gender 性别
     */
    public void updateBasicInfo(String name, String breed, PetGender gender) {
        if (name != null && !name.trim().isEmpty()) {
            this.name = name.trim();
        }
        this.breed = breed;
        this.gender = gender;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 更新体重
     * 
     * @param weight 新体重
     */
    public void updateWeight(Double weight) {
        if (weight != null && weight <= 0) {
            throw new IllegalArgumentException("体重必须大于 0");
        }
        this.weight = weight;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 设置绝育状态
     */
    public void markAsNeutered() {
        this.isNeutered = true;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 标记为已离世
     */
    public void markAsPassed() {
        this.status = PetStatus.PASSED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 更新宠物三视图
     */
    public void updateProfilePhotos(String frontViewUrl, String sideViewUrl, String topViewUrl) {
        this.frontViewUrl = frontViewUrl;
        this.sideViewUrl = sideViewUrl;
        this.topViewUrl = topViewUrl;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 更新三视图向量ID
     */
    public void updateProfileEmbeddingIds(String embeddingIds) {
        this.profileEmbeddingIds = embeddingIds;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 计算年龄（月）
     */
    public Integer getAgeInMonths() {
        if (birthDate == null) {
            return null;
        }
        LocalDate now = LocalDate.now();
        return (now.getYear() - birthDate.getYear()) * 12 + (now.getMonthValue() - birthDate.getMonthValue());
    }

    /**
     * 判断是否为幼年期（< 12 个月）
     */
    public boolean isPuppy() {
        Integer age = getAgeInMonths();
        return age != null && age < 12;
    }

    // Setter 方法用于持久化层重建对象
    public void setId(PetId id) {
        this.id = id;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public void setFrontViewUrl(String frontViewUrl) {
        this.frontViewUrl = frontViewUrl;
    }

    public void setSideViewUrl(String sideViewUrl) {
        this.sideViewUrl = sideViewUrl;
    }

    public void setTopViewUrl(String topViewUrl) {
        this.topViewUrl = topViewUrl;
    }

    public void setProfileEmbeddingIds(String profileEmbeddingIds) {
        this.profileEmbeddingIds = profileEmbeddingIds;
    }

    public void setSpecies(PetSpecies species) {
        this.species = species;
    }

    public void setBreed(String breed) {
        this.breed = breed;
    }

    public void setGender(PetGender gender) {
        this.gender = gender;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public void setAdoptDate(LocalDate adoptDate) {
        this.adoptDate = adoptDate;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public void setIsNeutered(Boolean isNeutered) {
        this.isNeutered = isNeutered;
    }

    public void setCoatColor(String coatColor) {
        this.coatColor = coatColor;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public void setStatus(PetStatus status) {
        this.status = status;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setSortOrder(Integer sortOrder) {
        // 用于排序功能
    }
}
