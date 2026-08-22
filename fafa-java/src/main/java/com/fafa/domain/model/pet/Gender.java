package com.fafa.domain.model.pet;

import lombok.Getter;

@Getter
public enum Gender {
    MALE("male", "公"),
    FEMALE("female", "母"),
    UNKNOWN("unknown", "未知");

    private final String code;
    private final String description;

    Gender(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static Gender fromCode(String code) {
        for (Gender gender : values()) {
            if (gender.code.equalsIgnoreCase(code)) {
                return gender;
            }
        }
        throw new IllegalArgumentException("Invalid gender: " + code);
    }
}
