package com.fafa.application.dto.feeding;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 指定日期的喂食清单。 */
@Schema(description = "指定日期喂食清单")
public record FeedingChecklistDTO(
                                  @Schema(description = "喂食计划ID", example = "1") Long planId,
                                  @Schema(description = "清单日期", example = "2026-08-20") LocalDate date,
                                  @Schema(description = "计划状态", example = "active") String status,
                                  @Schema(description = "当日喂食项列表") List<ItemDTO> items,
                                  @Schema(description = "已完成项数量", example = "2") int completedCount,
                                  @Schema(description = "总项数量", example = "3") int totalCount) {
    @Schema(description = "喂食清单项")
    public record ItemDTO(
                          @Schema(description = "喂食计划项ID", example = "101") Long planItemId,
                          @Schema(description = "计划项稳定标识", example = "breakfast") String itemKey,
                          @Schema(description = "食品卡ID", example = "201") Long foodId,
                          @Schema(description = "食品名称", example = "成猫鸡肉配方粮") String foodName,
                          @Schema(description = "计划喂食时间", example = "2026-08-20T08:00:00") LocalDateTime plannedAt,
                          @Schema(description = "目标喂食量", example = "60") BigDecimal amount,
                          @Schema(description = "喂食量单位", example = "g") String unit,
                          @Schema(description = "完成状态", example = "completed") String completionStatus,
                          @Schema(description = "关联实际喂食记录ID", example = "301") Long feedingRecordId,
                          @Schema(description = "实际喂食时间", example = "2026-08-20T08:10:00") LocalDateTime actualFeedingTime) {
    }
}
