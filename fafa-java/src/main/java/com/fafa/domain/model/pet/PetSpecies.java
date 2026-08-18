package com.fafa.domain.model.pet;

/**
 * 宠物种类枚举
 * 
 * @author FaFa Team
 * @since 1.0
 */
public enum PetSpecies {

    /**
     * 猫
     */
    CAT("cat", "猫"),

    /**
     * 狗
     */
    DOG("dog", "狗"),

    /**
     * 其他
     */
    OTHER("other", "其他");

    private final String code;
    private final String description;

    PetSpecies(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static PetSpecies fromCode(String code) {
        for (PetSpecies species : values()) {
            if (species.code.equals(code)) {
                return species;
            }
        }
        throw new IllegalArgumentException("未知的宠物种类: " + code);
    }
}
