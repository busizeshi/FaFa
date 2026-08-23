package com.fafa.domain.model.record;

import lombok.Getter;

/**
 * 排便维度
 *
 * not_seen 为中性逃生口：既非正常也非异常（漏记不惩罚）。
 *
 * @author FaFa Team
 * @since 2.0
 */
@Getter
public enum Defecation {

    NORMAL("normal", "正常"),
    SOFT("soft", "偏软"),
    DIARRHEA("diarrhea", "腹泻"),
    CONSTIPATION("constipation", "便秘"),
    NOT_SEEN("not_seen", "没看到");

    private final String code;
    private final String label;

    Defecation(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static Defecation fromCode(String code) {
        for (Defecation value : values()) {
            if (value.code.equalsIgnoreCase(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Invalid defecation: " + code);
    }

    public boolean isNormal() {
        return this == NORMAL;
    }

    /** 是否为明确异常档（偏软/腹泻/便秘） */
    public boolean isAbnormal() {
        return this == SOFT || this == DIARRHEA || this == CONSTIPATION;
    }
}
