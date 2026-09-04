package com.fafa.application.dto.chat;

import com.fafa.application.dto.expense.ExpenseDTO;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** AI 开支查询结果。 */
@Schema(description = "AI 开支查询结果")
public record AiExpenseQueryDTO(
        @Schema(description = "开支记录列表") List<ExpenseDTO> records,
        @Schema(description = "开支记录总数", example = "12") int count) {
}
