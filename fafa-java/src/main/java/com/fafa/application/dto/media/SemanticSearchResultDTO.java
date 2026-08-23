package com.fafa.application.dto.media;

import java.util.List;

/**
 * 语义检索结果DTO
 *
 * @param media  命中素材（已按相关度降序）
 * @param summary 确定性统计摘要（时间分布描述）
 * @author FaFa Team
 * @since 4.0
 */
public record SemanticSearchResultDTO(
    List<MediaDTO> media,
    String summary
) {
}
