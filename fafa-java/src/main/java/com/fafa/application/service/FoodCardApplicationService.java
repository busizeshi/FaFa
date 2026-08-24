package com.fafa.application.service;

import cn.dev33.satoken.stp.StpUtil;
import com.fafa.application.dto.food.FoodCardCommand;
import com.fafa.application.dto.food.FoodCardDTO;
import com.fafa.common.BusinessException;
import com.fafa.common.ErrorCode;
import com.fafa.domain.model.food.FoodCard;
import com.fafa.domain.model.food.FoodType;
import com.fafa.domain.repository.FeedingPlanRepository;
import com.fafa.domain.repository.FoodCardRepository;
import com.fafa.domain.repository.FoodTransitionPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/** 食品卡应用服务，负责归属校验、事务编排和 DTO 转换。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FoodCardApplicationService {

    private final FoodCardRepository repository;
    private final PetApplicationService petApplicationService;
    private final FeedingPlanRepository feedingPlanRepository;
    private final FoodTransitionPlanRepository foodTransitionPlanRepository;

    @Transactional
    public FoodCardDTO create(FoodCardCommand command) {
        Long userId = currentUserId();
        petApplicationService.checkPetOwnership(command.petId());
        FoodCard foodCard;
        try {
            foodCard = FoodCard.create(userId, command.petId(), command.name(), command.brand(),
                    parseType(command.foodType()), command.specification(), command.packageWeight(),
                    command.packageWeightUnit(), command.price(), command.currency(), command.openedAt(),
                    command.expiresAt(), command.nutritionBasis(), command.proteinPercent(), command.fatPercent(),
                    command.fiberPercent(), command.moisturePercent(), command.ashPercent(), command.calciumPercent(),
                    command.phosphorusPercent(), command.energyKcal(), command.energyUnit(), command.proteinSources(),
                    command.ingredients(), command.additiveFlags(), command.lifeStageLabel(), command.sourceMediaId(),
                    command.sourceRecognitionTaskId(), command.notes());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.FOOD_FIELD_INVALID, ex.getMessage());
        }
        FoodCard saved = repository.save(foodCard);
        log.info("食品卡创建成功: userId={}, petId={}, foodId={}", userId, command.petId(), saved.getId());
        return toDTO(saved);
    }

    public List<FoodCardDTO> list(Long petId, String status) {
        petApplicationService.checkPetOwnership(petId);
        return repository.findByUserIdAndPetId(currentUserId(), petId, status).stream().map(this::toDTO).toList();
    }

    public FoodCardDTO get(Long id) {
        return toDTO(findOwned(id));
    }

    public Optional<FoodCardDTO> findBySourceRecognitionTaskId(Long taskId) {
        return repository.findBySourceRecognitionTaskId(taskId).map(this::toDTO);
    }

    @Transactional
    public FoodCardDTO update(Long id, FoodCardCommand command) {
        FoodCard foodCard = findOwned(id);
        if (!foodCard.getPetId().equals(command.petId())) {
            throw new BusinessException(ErrorCode.FOOD_FIELD_INVALID, "食品卡所属宠物不能修改");
        }
        try {
            foodCard.update(command.name(), command.brand(), parseType(command.foodType()), command.specification(),
                    command.packageWeight(), command.packageWeightUnit(), command.price(), command.currency(),
                    command.openedAt(), command.expiresAt(), command.nutritionBasis(), command.proteinPercent(),
                    command.fatPercent(), command.fiberPercent(), command.moisturePercent(), command.ashPercent(),
                    command.calciumPercent(), command.phosphorusPercent(), command.energyKcal(), command.energyUnit(),
                    command.proteinSources(), command.ingredients(), command.additiveFlags(), command.lifeStageLabel(),
                    command.notes());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.FOOD_FIELD_INVALID, ex.getMessage());
        }
        return toDTO(repository.save(foodCard));
    }

    @Transactional
    public FoodCardDTO archive(Long id) {
        FoodCard foodCard = findOwned(id);
        ensureNotInActivePlan(foodCard);
        foodCard.archive();
        log.info("食品卡归档: userId={}, foodId={}", currentUserId(), id);
        return toDTO(repository.save(foodCard));
    }

    @Transactional
    public FoodCardDTO activate(Long id) {
        FoodCard foodCard = findOwned(id);
        foodCard.activate();
        return toDTO(repository.save(foodCard));
    }

    @Transactional
    public void delete(Long id) {
        FoodCard foodCard = findOwned(id);
        ensureNotInActivePlan(foodCard);
        repository.deleteById(foodCard.getId());
        log.info("食品卡删除成功: userId={}, foodId={}", currentUserId(), id);
    }

    private FoodCard findOwned(Long id) {
        return repository.findByIdAndUserId(id, currentUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.FOOD_NOT_FOUND));
    }

    private FoodType parseType(String code) {
        try {
            return FoodType.fromCode(code);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.FOOD_FIELD_INVALID, ex.getMessage());
        }
    }

    private FoodCardDTO toDTO(FoodCard foodCard) {
        return new FoodCardDTO(foodCard.getId(), foodCard.getPetId(), foodCard.getName(), foodCard.getBrand(),
                foodCard.getFoodType().getCode(), foodCard.getStatus().getCode(), foodCard.getSpecification(),
                foodCard.getPackageWeight(), foodCard.getPackageWeightUnit(), foodCard.getPrice(),
                foodCard.getCurrency(), foodCard.getOpenedAt(), foodCard.getExpiresAt(), foodCard.getNutritionBasis(),
                foodCard.getProteinPercent(), foodCard.getFatPercent(), foodCard.getFiberPercent(),
                foodCard.getMoisturePercent(), foodCard.getAshPercent(), foodCard.getCalciumPercent(),
                foodCard.getPhosphorusPercent(), foodCard.getEnergyKcal(), foodCard.getEnergyUnit(),
                foodCard.getProteinSources(), foodCard.getIngredients(), foodCard.getAdditiveFlags(),
                foodCard.getLifeStageLabel(), foodCard.getSourceMediaId(), foodCard.getSourceRecognitionTaskId(),
                foodCard.getNotes(), nutritionCompleteness(foodCard), foodCard.getCreatedAt(), foodCard.getUpdatedAt());
    }

    private double nutritionCompleteness(FoodCard foodCard) {
        int total = 5;
        int present = 0;
        present += foodCard.getProteinPercent() != null ? 1 : 0;
        present += foodCard.getFatPercent() != null ? 1 : 0;
        present += foodCard.getEnergyKcal() != null ? 1 : 0;
        present += foodCard.getCalciumPercent() != null ? 1 : 0;
        present += foodCard.getPhosphorusPercent() != null ? 1 : 0;
        return BigDecimal.valueOf(present).divide(BigDecimal.valueOf(total), 2, java.math.RoundingMode.HALF_UP).doubleValue();
    }

    private Long currentUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    private void ensureNotInActivePlan(FoodCard foodCard) {
        Long userId = currentUserId();
        if (feedingPlanRepository.existsActiveUsingFood(userId, foodCard.getId())
                || foodTransitionPlanRepository.existsActiveUsingFood(userId, foodCard.getId())) {
            throw new BusinessException(ErrorCode.FOOD_IN_ACTIVE_PLAN);
        }
    }
}
