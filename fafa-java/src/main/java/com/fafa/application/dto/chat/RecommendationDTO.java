package com.fafa.application.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** AI 助手推荐问题。 */
@Schema(description = "AI 推荐问题")
public record RecommendationDTO(
        @Schema(description = "关联宠物ID；通用推荐时为空", example = "1") Long petId,
        @Schema(description = "推荐问题列表") List<Item> items) {
    @Schema(description = "推荐问题项")
    public record Item(
            @Schema(description = "推荐问题ID", example = "weight_trend") String id,
            @Schema(description = "推荐问题文本", example = "小白最近的体重变化怎么样？") String text,
            @Schema(description = "问题分类", example = "health") String category) {
    }
}
