package com.fafa.domain.model.expense;

import lombok.Getter;

/** 开支来源。自动来源必须保持只读，避免用户修改医疗事实时发生数据分叉。 */
@Getter
public enum ExpenseSourceType {
    MANUAL("manual"),
    MEDICAL_VISIT("medical_visit");

    private final String code;

    ExpenseSourceType(String code) {
        this.code = code;
    }
}
