package com.fafa.application.dto.content;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "内容草稿")
public record ContentDraftDTO(
                              @Schema(description = "内容草稿ID", example = "1") Long id,
                              @Schema(description = "所属宠物ID", example = "1") Long petId,
                              @Schema(description = "内容类型", example = "share_note") String contentType,
                              @Schema(description = "主题类型", example = "monthly_summary") String topicType,
                              @Schema(description = "自定义主题") String customTopic,
                              @Schema(description = "统计周期类型", example = "month") String periodType,
                              @Schema(description = "统计开始日期", example = "2026-08-01") LocalDate periodStart,
                              @Schema(description = "统计结束日期", example = "2026-08-31") LocalDate periodEnd,
                              @Schema(description = "选中的素材数据") JsonNode selectedMedia,
                              @Schema(description = "生成草稿时使用的事实快照") JsonNode factSnapshot,
                              @Schema(description = "内容草稿正文") JsonNode draft,
                              @Schema(description = "草稿状态") String status,
                              @Schema(description = "内容模式") String contentMode,
                              @Schema(description = "生成触发来源") String generationTrigger,
                              @Schema(description = "幂等键") String idempotencyKey,
                              @Schema(description = "生成版本号", example = "1") int generationVersion,
                              @Schema(description = "使用的模型名称") String modelName,
                              @Schema(description = "提示词版本") String promptVersion,
                              @Schema(description = "最近一次失败代码") String lastErrorCode,
                              @Schema(description = "最近一次失败信息") String lastErrorMessage,
                              @Schema(description = "创建时间") LocalDateTime createdAt,
                              @Schema(description = "最后更新时间") LocalDateTime updatedAt) {
}
