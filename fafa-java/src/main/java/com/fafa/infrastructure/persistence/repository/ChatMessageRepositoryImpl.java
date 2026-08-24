package com.fafa.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fafa.domain.model.chat.ChatMessage;
import com.fafa.domain.model.chat.ChatMessageRole;
import com.fafa.domain.repository.ChatMessageRepository;
import com.fafa.infrastructure.persistence.mapper.ChatMessageMapper;
import com.fafa.infrastructure.persistence.po.ChatMessagePO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** AI 消息仓储实现。 */
@Repository
@RequiredArgsConstructor
public class ChatMessageRepositoryImpl implements ChatMessageRepository {

    private final ChatMessageMapper mapper;

    @Override
    public ChatMessage save(ChatMessage message) {
        ChatMessagePO po = toPO(message);
        if (po.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        return toDomain(mapper.selectById(po.getId()));
    }

    @Override
    public List<ChatMessage> findBySessionId(Long sessionId, Long userId, Long beforeId, int limit) {
        LambdaQueryWrapper<ChatMessagePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessagePO::getSessionId, sessionId)
                .eq(ChatMessagePO::getUserId, userId)
                .lt(beforeId != null && beforeId > 0, ChatMessagePO::getId, beforeId)
                .orderByDesc(ChatMessagePO::getId)
                .last("LIMIT " + limit);
        return mapper.selectList(wrapper).stream().map(this::toDomain).toList().reversed();
    }

    @Override
    public Optional<ChatMessage> findUserMessageByClientId(Long sessionId, Long userId,
                                                            String clientMessageId) {
        LambdaQueryWrapper<ChatMessagePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessagePO::getSessionId, sessionId)
                .eq(ChatMessagePO::getUserId, userId)
                .eq(ChatMessagePO::getClientMessageId, clientMessageId)
                .eq(ChatMessagePO::getRole, ChatMessageRole.USER.getCode())
                .last("LIMIT 1");
        return Optional.ofNullable(toDomain(mapper.selectOne(wrapper)));
    }

    @Override
    public Optional<ChatMessage> findLatestAssistantAfter(Long sessionId, Long userId, Long messageId) {
        LambdaQueryWrapper<ChatMessagePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessagePO::getSessionId, sessionId)
                .eq(ChatMessagePO::getUserId, userId)
                .eq(ChatMessagePO::getRole, ChatMessageRole.ASSISTANT.getCode())
                .gt(ChatMessagePO::getId, messageId)
                .orderByAsc(ChatMessagePO::getId)
                .last("LIMIT 1");
        return Optional.ofNullable(toDomain(mapper.selectOne(wrapper)));
    }

    private ChatMessage toDomain(ChatMessagePO po) {
        if (po == null) {
            return null;
        }
        return ChatMessage.restore(po.getId(), po.getSessionId(), po.getUserId(), po.getPetId(),
                ChatMessageRole.fromCode(po.getRole()), po.getContent(), po.getClientMessageId(),
                po.getBlocksJson(), po.getCitationsJson(), po.getToolCallsJson(),
                po.getRecordDraftJson(), po.getModel(), po.getPromptTokens(),
                po.getCompletionTokens(), po.getAssistantStatus(), po.getTraceId(), po.getCreatedAt());
    }

    private ChatMessagePO toPO(ChatMessage message) {
        ChatMessagePO po = new ChatMessagePO();
        po.setId(message.getId());
        po.setSessionId(message.getSessionId());
        po.setUserId(message.getUserId());
        po.setPetId(message.getPetId());
        po.setRole(message.getRole().getCode());
        po.setContent(message.getContent());
        po.setClientMessageId(message.getClientMessageId());
        po.setBlocksJson(message.getBlocksJson());
        po.setCitationsJson(message.getCitationsJson());
        po.setToolCallsJson(message.getToolCallsJson());
        po.setRecordDraftJson(message.getRecordDraftJson());
        po.setModel(message.getModel());
        po.setPromptTokens(message.getPromptTokens());
        po.setCompletionTokens(message.getCompletionTokens());
        po.setAssistantStatus(message.getAssistantStatus());
        po.setTraceId(message.getTraceId());
        return po;
    }
}
