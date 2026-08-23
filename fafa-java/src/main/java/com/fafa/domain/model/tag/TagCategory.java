package com.fafa.domain.model.tag;

import lombok.Getter;

/**
 * 标签分类
 *
 * 与 user_tag.category 列取值一致；宠物创建时自动生成的宠物名标签为 PET。
 *
 * @author FaFa Team
 * @since 4.0
 */
@Getter
public enum TagCategory {

    PET("pet", "宠物名"),
    LOCATION("location", "地点"),
    ACTION("action", "动作"),
    OBJECT("object", "物品"),
    MOOD("mood", "心情"),
    OTHER("other", "其他");

    private final String code;
    private final String label;

    TagCategory(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static TagCategory fromCode(String code) {
        for (TagCategory value : values()) {
            if (value.code.equalsIgnoreCase(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Invalid tag category: " + code);
    }
}
