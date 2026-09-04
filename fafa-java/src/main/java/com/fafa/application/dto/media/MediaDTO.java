package com.fafa.application.dto.media;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 素材DTO
 *
 * @author FaFa Team
 * @since 4.0
 */
@Schema(description = "素材信息")
public record MediaDTO(
    @Schema(description = "素材ID", example = "1") Long id,
    @Schema(description = "归属宠物ID；未确认归属时为空", example = "1") Long petId,
    @Schema(description = "宠物名称", example = "小白") String petName,
    @Schema(description = "素材类型：photo=照片，video=视频", example = "photo") String mediaType,
    @Schema(description = "素材访问URL") String fileUrl,
    @Schema(description = "原始文件名", example = "IMG_20260820.jpg") String fileName,
    @Schema(description = "文件大小（字节）", example = "1048576") Long fileSize,
    @Schema(description = "文件 MIME 类型", example = "image/jpeg") String mimeType,
    @Schema(description = "用户标签和系统标签列表", example = "[\"晒太阳\", \"阳台\"]") List<String> tags,
    @Schema(description = "素材处理状态", example = "completed") String status,
    @Schema(description = "AI 生成的素材描述") String aiDescription,
    @Schema(description = "AI 识别标签列表") List<String> aiTags,
    @Schema(description = "宠物归属识别相似度，范围 0-1", example = "0.95") BigDecimal recognitionScore,
    @Schema(description = "是否已确认宠物归属", example = "true") boolean recognitionConfirmed,
    @Schema(description = "素材拍摄时间", example = "2026-08-20T15:30:00") LocalDateTime takenAt,
    @Schema(description = "素材处理失败原因，成功时为空") String errorMessage,
    @Schema(description = "素材创建时间", example = "2026-08-20T15:30:00") LocalDateTime createdAt
) {
}
