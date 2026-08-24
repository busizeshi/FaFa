package com.fafa.domain.repository;

import com.fafa.domain.model.feeding.FeedingPlan;

import java.util.List;
import java.util.Optional;

/** 喂食计划仓储接口。 */
public interface FeedingPlanRepository {
    FeedingPlan save(FeedingPlan plan);
    Optional<FeedingPlan> findByIdAndUserId(Long id, Long userId);
    List<FeedingPlan> findByPetId(Long userId, Long petId, String status);
    boolean existsActiveUsingFood(Long userId, Long foodId);
}
