package com.fafa.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fafa.domain.model.chat.ChatSession;
import com.fafa.domain.model.chat.ChatSessionStatus;
import com.fafa.domain.repository.ChatSessionRepository;
import com.fafa.infrastructure.persistence.mapper.ChatSessionMapper;
import com.fafa.infrastructure.persistence.po.ChatSessionPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** AI 会话仓储实现。 */
@Repository
@RequiredArgsConstructor
public class ChatSessionRepositoryImpl implements ChatSessionRepository {

    private final ChatSessionMapper mapper;

    @Override
    public ChatSession save(ChatSession session) {
        ChatSessionPO po = toPO(session);
        if (po.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        return toDomain(mapper.selectById(po.getId()));
    }

    @Override
    public Optional<ChatSession> findById(Long id) {
        return Optional.ofNullable(toDomain(mapper.selectById(id)));
    }

    @Override
    public List<ChatSession> findByUserId(Long userId, Long petId, int offset, int limit) {
        LambdaQueryWrapper<ChatSessionPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatSessionPO::getUserId, userId)
                .eq(ChatSessionPO::getStatus, ChatSessionStatus.ACTIVE.getCode())
                .eq(petId != null, ChatSessionPO::getPetId, petId)
                .orderByDesc(ChatSessionPO::getLastMessageAt)
                .orderByDesc(ChatSessionPO::getId)
                .last("LIMIT " + offset + "," + limit);
        return mapper.selectList(wrapper).stream().map(this::toDomain).toList();
    }

    @Override
    public long countByUserId(Long userId, Long petId) {
        LambdaQueryWrapper<ChatSessionPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatSessionPO::getUserId, userId)
                .eq(ChatSessionPO::getStatus, ChatSessionStatus.ACTIVE.getCode())
                .eq(petId != null, ChatSessionPO::getPetId, petId);
        return mapper.selectCount(wrapper);
    }

    private ChatSession toDomain(ChatSessionPO po) {
        if (po == null) {
            return null;
        }
        return ChatSession.restore(po.getId(), po.getUserId(), po.getPetId(), po.getTitle(),
                ChatSessionStatus.fromCode(po.getStatus()), po.getLastMessageAt(),
                po.getCreatedAt(), po.getUpdatedAt());
    }

    private ChatSessionPO toPO(ChatSession session) {
        ChatSessionPO po = new ChatSessionPO();
        po.setId(session.getId());
        po.setUserId(session.getUserId());
        po.setPetId(session.getPetId());
        po.setTitle(session.getTitle());
        po.setStatus(session.getStatus().getCode());
        po.setLastMessageAt(session.getLastMessageAt());
        po.setIsDeleted(session.getStatus() == ChatSessionStatus.DELETED);
        return po;
    }
}
