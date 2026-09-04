package com.fafa.interfaces.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** 发送 AI 消息请求。 */
@Schema(description = "发送 AI 消息请求")
public record SendChatMessageRequest(
        @Schema(description = "关联宠物ID；可选，用于限定 AI 上下文", example = "1")
        @Positive(message = "petId 必须为正数") Long petId,
        @Schema(description = "发送给 AI 的消息内容", example = "小白最近的体重变化怎么样？")
        @NotBlank(message = "消息不能为空")
        @Size(max = 8000, message = "消息不能超过8000字符") String message,
        @Schema(description = "客户端消息幂等标识，用于避免重复提交", example = "client-msg-001")
        @Size(max = 64, message = "clientMessageId 不能超过64字符") String clientMessageId
) {
}
