package com.fafa.application.dto.expense;

import io.swagger.v3.oas.annotations.media.Schema;

/** 可用开支分类响应。 */
@Schema(description = "开支分类")
public record ExpenseCategoryDTO(
        @Schema(description = "分类编码", example = "food") String code,
        @Schema(description = "分类名称", example = "食品") String label,
        @Schema(description = "展示排序序号", example = "1") int sortOrder) {
}
