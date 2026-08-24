package com.fafa.interfaces.dto.chat;

import jakarta.validation.constraints.Positive;

/** 创建 AI 会话请求。 */
public record CreateChatSessionRequest(@Positive(message = "petId 必须为正数") Long petId) {
}
