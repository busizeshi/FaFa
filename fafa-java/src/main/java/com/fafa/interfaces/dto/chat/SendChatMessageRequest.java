package com.fafa.interfaces.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** 发送 AI 消息请求。 */
public record SendChatMessageRequest(
        @Positive(message = "petId 必须为正数") Long petId,
        @NotBlank(message = "消息不能为空")
        @Size(max = 8000, message = "消息不能超过8000字符") String message,
        @Size(max = 64, message = "clientMessageId 不能超过64字符") String clientMessageId
) {
}
