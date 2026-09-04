package com.fafa.application.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Agent 体重工具响应。 */
@Schema(description = "AI 查询的体重历史")
public record AiWeightHistoryDTO(
                                 @Schema(description = "宠物ID", example = "1") Long petId,
                                 @Schema(description = "查询开始日期", example = "2026-08-01") LocalDate startDate,
                                 @Schema(description = "查询结束日期", example = "2026-08-31") LocalDate endDate,
                                 @Schema(description = "体重记录列表") List<AiWeightRecordDTO> records) {
    @Schema(description = "AI 体重记录")
    public record AiWeightRecordDTO(
            @Schema(description = "体重记录ID", example = "1") Long id,
            @Schema(description = "记录日期", example = "2026-08-20") LocalDate recordDate,
            @Schema(description = "体重（kg）", example = "4.5") BigDecimal weight,
            @Schema(description = "备注") String notes) {
    }
}
