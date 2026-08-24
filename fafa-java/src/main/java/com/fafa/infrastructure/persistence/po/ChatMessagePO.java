package com.fafa.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** AI 消息持久化对象。 */
@Data
@TableName("ai_chat_message")
public class ChatMessagePO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private Long userId;
    private Long petId;
    private String role;
    private String content;
    private String clientMessageId;
    private String blocksJson;
    private String citationsJson;
    private String toolCallsJson;
    private String recordDraftJson;
    private String model;
    private Integer promptTokens;
    private Integer completionTokens;
    private String assistantStatus;
    private String traceId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
