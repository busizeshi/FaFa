package com.fafa.application.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Agent 宠物资料工具响应。 */
@Schema(description = "AI 查询的宠物资料")
public record AiPetProfileDTO(
                              @Schema(description = "宠物ID", example = "1") Long petId,
                              @Schema(description = "宠物名称", example = "小白") String name,
                              @Schema(description = "宠物类型", example = "cat") String type,
                              @Schema(description = "宠物品种", example = "英短") String breed,
                              @Schema(description = "宠物性别", example = "male") String gender,
                              @Schema(description = "出生日期", example = "2023-01-15") LocalDate birthDate,
                              @Schema(description = "最近体重（kg）", example = "4.5") BigDecimal weight,
                              @Schema(description = "最近体重记录日期", example = "2026-08-20") LocalDate weightRecordDate) {
}
