package com.fafa.domain.repository;

import com.fafa.domain.model.food.FoodTransitionPlan;

import java.util.List;
import java.util.Optional;

/** 换粮计划仓储。 */
public interface FoodTransitionPlanRepository {
    FoodTransitionPlan save(FoodTransitionPlan plan);
    Optional<FoodTransitionPlan> findByIdAndUserId(Long id, Long userId);
    List<FoodTransitionPlan> findByPetId(Long userId, Long petId, String status);
    boolean existsActiveUsingFood(Long userId, Long foodId);
}
