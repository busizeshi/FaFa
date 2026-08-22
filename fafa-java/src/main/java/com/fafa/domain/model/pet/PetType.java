package com.fafa.domain.model.pet;

import lombok.Getter;

@Getter
public enum PetType {
    CAT("cat", "猫"),
    DOG("dog", "狗"),
    OTHER("other", "其他");

    private final String code;
    private final String description;

    PetType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static PetType fromCode(String code) {
        for (PetType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid pet type: " + code);
    }
}
