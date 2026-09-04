package com.fafa.application.dto.media;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 语义检索结果DTO
 *
 * @param media  命中素材（已按相关度降序）
 * @param summary 确定性统计摘要（时间分布描述）
 * @author FaFa Team
 * @since 4.0
 */
@Schema(description = "素材语义检索结果")
public record SemanticSearchResultDTO(
    @Schema(description = "按相关度降序排列的命中素材") List<MediaDTO> media,
    @Schema(description = "检索结果摘要") String summary
) {
}
