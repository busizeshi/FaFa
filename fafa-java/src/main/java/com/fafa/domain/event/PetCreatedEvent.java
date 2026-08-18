package com.fafa.domain.event;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 宠物创建事件
 * 
 * 领域事件：用于解耦不同聚合之间的交互
 * 
 * @author FaFa Team
 * @since 1.0
 */
@Getter
public class PetCreatedEvent {

    /**
     * 宠物 ID
     */
    private final Long petId;

    /**
     * 用户 ID
     */
    private final Long userId;

    /**
     * 事件发生时间
     */
    private final LocalDateTime occurredAt;

    public PetCreatedEvent(Long petId, Long userId) {
        this.petId = petId;
        this.userId = userId;
        this.occurredAt = LocalDateTime.now();
    }
}
