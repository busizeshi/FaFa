package com.fafa.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fafa.domain.model.food.FoodTransitionPlan;
import com.fafa.domain.model.food.FoodTransitionStatus;
import com.fafa.domain.repository.FoodTransitionPlanRepository;
import com.fafa.infrastructure.persistence.mapper.FoodTransitionPlanMapper;
import com.fafa.infrastructure.persistence.po.FoodTransitionPlanPO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** 换粮计划仓储实现。 */
@Repository
@RequiredArgsConstructor
public class FoodTransitionPlanRepositoryImpl implements FoodTransitionPlanRepository {

    private final FoodTransitionPlanMapper mapper;
    private final ObjectMapper objectMapper;

    @Override
    public FoodTransitionPlan save(FoodTransitionPlan plan) {
        FoodTransitionPlanPO po = toPO(plan);
        if (po.getId() == null) mapper.insert(po); else mapper.updateById(po);
        return toDomain(mapper.selectById(po.getId()));
    }

    @Override
    public Optional<FoodTransitionPlan> findByIdAndUserId(Long id, Long userId) {
        LambdaQueryWrapper<FoodTransitionPlanPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FoodTransitionPlanPO::getId, id).eq(FoodTransitionPlanPO::getUserId, userId);
        return Optional.ofNullable(toDomain(mapper.selectOne(wrapper)));
    }

    @Override
    public List<FoodTransitionPlan> findByPetId(Long userId, Long petId, String status) {
        LambdaQueryWrapper<FoodTransitionPlanPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FoodTransitionPlanPO::getUserId, userId).eq(FoodTransitionPlanPO::getPetId, petId);
        if (status != null && !status.isBlank()) wrapper.eq(FoodTransitionPlanPO::getStatus, status);
        wrapper.orderByDesc(FoodTransitionPlanPO::getStartDate).orderByDesc(FoodTransitionPlanPO::getId);
        return mapper.selectList(wrapper).stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existsActiveUsingFood(Long userId, Long foodId) {
        LambdaQueryWrapper<FoodTransitionPlanPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FoodTransitionPlanPO::getUserId, userId)
                .eq(FoodTransitionPlanPO::getStatus, FoodTransitionStatus.ACTIVE.getCode())
                .and(condition -> condition.eq(FoodTransitionPlanPO::getOldFoodId, foodId)
                        .or().eq(FoodTransitionPlanPO::getNewFoodId, foodId));
        return mapper.selectCount(wrapper) > 0;
    }

    private FoodTransitionPlan toDomain(FoodTransitionPlanPO po) {
        if (po == null) return null;
        return FoodTransitionPlan.restore(po.getId(), po.getUserId(), po.getPetId(), po.getOldFoodId(),
                po.getNewFoodId(), po.getStartDate(), po.getEndDate(), po.getDailyTargetAmount(), po.getUnit(),
                readSteps(po.getRatioSteps()), po.getReactionDate(), po.getReactionCode(), po.getReactionNote(),
                FoodTransitionStatus.fromCode(po.getStatus()), Boolean.TRUE.equals(po.getIsDeleted()),
                po.getCreatedAt(), po.getUpdatedAt());
    }

    private FoodTransitionPlanPO toPO(FoodTransitionPlan plan) {
        FoodTransitionPlanPO po = new FoodTransitionPlanPO();
        po.setId(plan.getId()); po.setUserId(plan.getUserId()); po.setPetId(plan.getPetId());
        po.setOldFoodId(plan.getOldFoodId()); po.setNewFoodId(plan.getNewFoodId());
        po.setStartDate(plan.getStartDate()); po.setEndDate(plan.getEndDate());
        po.setDailyTargetAmount(plan.getDailyTargetAmount()); po.setUnit(plan.getUnit());
        try { po.setRatioSteps(objectMapper.writeValueAsString(plan.getRatioSteps())); }
        catch (Exception ex) { throw new IllegalStateException("换粮比例序列化失败", ex); }
        po.setReactionDate(plan.getReactionDate()); po.setReactionCode(plan.getReactionCode());
        po.setReactionNote(plan.getReactionNote());
        po.setStatus(plan.getStatus().getCode()); po.setIsDeleted(plan.isDeleted());
        return po;
    }

    private List<FoodTransitionPlan.RatioStep> readSteps(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() { });
        } catch (Exception ex) {
            throw new IllegalStateException("换粮比例 JSON 无效", ex);
        }
    }
}
