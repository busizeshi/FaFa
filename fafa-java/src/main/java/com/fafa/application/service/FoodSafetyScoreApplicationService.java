package com.fafa.application.service;

import cn.dev33.satoken.stp.StpUtil;
import com.fafa.application.dto.food.FoodSafetyScoreDTO;
import com.fafa.common.BusinessException;
import com.fafa.common.ErrorCode;
import com.fafa.domain.model.food.FoodCard;
import com.fafa.domain.repository.FoodCardRepository;
import com.fafa.domain.service.FoodSafetyScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 食品提示分应用服务。 */
@Service
@RequiredArgsConstructor
public class FoodSafetyScoreApplicationService {

    private final FoodCardRepository foodCardRepository;
    private final FoodSafetyScoreService scoreService;

    public FoodSafetyScoreDTO get(Long foodId) {
        FoodCard food = foodCardRepository.findByIdAndUserId(foodId, StpUtil.getLoginIdAsLong())
                .orElseThrow(() -> new BusinessException(ErrorCode.FOOD_NOT_FOUND));
        FoodSafetyScoreService.FoodSafetyScore score = scoreService.calculate(food);
        return new FoodSafetyScoreDTO(foodId, score.score(), score.label(), score.policyVersion(),
                score.dimensions().stream().map(item -> new FoodSafetyScoreDTO.DimensionDTO(
                        item.key(), item.label(), item.score(), item.maxScore(), item.status(), item.evidence())).toList(),
                score.missingFields(), score.warnings());
    }
}
