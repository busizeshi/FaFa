package com.fafa.application.service;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fafa.application.dto.PageDTO;
import com.fafa.application.dto.chat.*;
import com.fafa.application.port.ChatAgentGateway;
import com.fafa.common.BusinessException;
import com.fafa.common.ErrorCode;
import com.fafa.domain.model.chat.ChatMessage;
import com.fafa.domain.model.chat.ChatSession;
import com.fafa.domain.repository.ChatMessageRepository;
import com.fafa.domain.repository.ChatSessionRepository;
import com.fafa.infrastructure.config.FaFaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AI 会话应用服务。
 *
 * Java 负责会话权限、消息持久化和响应白名单过滤，Python 只负责 Agent 编排。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatApplicationService {

    private static final int DEFAULT_PAGE = 1;
    private static final int MAX_PAGE_SIZE = 50;
    private static final String[] ALLOWED_BLOCK_TYPES = {"stat", "chart", "media", "link", "notice"};

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final ChatAgentGateway chatAgentGateway;
    private final PetApplicationService petApplicationService;
    private final FaFaProperties properties;
    private final ObjectMapper objectMapper;

    @Transactional
    public ChatSessionDTO createSession(Long petId) {
        Long userId = currentUserId();
        if (petId != null) {
            petApplicationService.checkPetOwnership(petId);
        }
        ChatSession saved = sessionRepository.save(ChatSession.create(userId, petId, LocalDateTime.now()));
        log.info("创建 AI 会话: userId={}, sessionId={}, petId={}", userId, saved.getId(), petId);
        return toSessionDTO(saved);
    }

    public PageDTO<ChatSessionDTO> listSessions(Long petId, int page, int size) {
        Long userId = currentUserId();
        int safePage = page < DEFAULT_PAGE ? DEFAULT_PAGE : page;
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int offset = (safePage - 1) * safeSize;
        List<ChatSessionDTO> records = sessionRepository.findByUserId(userId, petId, offset, safeSize)
                .stream().map(this::toSessionDTO).toList();
        return PageDTO.of(records, sessionRepository.countByUserId(userId, petId), safePage, safeSize);
    }

    public List<ChatMessageDTO> listMessages(Long sessionId, Long beforeId, int limit) {
        ChatSession session = ownedSession(sessionId);
        int safeLimit = Math.min(Math.max(limit, 1), MAX_PAGE_SIZE);
        return messageRepository.findBySessionId(session.getId(), session.getUserId(), beforeId, safeLimit)
                .stream().map(this::toMessageDTO).toList();
    }

    @Transactional
    public void deleteSession(Long sessionId) {
        ChatSession session = ownedSession(sessionId);
        session.delete(LocalDateTime.now());
        sessionRepository.save(session);
        log.info("删除 AI 会话: userId={}, sessionId={}", session.getUserId(), sessionId);
    }

    /** 发送消息时不持有数据库事务，避免模型调用期间占用连接。 */
    public ChatReplyDTO sendMessage(SendChatMessageCommand command) {
        Long userId = currentUserId();
        validateMessage(command.message());
        ChatSession session = ownedSession(command.sessionId());
        session.ensureActive();
        Long petId = command.petId() != null ? command.petId() : session.getPetId();
        if (petId != null) {
            petApplicationService.checkPetOwnership(petId);
        }
        if (command.clientMessageId() != null && !command.clientMessageId().isBlank()) {
            ChatReplyDTO duplicate = findDuplicate(session, command.clientMessageId());
            if (duplicate != null) {
                return duplicate;
            }
        }

        LocalDateTime now = LocalDateTime.now();
        ChatMessage userMessage = messageRepository.save(ChatMessage.user(session.getId(), userId, petId,
                command.message(), command.clientMessageId(), currentTraceId(), now));
        session.bindPet(petId, now);
        session.setTitleIfBlank(command.message(), now);
        sessionRepository.save(session);

        if (petId == null) {
            ChatMessage assistant = messageRepository.save(ChatMessage.assistant(session.getId(), userId, null,
                    "请先选择一只宠物，我才能查询它的历史记录。", null, null, null, null,
                    null, null, null, currentTraceId(), LocalDateTime.now()));
            return toReply(session, userMessage, assistant, null);
        }

        ChatAgentResponse agentResponse = chatAgentGateway.chat(new ChatAgentRequest(
                userId, petId, command.message(), String.valueOf(session.getId()),
                String.valueOf(userMessage.getId()), historyForAgent(session, userMessage.getId()),
                currentTraceId()));

        ChatAgentResponse.ChatAgentUsage usage = agentResponse.usage();
        ChatMessage assistant = messageRepository.save(ChatMessage.assistant(session.getId(), userId, petId,
                agentResponse.answer(), jsonArray(agentResponse.blocks()), jsonArray(agentResponse.citations()),
                jsonAnyArray(agentResponse.toolCalls()), jsonNode(agentResponse.recordDraft()),
                usage == null ? null : usage.model(), usage == null ? null : usage.promptTokens(),
                usage == null ? null : usage.completionTokens(), currentTraceId(), LocalDateTime.now()));
        session.touch(LocalDateTime.now());
        sessionRepository.save(session);
        log.info("AI 对话完成: userId={}, sessionId={}, messageId={}, assistantMessageId={}",
                userId, session.getId(), userMessage.getId(), assistant.getId());
        return toReply(session, userMessage, assistant, agentResponse);
    }

    private List<ChatAgentRequest.HistoryItem> historyForAgent(ChatSession session, Long beforeId) {
        return messageRepository.findBySessionId(session.getId(), session.getUserId(), beforeId,
                        properties.getChat().getHistoryMessageLimit())
                .stream()
                .filter(message -> message.getRole().getCode().equals("user")
                        || message.getRole().getCode().equals("assistant"))
                .map(message -> new ChatAgentRequest.HistoryItem(message.getRole().getCode(), message.getContent()))
                .toList();
    }

    private ChatReplyDTO findDuplicate(ChatSession session, String clientMessageId) {
        return messageRepository.findUserMessageByClientId(session.getId(), session.getUserId(), clientMessageId)
                .map(message -> messageRepository.findLatestAssistantAfter(session.getId(), session.getUserId(),
                                message.getId())
                        .map(assistant -> toReply(session, message, assistant, null))
                        .orElseThrow(() -> new BusinessException(ErrorCode.AI_CHAT_DUPLICATE)))
                .orElse(null);
    }

    private ChatReplyDTO toReply(ChatSession session, ChatMessage user, ChatMessage assistant,
                                 ChatAgentResponse response) {
        List<JsonNode> blocks = parseArray(assistant.getBlocksJson());
        List<JsonNode> citations = parseArray(assistant.getCitationsJson());
        List<String> suggestions = response == null ? Collections.emptyList()
                : response.suggestedQuestions() == null ? Collections.emptyList() : response.suggestedQuestions();
        return new ChatReplyDTO(session.getId(), user.getId(), assistant.getId(), assistant.getPetId(),
                assistant.getContent(), blocks, citations, suggestions,
                parseNode(assistant.getRecordDraftJson()), assistant.getTraceId());
    }

    private ChatMessageDTO toMessageDTO(ChatMessage message) {
        return new ChatMessageDTO(message.getId(), message.getSessionId(), message.getRole().getCode(),
                message.getContent(), parseArray(message.getBlocksJson()), parseArray(message.getCitationsJson()),
                parseNode(message.getRecordDraftJson()), message.getAssistantStatus(), message.getCreatedAt());
    }

    private ChatSessionDTO toSessionDTO(ChatSession session) {
        return new ChatSessionDTO(session.getId(), session.getPetId(), session.getTitle(),
                session.getStatus().getCode(), session.getLastMessageAt(), session.getCreatedAt());
    }

    private ChatSession ownedSession(Long sessionId) {
        Long userId = currentUserId();
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AI_CHAT_SESSION_NOT_FOUND));
        try {
            session.ensureOwnedBy(userId);
        } catch (IllegalStateException ex) {
            throw new BusinessException(ErrorCode.AI_CHAT_SESSION_NOT_FOUND);
        }
        return session;
    }

    private void validateMessage(String message) {
        if (message == null || message.isBlank()
                || message.trim().length() > properties.getChat().getMaxMessageLength()) {
            throw new BusinessException(ErrorCode.AI_CHAT_MESSAGE_INVALID);
        }
    }

    private String jsonArray(List<JsonNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return null;
        }
        List<JsonNode> safeNodes = nodes.stream().filter(this::isAllowedNode).toList();
        if (safeNodes.isEmpty()) {
            return null;
        }
        return writeJson(safeNodes);
    }

    private String jsonAnyArray(List<JsonNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return null;
        }
        return writeJson(nodes);
    }

    private boolean isAllowedNode(JsonNode node) {
        if (node == null || !node.isObject()) {
            return false;
        }
        if (!node.hasNonNull("type")) {
            return node.has("sourceType") || node.has("source_type");
        }
        String type = node.get("type").asText();
        for (String allowed : ALLOWED_BLOCK_TYPES) {
            if (allowed.equals(type)) {
                return true;
            }
        }
        return false;
    }

    private String jsonNode(JsonNode node) {
        return node == null || node.isNull() ? null : writeJson(node);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.AI_CHAT_TOOL_FAILED, "AI 响应格式化失败");
        }
    }

    private List<JsonNode> parseArray(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            if (!node.isArray()) {
                return List.of();
            }
            List<JsonNode> result = new ArrayList<>();
            node.forEach(item -> {
                if (isAllowedNode(item)) {
                    result.add(item);
                }
            });
            return result;
        } catch (JsonProcessingException ex) {
            log.warn("AI 消息结构化字段解析失败");
            return List.of();
        }
    }

    private JsonNode parseNode(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    private Long currentUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    private String currentTraceId() {
        String traceId = org.slf4j.MDC.get("traceId");
        return traceId == null || traceId.isBlank() ? "no-trace" : traceId;
    }
}
