package com.fafa.application.dto.expense;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** 开支统计响应，趋势点由后端补齐零值，便于前端直接绘图。 */
@Schema(description = "开支统计")
public record ExpenseStatisticsDTO(
                                   @Schema(description = "所属宠物ID", example = "1") Long petId,
                                   @Schema(description = "统计周期类型", example = "month") String period,
                                   @Schema(description = "统计开始日期", example = "2026-08-01") LocalDate startDate,
                                   @Schema(description = "统计结束日期", example = "2026-08-31") LocalDate endDate,
                                   @Schema(description = "统计货币代码", example = "CNY") String currency,
                                   @Schema(description = "统计周期内总金额", example = "520.00") BigDecimal totalAmount,
                                   @Schema(description = "统计周期内开支记录数", example = "8") int expenseCount,
                                   @Schema(description = "数据状态") String dataStatus,
                                   @Schema(description = "统计计算版本") String calculationVersion,
                                   @Schema(description = "分类汇总列表") List<CategoryBreakdown> categoryBreakdown,
                                   @Schema(description = "趋势数据点列表") List<TrendPoint> trend,
                                   @Schema(description = "统计限制或数据缺失说明") List<String> limitations) {

    @Schema(description = "开支分类汇总")
    public record CategoryBreakdown(
                                    @Schema(description = "分类编码", example = "food") String category,
                                    @Schema(description = "分类名称", example = "食品") String categoryLabel,
                                    @Schema(description = "分类金额", example = "300.00") BigDecimal amount,
                                    @Schema(description = "分类金额占比（百分比）", example = "57.69") BigDecimal percentage) {
    }

    @Schema(description = "开支趋势数据点")
    public record TrendPoint(
                             @Schema(description = "趋势周期标识", example = "2026-08") String period,
                             @Schema(description = "该周期金额", example = "300.00") BigDecimal amount,
                             @Schema(description = "该周期开支记录数", example = "4") int expenseCount) {
    }
}
