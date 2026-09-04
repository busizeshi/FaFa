package com.fafa.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 通用分页结果DTO
 *
 * @author FaFa Team
 * @since 4.0
 */
@Schema(description = "通用分页结果")
public record PageDTO<T>(
    @Schema(description = "当前页数据列表")
    List<T> records,
    @Schema(description = "符合条件的总记录数", example = "42")
    long total,
    @Schema(description = "当前页码，从1开始", example = "1")
    int page,
    @Schema(description = "每页记录数", example = "20")
    int size
) {
    public static <T> PageDTO<T> of(List<T> records, long total, int page, int size) {
        return new PageDTO<>(records, total, page, size);
    }
}
