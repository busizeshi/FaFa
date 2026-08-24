package com.fafa.domain.model.expense;

import lombok.Getter;

import java.util.Arrays;

/** 开支分类，分类编码直接作为持久化和接口契约的一部分。 */
@Getter
public enum ExpenseCategory {
    FOOD("food", "食品", 1),
    MEDICAL("medical", "医疗", 2),
    SUPPLIES("supplies", "用品", 3),
    GROOMING("grooming", "美容护理", 4),
    INSURANCE("insurance", "保险", 5),
    OTHER("other", "其他", 6);

    private final String code;
    private final String label;
    private final int sortOrder;

    ExpenseCategory(String code, String label, int sortOrder) {
        this.code = code;
        this.label = label;
        this.sortOrder = sortOrder;
    }

    public static ExpenseCategory fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("开支分类不能为空");
        }
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(code.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("不支持的开支分类: " + code));
    }
}
