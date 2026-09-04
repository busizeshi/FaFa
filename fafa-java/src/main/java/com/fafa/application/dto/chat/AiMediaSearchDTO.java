package com.fafa.application.dto.chat;

import com.fafa.application.dto.media.MediaDTO;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** Agent 素材语义检索响应。 */
@Schema(description = "AI 素材语义检索响应")
public record AiMediaSearchDTO(
                               @Schema(description = "命中素材列表") List<MediaDTO> results,
                               @Schema(description = "本次请求是否扣除额度", example = "true") boolean charged,
                               @Schema(description = "扣除的额度类型", example = "ai_search") String quotaType,
                               @Schema(description = "本月剩余额度；-1 表示不限") Integer remaining) {
}
