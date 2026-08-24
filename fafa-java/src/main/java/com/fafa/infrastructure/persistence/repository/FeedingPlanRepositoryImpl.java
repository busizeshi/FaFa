package com.fafa.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fafa.domain.model.feeding.FeedingPlan;
import com.fafa.domain.model.feeding.FeedingPlanItem;
import com.fafa.domain.model.feeding.FeedingPlanStatus;
import com.fafa.domain.repository.FeedingPlanRepository;
import com.fafa.infrastructure.persistence.mapper.FeedingPlanItemMapper;
import com.fafa.infrastructure.persistence.mapper.FeedingPlanMapper;
import com.fafa.infrastructure.persistence.po.FeedingPlanItemPO;
import com.fafa.infrastructure.persistence.po.FeedingPlanPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** 喂食计划仓储实现。 */
@Repository
@RequiredArgsConstructor
public class FeedingPlanRepositoryImpl implements FeedingPlanRepository {

    private final FeedingPlanMapper planMapper;
    private final FeedingPlanItemMapper itemMapper;

    @Override
    public FeedingPlan save(FeedingPlan plan) {
        FeedingPlanPO po = toPO(plan);
        if (po.getId() == null) planMapper.insert(po); else planMapper.updateById(po);
        for (FeedingPlanItem item : plan.getItems()) {
            FeedingPlanItemPO itemPO = toItemPO(po.getId(), item);
            if (itemPO.getId() == null) itemMapper.insert(itemPO); else itemMapper.updateById(itemPO);
        }
        return toDomain(planMapper.selectById(po.getId()));
    }

    @Override
    public Optional<FeedingPlan> findByIdAndUserId(Long id, Long userId) {
        LambdaQueryWrapper<FeedingPlanPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FeedingPlanPO::getId, id).eq(FeedingPlanPO::getUserId, userId);
        return Optional.ofNullable(toDomain(planMapper.selectOne(wrapper)));
    }

    @Override
    public List<FeedingPlan> findByPetId(Long userId, Long petId, String status) {
        LambdaQueryWrapper<FeedingPlanPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FeedingPlanPO::getUserId, userId).eq(FeedingPlanPO::getPetId, petId);
        if (status != null && !status.isBlank()) wrapper.eq(FeedingPlanPO::getStatus, status);
        wrapper.orderByDesc(FeedingPlanPO::getStartDate).orderByDesc(FeedingPlanPO::getId);
        return planMapper.selectList(wrapper).stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existsActiveUsingFood(Long userId, Long foodId) {
        LambdaQueryWrapper<FeedingPlanPO> planWrapper = new LambdaQueryWrapper<>();
        planWrapper.eq(FeedingPlanPO::getUserId, userId)
                .eq(FeedingPlanPO::getStatus, FeedingPlanStatus.ACTIVE.getCode());
        List<Long> activePlanIds = planMapper.selectList(planWrapper).stream()
                .map(FeedingPlanPO::getId).toList();
        if (activePlanIds.isEmpty()) {
            return false;
        }
        LambdaQueryWrapper<FeedingPlanItemPO> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.in(FeedingPlanItemPO::getFeedingPlanId, activePlanIds)
                .eq(FeedingPlanItemPO::getFoodId, foodId);
        return itemMapper.selectCount(itemWrapper) > 0;
    }

    private FeedingPlan toDomain(FeedingPlanPO po) {
        if (po == null) return null;
        LambdaQueryWrapper<FeedingPlanItemPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FeedingPlanItemPO::getFeedingPlanId, po.getId())
                .orderByAsc(FeedingPlanItemPO::getSortOrder).orderByAsc(FeedingPlanItemPO::getMealTime);
        List<FeedingPlanItem> items = itemMapper.selectList(wrapper).stream().map(this::toItem).toList();
        return FeedingPlan.restore(po.getId(), po.getUserId(), po.getPetId(), po.getName(), po.getStartDate(),
                po.getEndDate(), po.getDailyTargetCalories(), po.getDailyTargetAmount(), po.getUnit(),
                po.getRecommendationSource(), po.getPolicyVersion(), po.getVersion(),
                FeedingPlanStatus.fromCode(po.getStatus()), po.getAcceptedAt(), po.getNotes(), items,
                Boolean.TRUE.equals(po.getIsDeleted()), po.getCreatedAt(), po.getUpdatedAt());
    }

    private FeedingPlanPO toPO(FeedingPlan plan) {
        FeedingPlanPO po = new FeedingPlanPO();
        po.setId(plan.getId()); po.setUserId(plan.getUserId()); po.setPetId(plan.getPetId());
        po.setName(plan.getName()); po.setStartDate(plan.getStartDate()); po.setEndDate(plan.getEndDate());
        po.setDailyTargetCalories(plan.getDailyTargetCalories()); po.setDailyTargetAmount(plan.getDailyTargetAmount());
        po.setUnit(plan.getUnit()); po.setRecommendationSource(plan.getRecommendationSource());
        po.setPolicyVersion(plan.getPolicyVersion()); po.setVersion(plan.getVersion());
        po.setStatus(plan.getStatus().getCode()); po.setAcceptedAt(plan.getAcceptedAt()); po.setNotes(plan.getNotes());
        po.setIsDeleted(plan.isDeleted());
        return po;
    }

    private FeedingPlanItemPO toItemPO(Long planId, FeedingPlanItem item) {
        FeedingPlanItemPO po = new FeedingPlanItemPO();
        po.setId(item.id()); po.setFeedingPlanId(planId); po.setItemKey(item.itemKey()); po.setFoodId(item.foodId());
        po.setMealTime(item.mealTime()); po.setAmount(item.amount()); po.setUnit(item.unit().toLowerCase());
        po.setPercentage(item.percentage()); po.setCycleType(item.cycleType()); po.setCycleValue(item.cycleValue());
        po.setNutritionSnapshot(item.nutritionSnapshot()); po.setSortOrder(item.sortOrder()); po.setIsDeleted(false);
        return po;
    }

    private FeedingPlanItem toItem(FeedingPlanItemPO po) {
        return new FeedingPlanItem(po.getId(), po.getItemKey(), po.getFoodId(), po.getMealTime(), po.getAmount(),
                po.getUnit(), po.getPercentage(), po.getCycleType(), po.getCycleValue(),
                po.getNutritionSnapshot(), po.getSortOrder());
    }
}
