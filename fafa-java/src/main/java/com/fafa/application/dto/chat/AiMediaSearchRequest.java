package com.fafa.application.dto.chat;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Agent 素材语义检索请求。 */
@Schema(description = "AI 素材语义检索请求")
public record AiMediaSearchRequest(
                                   @Schema(description = "调用方用户ID", example = "1") @NotNull @JsonAlias("user_id") Long userId,
                                   @Schema(description = "自然语言检索语句", example = "豆包在阳台晒太阳的照片") @NotBlank String query,
                                   @Schema(description = "宠物过滤条件，可选", example = "1") @JsonAlias("pet_id") Long petId,
                                   @Schema(description = "开始日期，可选", example = "2026-08-01") @JsonAlias("start_date") LocalDate startDate,
                                   @Schema(description = "结束日期，可选", example = "2026-08-31") @JsonAlias("end_date") LocalDate endDate,
                                   @Schema(description = "最大返回条数", example = "20") Integer limit) {
}
