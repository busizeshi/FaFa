package com.fafa.application.dto.food;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 食品识别任务返回对象。 */
public record FoodRecognitionDTO(
        Long taskId,
        Long petId,
        Long mediaId,
        String status,
        Map<String, Object> draft,
        List<String> warnings,
        String model,
        String promptVersion,
        Long foodCardId,
        String errorCode,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {
}
