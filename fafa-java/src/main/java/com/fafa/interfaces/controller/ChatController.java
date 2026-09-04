package com.fafa.interfaces.controller;

import com.fafa.application.dto.PageDTO;
import com.fafa.application.dto.chat.*;
import com.fafa.application.service.ChatApplicationService;
import com.fafa.application.service.ChatRecommendationService;
import com.fafa.common.Result;
import com.fafa.interfaces.dto.chat.CreateChatSessionRequest;
import com.fafa.interfaces.dto.chat.SendChatMessageRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** AI 助手公开接口。 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "AI 助手", description = "AI 会话、数据问答和自然语言记录草稿")
public class ChatController {

    private final ChatApplicationService chatApplicationService;
    private final ChatRecommendationService chatRecommendationService;

    @PostMapping("/sessions")
    @Operation(summary = "创建 AI 会话")
    public Result<ChatSessionDTO> createSession(@Valid @RequestBody(required = false)
                                                 CreateChatSessionRequest request) {
        return Result.ok(chatApplicationService.createSession(request == null ? null : request.petId()));
    }

    @GetMapping("/sessions")
    @Operation(summary = "查询 AI 会话列表")
    public Result<PageDTO<ChatSessionDTO>> listSessions(
            @Parameter(description = "宠物过滤条件；不传表示查询全部会话", example = "1") @RequestParam(required = false) Long petId,
            @Parameter(description = "页码，从1开始", example = "1") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页会话数", example = "20") @RequestParam(defaultValue = "20") int size) {
        return Result.ok(chatApplicationService.listSessions(petId, page, size));
    }

    @GetMapping("/sessions/{sessionId}/messages")
    @Operation(summary = "查询 AI 会话消息")
    public Result<List<ChatMessageDTO>> listMessages(
            @Parameter(description = "会话ID", required = true, example = "1") @PathVariable Long sessionId,
            @Parameter(description = "消息游标；只返回该消息之前的记录，不传表示从最新消息开始", example = "100") @RequestParam(required = false) Long beforeId,
            @Parameter(description = "最多返回的消息条数", example = "50") @RequestParam(defaultValue = "50") int limit) {
        return Result.ok(chatApplicationService.listMessages(sessionId, beforeId, limit));
    }

    @PostMapping("/sessions/{sessionId}/messages")
    @Operation(summary = "发送 AI 消息")
    public Result<ChatReplyDTO> sendMessage(
            @Parameter(description = "会话ID") @PathVariable Long sessionId,
            @Valid @RequestBody SendChatMessageRequest request) {
        return Result.ok(chatApplicationService.sendMessage(new SendChatMessageCommand(
                sessionId, request.petId(), request.message(), request.clientMessageId())));
    }

    @DeleteMapping("/sessions/{sessionId}")
    @Operation(summary = "删除 AI 会话")
    public Result<Void> deleteSession(
            @Parameter(description = "会话ID", required = true, example = "1") @PathVariable Long sessionId) {
        chatApplicationService.deleteSession(sessionId);
        return Result.ok();
    }

    @GetMapping("/recommendations")
    @Operation(summary = "查询 AI 推荐问题")
    public Result<RecommendationDTO> recommendations(
            @Parameter(description = "宠物过滤条件；不传表示通用推荐", example = "1") @RequestParam(required = false) Long petId,
            @Parameter(description = "最多返回的推荐问题数", example = "6") @RequestParam(defaultValue = "6") int limit) {
        return Result.ok(chatRecommendationService.getRecommendations(petId, limit));
    }
}
