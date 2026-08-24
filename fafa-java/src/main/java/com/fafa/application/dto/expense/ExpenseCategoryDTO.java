package com.fafa.application.dto.expense;

/** 可用开支分类响应。 */
public record ExpenseCategoryDTO(String code, String label, int sortOrder) {
}
