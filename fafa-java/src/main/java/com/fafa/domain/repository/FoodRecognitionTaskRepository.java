package com.fafa.domain.repository;

import com.fafa.domain.model.food.FoodRecognitionTask;

import java.util.Optional;

/** 食品识别任务仓储。 */
public interface FoodRecognitionTaskRepository {

    Optional<FoodRecognitionTask> findByIdAndUserId(Long id, Long userId);

    Optional<FoodRecognitionTask> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

    FoodRecognitionTask save(FoodRecognitionTask task);
}
