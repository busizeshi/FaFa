package com.fafa.application.dto.content;

import java.time.LocalDateTime;

public record UpsertContentScheduleCommand(Long userId, Long petId, String contentType,
                                           String topicType, int intervalDays,
                                           LocalDateTime nextRunAt) {
}
