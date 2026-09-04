package com.fafa.application.dto.feeding;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** 动态喂养分析返回对象。 */
@Schema(description = "动态喂养分析")
public record FeedingAnalysisDTO(
        @Schema(description = "所属宠物ID", example = "1") Long petId,
        @Schema(description = "分析日期范围") RangeDTO range,
        @Schema(description = "分析事实统计") FactsDTO facts,
        @Schema(description = "分析观察结论列表") List<ObservationDTO> observations,
        @Schema(description = "复查建议列表") List<String> reviewSuggestions,
        @Schema(description = "数据限制或缺失说明") List<String> limitations,
        @Schema(description = "分析数据来源", example = "rule") String analysisSource,
        @Schema(description = "分析策略版本") String policyVersion
) {
    @Schema(description = "分析日期范围")
    public record RangeDTO(
            @Schema(description = "开始日期", example = "2026-08-01") LocalDate startDate,
            @Schema(description = "结束日期", example = "2026-08-31") LocalDate endDate) {
    }

    @Schema(description = "喂养事实统计")
    public record FactsDTO(
                           @Schema(description = "实际喂食天数", example = "28") int feedingDays,
                           @Schema(description = "平均每日喂食量", example = "180") BigDecimal averageDailyAmount,
                           @Schema(description = "喂食量单位", example = "g") String unit,
                           @Schema(description = "计划覆盖天数", example = "30") int plannedDays,
                           @Schema(description = "计划完成率，范围 0-1", example = "0.93") BigDecimal planCompletionRate,
                           @Schema(description = "体重统计") WeightDTO weight,
                           @Schema(description = "每日状态统计") DailyStatusFactsDTO dailyStatus) {
    }

    @Schema(description = "体重统计")
    public record WeightDTO(
            @Schema(description = "区间首条体重（kg）", example = "4.2") BigDecimal firstKg,
            @Schema(description = "区间末条体重（kg）", example = "4.5") BigDecimal lastKg,
            @Schema(description = "体重变化（kg）", example = "0.3") BigDecimal changeKg,
            @Schema(description = "体重样本数量", example = "4") int sampleCount) {
    }

    @Schema(description = "每日状态统计")
    public record DailyStatusFactsDTO(
            @Schema(description = "食欲正常天数", example = "25") int appetiteNormalDays,
            @Schema(description = "便便偏软天数", example = "1") int softStoolDays,
            @Schema(description = "拒食天数", example = "0") int refusalDays) {
    }

    @Schema(description = "喂养分析观察结论")
    public record ObservationDTO(
            @Schema(description = "观察结论编码", example = "weight_stable") String key,
            @Schema(description = "观察结论文本") String text,
            @Schema(description = "支持该结论的证据") List<String> evidence) {
    }
}
