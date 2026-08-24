package com.fafa.application.service;

import cn.dev33.satoken.stp.StpUtil;
import com.fafa.common.BusinessException;
import com.fafa.common.ErrorCode;
import com.fafa.domain.model.food.FoodCard;
import com.fafa.domain.model.food.FoodTransitionPlan;
import com.fafa.domain.model.food.FoodTransitionStatus;
import com.fafa.domain.repository.FoodCardRepository;
import com.fafa.domain.repository.FoodTransitionPlanRepository;
import com.fafa.infrastructure.config.FaFaProperties;
import com.fafa.application.dto.food.FoodTransitionDTO;
import com.fafa.interfaces.dto.food.SaveFoodTransitionRequest;
import com.fafa.interfaces.dto.food.UpdateFoodReactionRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.List;

/** 换粮计划应用服务。 */
@Service
@RequiredArgsConstructor
public class FoodTransitionApplicationService {

    private final FoodTransitionPlanRepository repository;
    private final FoodCardRepository foodCardRepository;
    private final PetApplicationService petApplicationService;
    private final FaFaProperties properties;

    @Transactional
    public FoodTransitionDTO create(SaveFoodTransitionRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        petApplicationService.checkPetOwnership(request.getPetId());
        FoodCard oldFood = findFood(userId, request.getOldFoodId(), request.getPetId());
        FoodCard newFood = findFood(userId, request.getNewFoodId(), request.getPetId());
        if (!oldFood.canBeUsedForNewPlan() || !newFood.canBeUsedForNewPlan()) {
            throw new BusinessException(ErrorCode.FOOD_TRANSITION_INVALID, "旧粮和新粮必须为 active 食品卡");
        }
        long days = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;
        if (days < properties.getFood().getTransition().getMinDays()
                || days > properties.getFood().getTransition().getMaxDays()) {
            throw new BusinessException(ErrorCode.FOOD_TRANSITION_INVALID, "换粮天数超出配置范围");
        }
        List<FoodTransitionPlan.RatioStep> steps = request.getRatioSteps().stream()
                .map(item -> new FoodTransitionPlan.RatioStep(item.getDate(), item.getOldPercent(), item.getNewPercent()))
                .toList();
        FoodTransitionPlan plan;
        try {
            plan = FoodTransitionPlan.create(userId, request.getPetId(), oldFood.getId(), newFood.getId(),
                    request.getStartDate(), request.getEndDate(), request.getDailyTargetAmount(), request.getUnit(),
                    steps, FoodTransitionStatus.fromCode(request.getStatus() == null ? "active" : request.getStatus()));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.FOOD_TRANSITION_INVALID, ex.getMessage());
        }
        return toDTO(repository.save(plan));
    }

    public List<FoodTransitionDTO> list(Long petId, String status) {
        petApplicationService.checkPetOwnership(petId);
        return repository.findByPetId(StpUtil.getLoginIdAsLong(), petId, status).stream().map(this::toDTO).toList();
    }

    public FoodTransitionDTO get(Long id) {
        return toDTO(findOwned(id));
    }

    @Transactional
    public FoodTransitionDTO updateReaction(Long id, UpdateFoodReactionRequest request) {
        FoodTransitionPlan plan = findOwned(id);
        try {
            plan.updateReaction(request.getDate(), request.getReaction(), request.getNote());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.FOOD_TRANSITION_INVALID, ex.getMessage());
        }
        return toDTO(repository.save(plan));
    }

    @Transactional
    public FoodTransitionDTO complete(Long id) {
        FoodTransitionPlan plan = findOwned(id);
        try { plan.complete(); } catch (IllegalStateException ex) {
            throw new BusinessException(ErrorCode.FOOD_TRANSITION_INVALID, ex.getMessage());
        }
        return toDTO(repository.save(plan));
    }

    @Transactional
    public FoodTransitionDTO cancel(Long id) {
        FoodTransitionPlan plan = findOwned(id);
        try { plan.cancel(); } catch (IllegalStateException ex) {
            throw new BusinessException(ErrorCode.FOOD_TRANSITION_INVALID, ex.getMessage());
        }
        return toDTO(repository.save(plan));
    }

    private FoodCard findFood(Long userId, Long foodId, Long petId) {
        return foodCardRepository.findByIdAndUserId(foodId, userId)
                .filter(food -> food.getPetId().equals(petId))
                .orElseThrow(() -> new BusinessException(ErrorCode.FOOD_TRANSITION_INVALID, "食品卡不属于当前宠物"));
    }

    private FoodTransitionPlan findOwned(Long id) {
        return repository.findByIdAndUserId(id, StpUtil.getLoginIdAsLong())
                .orElseThrow(() -> new BusinessException(ErrorCode.FOOD_TRANSITION_INVALID, "换粮计划不存在"));
    }

    private FoodTransitionDTO toDTO(FoodTransitionPlan plan) {
        return new FoodTransitionDTO(plan.getId(), plan.getPetId(), plan.getOldFoodId(), plan.getNewFoodId(),
                plan.getStartDate(), plan.getEndDate(), plan.getDailyTargetAmount(), plan.getUnit(),
                plan.getRatioSteps().stream().map(item -> new FoodTransitionDTO.RatioStepDTO(item.date(),
                item.oldPercent(), item.newPercent())).toList(), plan.getReactionDate(), plan.getReactionCode(),
                plan.getReactionNote(), plan.getStatus().getCode());
    }
}
