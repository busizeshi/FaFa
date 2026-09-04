package com.fafa.application.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Agent 每日状态工具响应。 */
@Schema(description = "AI 查询的每日状态历史")
public record AiDailyStatusDTO(
                               @Schema(description = "宠物ID", example = "1") Long petId,
                               @Schema(description = "查询开始日期", example = "2026-08-01") LocalDate startDate,
                               @Schema(description = "查询结束日期", example = "2026-08-31") LocalDate endDate,
                               @Schema(description = "每日状态记录列表") List<AiDailyStatusRecordDTO> records,
                               @Schema(description = "已记录天数", example = "25") int recordedDays,
                               @Schema(description = "缺失记录天数", example = "6") int missingDays,
                               @Schema(description = "拒食天数", example = "1") int refusedDays) {

    @Schema(description = "AI 每日状态记录")
    public record AiDailyStatusRecordDTO(
                                         @Schema(description = "记录日期", example = "2026-08-20") LocalDate recordDate,
                                         @Schema(description = "食欲状态", example = "normal") String appetite,
                                         @Schema(description = "精神活动状态", example = "active") String energy,
                                         @Schema(description = "饮水状态", example = "normal") String waterIntake,
                                         @Schema(description = "排便状态", example = "normal") String defecation,
                                         @Schema(description = "呕吐状态", example = "none") String vomiting,
                                         @Schema(description = "排尿状态", example = "normal") String urination,
                                         @Schema(description = "进食量（克）", example = "80") BigDecimal appetiteAmount,
                                         @Schema(description = "饮水量（毫升）", example = "200") BigDecimal waterAmount,
                                         @Schema(description = "备注") String notes) {
    }
}
