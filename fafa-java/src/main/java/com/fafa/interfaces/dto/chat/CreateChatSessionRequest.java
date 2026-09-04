package com.fafa.interfaces.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;

/** 创建 AI 会话请求。 */
@Schema(description = "创建 AI 会话请求")
public record CreateChatSessionRequest(
        @Schema(description = "关联宠物ID；不传表示创建通用会话", example = "1")
        @Positive(message = "petId 必须为正数") Long petId) {
}
