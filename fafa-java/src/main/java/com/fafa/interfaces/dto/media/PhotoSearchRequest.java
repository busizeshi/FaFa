package com.fafa.interfaces.dto.media;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 素材融合检索请求（确定性 + 语义）
 *
 * query 为空走确定性分页检索（不限次）；query 有值走语义检索（占 AI 月配额），
 * petId/tags/日期作为确定性硬过滤叠加。
 *
 * @author FaFa Team
 * @since 4.0
 */
@Data
@Schema(description = "素材融合检索请求")
public class PhotoSearchRequest {

    @Size(max = 100, message = "检索语句不能超过100字符")
    @Schema(description = "自然语言检索语句（为空则走确定性检索，不占 AI 配额）", example = "豆包在阳台晒太阳的照片")
    private String query;

    @Schema(description = "宠物过滤（可选）", example = "1")
    private Long petId;

    @Schema(description = "标签过滤（可选，任一命中即算）")
    private List<String> tags;

    @Schema(description = "开始日期（可选）", example = "2026-08-01")
    private LocalDate startDate;

    @Schema(description = "结束日期（可选）", example = "2026-08-23")
    private LocalDate endDate;

    @Min(value = 1, message = "页码不能小于1")
    @Schema(description = "页码，从1开始（仅确定性检索生效，语义检索固定第1页）", example = "1")
    private Integer page;

    @Min(value = 1, message = "每页条数不能小于1")
    @Max(value = 100, message = "每页条数不能超过100")
    @Schema(description = "每页条数（默认20，上限100；语义检索时作为最大返回条数）", example = "20")
    private Integer size;
}
