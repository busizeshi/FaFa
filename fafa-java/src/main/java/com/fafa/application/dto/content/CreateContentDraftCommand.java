package com.fafa.application.dto.content;

import java.time.LocalDate;
import java.util.List;

public record CreateContentDraftCommand(Long userId, Long petId, String contentType,
                                        String topicType, String customTopic, String periodType,
                                        LocalDate anchorDate, List<Long> selectedMediaIds,
                                        String idempotencyKey, String trigger) {
}
