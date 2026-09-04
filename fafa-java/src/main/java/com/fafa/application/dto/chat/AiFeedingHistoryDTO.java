package com.fafa.application.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Agent 喂食工具响应。 */
@Schema(description = "AI 查询的喂食历史")
public record AiFeedingHistoryDTO(
                                  @Schema(description = "宠物ID", example = "1") Long petId,
                                  @Schema(description = "查询开始日期", example = "2026-08-01") LocalDate startDate,
                                  @Schema(description = "查询结束日期", example = "2026-08-31") LocalDate endDate,
                                  @Schema(description = "喂食记录列表") List<AiFeedingRecordDTO> records,
                                  @Schema(description = "记录总数", example = "24") int count) {
    @Schema(description = "AI 喂食记录")
    public record AiFeedingRecordDTO(
                                     @Schema(description = "喂食记录ID", example = "1") Long id,
                                     @Schema(description = "实际喂食时间", example = "2026-08-20T08:30:00") LocalDateTime feedingTime,
                                     @Schema(description = "食物类型", example = "main_food") String foodType,
                                     @Schema(description = "喂食量", example = "60") BigDecimal amount,
                                     @Schema(description = "喂食量单位", example = "g") String unit,
                                     @Schema(description = "备注") String notes) {
    }
}
