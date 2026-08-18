package com.fafa.domain.model.pet;

/**
 * 宠物性别枚举
 * 
 * @author FaFa Team
 * @since 1.0
 */
public enum PetGender {

    /**
     * 公
     */
    MALE("male", "公"),

    /**
     * 母
     */
    FEMALE("female", "母"),

    /**
     * 未知
     */
    UNKNOWN("unknown", "未知");

    private final String code;
    private final String description;

    PetGender(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static PetGender fromCode(String code) {
        for (PetGender gender : values()) {
            if (gender.code.equals(code)) {
                return gender;
            }
        }
        return UNKNOWN;
    }
}
