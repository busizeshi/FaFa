package com.fafa.interfaces.dto.feeding;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** 生成喂食建议预览请求。 */
@Data
@Schema(description = "生成喂食建议预览请求")
public class FeedingRecommendationRequest {
    @NotNull @Schema(description = "所属宠物ID", example = "1") private Long petId;
    @NotEmpty @Schema(description = "参与计算的食品卡ID列表", example = "[101, 102]") private List<Long> foodIds;
    @NotNull @Schema(description = "喂食建议开始日期", example = "2026-09-01") private LocalDate startDate;
    @Schema(description = "每日目标喂食总量；与 targetCalories 至少提供一个", example = "180")
    private BigDecimal dailyTargetAmount;
    @Schema(description = "每日目标能量（kcal）；与 dailyTargetAmount 至少提供一个", example = "720")
    private BigDecimal targetCalories;
    @Schema(description = "喂食量单位：g=克，ml=毫升", example = "g") private String unit;
    @Schema(description = "每日餐次数", example = "3") private Integer mealCount;
    @Schema(description = "每日进餐时间列表", example = "[\"08:00\", \"13:00\", \"19:00\"]")
    private List<LocalTime> mealTimes;
    @Schema(description = "活动水平：low=低，normal=正常，high=高", example = "normal")
    private String activityLevel;
    @Schema(description = "用户补充说明") private String userNote;
}
