package com.fafa.application.dto;

import java.util.List;

/**
 * 通用分页结果DTO
 *
 * @author FaFa Team
 * @since 4.0
 */
public record PageDTO<T>(
    List<T> records,
    long total,
    int page,
    int size
) {
    public static <T> PageDTO<T> of(List<T> records, long total, int page, int size) {
        return new PageDTO<>(records, total, page, size);
    }
}
