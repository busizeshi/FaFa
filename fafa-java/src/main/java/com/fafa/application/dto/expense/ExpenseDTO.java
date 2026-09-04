package com.fafa.application.dto.expense;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 开支记录响应。 */
@Schema(description = "开支记录")
public record ExpenseDTO(
                         @Schema(description = "开支记录ID", example = "1") Long id,
                         @Schema(description = "所属宠物ID", example = "1") Long petId,
                         @Schema(description = "开支分类编码", example = "food") String category,
                         @Schema(description = "开支分类名称", example = "食品") String categoryLabel,
                         @Schema(description = "开支金额", example = "128.00") BigDecimal amount,
                         @Schema(description = "货币代码", example = "CNY") String currency,
                         @Schema(description = "开支发生日期", example = "2026-08-20") LocalDate expenseDate,
                         @Schema(description = "开支标题", example = "购买猫粮") String title,
                         @Schema(description = "开支备注") String notes,
                         @Schema(description = "关联收据素材ID", example = "1001") Long receiptMediaId,
                         @Schema(description = "关联食品卡ID", example = "2001") Long foodCardId,
                         @Schema(description = "食品名称快照", example = "成猫鸡肉配方粮") String foodNameSnapshot,
                         @Schema(description = "关联就医档案ID", example = "3001") Long medicalVisitId,
                         @Schema(description = "开支来源类型", example = "manual") String sourceType,
                         @Schema(description = "是否由系统自动生成", example = "false") boolean systemGenerated,
                         @Schema(description = "当前记录是否允许编辑", example = "true") boolean editable,
                         @Schema(description = "创建时间", example = "2026-08-20T10:30:00") LocalDateTime createdAt,
                         @Schema(description = "最后更新时间", example = "2026-08-20T10:30:00") LocalDateTime updatedAt) {
}
