package com.fafa.application.dto.content;

import java.time.LocalDateTime;

public record ContentScheduleDTO(Long id, Long petId, String contentType, String topicType,
                                 int intervalDays, LocalDateTime nextRunAt, LocalDateTime lastRunAt,
                                 boolean enabled, LocalDateTime updatedAt) {
}
