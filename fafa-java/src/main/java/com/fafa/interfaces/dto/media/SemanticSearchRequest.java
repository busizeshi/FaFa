package com.fafa.interfaces.dto.media;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * AI 语义检索请求
 *
 * @author FaFa Team
 * @since 4.0
 */
@Data
@Schema(description = "AI 语义检索请求")
public class SemanticSearchRequest {

    @NotBlank(message = "检索语句不能为空")
    @Size(max = 100, message = "检索语句不能超过100字符")
    @Schema(description = "自然语言检索语句", example = "豆包在阳台晒太阳的照片")
    private String query;

    @Schema(description = "宠物过滤（可选）", example = "1")
    private Long petId;

    @Schema(description = "开始日期（可选）", example = "2026-08-01")
    private LocalDate startDate;

    @Schema(description = "结束日期（可选）", example = "2026-08-23")
    private LocalDate endDate;

    @Min(value = 1, message = "返回条数不能小于1")
    @Max(value = 100, message = "返回条数不能超过100")
    @Schema(description = "最大返回条数（默认20，上限100）", example = "20")
    private Integer limit;
}
