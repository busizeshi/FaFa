package com.fafa.domain.model.chat;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ChatMessageTest {

    @Test
    void shouldRejectBlankMessage() {
        assertThrows(IllegalArgumentException.class, () -> ChatMessage.user(
                1L, 3L, 1L, " ", null, "trace", LocalDateTime.now()));
    }

    @Test
    void shouldRejectOversizedMessage() {
        String message = "x".repeat(ChatMessage.MAX_CONTENT_LENGTH + 1);

        assertThrows(IllegalArgumentException.class, () -> ChatMessage.user(
                1L, 3L, 1L, message, null, "trace", LocalDateTime.now()));
    }
}
