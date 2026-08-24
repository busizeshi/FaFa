package com.fafa.domain.model.chat;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ChatSessionTest {

    @Test
    void shouldBindPetAndDeleteSession() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 23, 15, 0);
        ChatSession session = ChatSession.create(3L, null, now);

        session.bindPet(1L, now.plusMinutes(1));
        session.setTitleIfBlank("豆包最近体重怎么样？", now.plusMinutes(1));

        assertEquals(1L, session.getPetId());
        assertEquals("豆包最近体重怎么样？", session.getTitle());
        assertEquals(1L, session.resolvePetId(null));

        session.delete(now.plusMinutes(2));
        assertEquals(ChatSessionStatus.DELETED, session.getStatus());
        assertThrows(IllegalStateException.class, session::ensureActive);
    }

    @Test
    void shouldRejectOtherUser() {
        ChatSession session = ChatSession.create(3L, 1L, LocalDateTime.now());

        assertThrows(IllegalStateException.class, () -> session.ensureOwnedBy(4L));
    }
}
