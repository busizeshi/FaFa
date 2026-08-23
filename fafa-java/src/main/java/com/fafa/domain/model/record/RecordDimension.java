package com.fafa.domain.model.record;

import lombok.Getter;

/**
 * 记录维度元信息
 *
 * 描述每日打卡的六个观察维度，供物种配置（SpeciesRecordPolicy）
 * 与前端渲染（dimensions 接口）使用。FoodType 不属于观察维度，不在此列。
 *
 * @author FaFa Team
 * @since 2.0
 */
@Getter
public enum RecordDimension {

    APPETITE("appetite", "食欲"),
    ENERGY("energy", "精神活动"),
    WATER_INTAKE("water_intake", "饮水"),
    DEFECATION("defecation", "排便"),
    VOMITING("vomiting", "呕吐"),
    URINATION("urination", "排尿");

    private final String code;
    private final String label;

    RecordDimension(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static RecordDimension fromCode(String code) {
        for (RecordDimension dimension : values()) {
            if (dimension.code.equalsIgnoreCase(code)) {
                return dimension;
            }
        }
        throw new IllegalArgumentException("Invalid record dimension: " + code);
    }
}
