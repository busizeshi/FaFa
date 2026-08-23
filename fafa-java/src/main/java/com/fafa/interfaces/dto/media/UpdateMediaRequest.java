package com.fafa.interfaces.dto.media;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 编辑素材请求（tags / takenAt）
 *
 * @author FaFa Team
 * @since 4.0
 */
@Data
@Schema(description = "编辑素材请求")
public class UpdateMediaRequest {

    @Schema(description = "标签列表（全量替换，≤10个、单个≤32字符）", example = "[\"晒太阳\", \"阳台\"]")
    private List<String> tags;

    @Schema(description = "拍摄时间（传 null 表示不修改）", example = "2026-08-20T15:30:00")
    private LocalDateTime takenAt;
}
