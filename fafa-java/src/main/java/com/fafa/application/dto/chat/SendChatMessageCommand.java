package com.fafa.application.dto.chat;

/** 发送 AI 消息命令。 */
public record SendChatMessageCommand(Long sessionId, Long petId, String message,
                                     String clientMessageId) {
}
