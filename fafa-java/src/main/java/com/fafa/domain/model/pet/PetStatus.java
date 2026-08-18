package com.fafa.domain.model.pet;

/**
 * 宠物状态枚举
 * 
 * @author FaFa Team
 * @since 1.0
 */
public enum PetStatus {

    /**
     * 正常
     */
    NORMAL(1, "正常"),

    /**
     * 已离世
     */
    PASSED(0, "已离世");

    private final Integer code;
    private final String description;

    PetStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public Integer getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static PetStatus fromCode(Integer code) {
        for (PetStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return NORMAL;
    }
}
