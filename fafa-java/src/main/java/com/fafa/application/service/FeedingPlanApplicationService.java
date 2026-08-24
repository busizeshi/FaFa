package com.fafa.application.service;

import cn.dev33.satoken.stp.StpUtil;
import com.fafa.application.dto.feeding.FeedingChecklistDTO;
import com.fafa.application.dto.feeding.FeedingPlanDTO;
import com.fafa.application.dto.feeding.FeedingRecommendationDTO;
import com.fafa.common.BusinessException;
import com.fafa.common.ErrorCode;
import com.fafa.domain.model.feeding.FeedingPlan;
import com.fafa.domain.model.feeding.FeedingPlanItem;
import com.fafa.domain.model.feeding.FeedingPlanStatus;
import com.fafa.domain.model.food.FoodCard;
import com.fafa.domain.model.record.FeedingRecord;
import com.fafa.domain.repository.FeedingPlanRepository;
import com.fafa.domain.repository.FeedingRecordRepository;
import com.fafa.domain.repository.FoodCardRepository;
import com.fafa.infrastructure.config.FaFaProperties;
import com.fafa.interfaces.dto.feeding.FeedingRecommendationRequest;
import com.fafa.interfaces.dto.feeding.SaveFeedingPlanRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 喂食计划应用服务。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedingPlanApplicationService {

    private static final String DEFAULT_UNIT = "g";
    private static final String DEFAULT_CYCLE = "daily";

    private final FeedingPlanRepository repository;
    private final FoodCardRepository foodCardRepository;
    private final FeedingRecordRepository feedingRecordRepository;
    private final PetApplicationService petApplicationService;
    private final FaFaProperties properties;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public FeedingRecommendationDTO recommend(FeedingRecommendationRequest request) {
        petApplicationService.checkPetOwnership(request.getPetId());
        if (request.getFoodIds() == null || request.getFoodIds().isEmpty()) {
            throw new BusinessException(ErrorCode.FEEDING_PLAN_INVALID, "至少选择一张 active 食品卡");
        }
        List<LocalTime> times = request.getMealTimes() == null ? List.of() : request.getMealTimes();
        if (times.isEmpty() || (request.getMealCount() != null && request.getMealCount() != times.size())) {
            throw new BusinessException(ErrorCode.FEEDING_PLAN_INVALID, "餐次和时段数量必须一致");
        }
        if (times.size() > properties.getFeeding().getMaxItemsPerPlan()
                || times.size() != times.stream().distinct().count()) {
            throw new BusinessException(ErrorCode.FEEDING_PLAN_INVALID, "计划项数量超过限制");
        }
        Map<Long, FoodCard> foods = loadActiveFoods(request.getFoodIds(), request.getPetId());
        BigDecimal perMeal = request.getDailyTargetAmount() == null ? null
                : request.getDailyTargetAmount().divide(BigDecimal.valueOf(times.size()), 3, RoundingMode.HALF_UP);
        BigDecimal percentage = BigDecimal.valueOf(100).divide(BigDecimal.valueOf(times.size()), 2, RoundingMode.HALF_UP);
        List<FeedingRecommendationDTO.ItemDTO> items = new java.util.ArrayList<>();
        for (int index = 0; index < times.size(); index++) {
            Long foodId = request.getFoodIds().get(Math.min(index, request.getFoodIds().size() - 1));
            FoodCard food = foods.get(foodId);
            items.add(new FeedingRecommendationDTO.ItemDTO("meal-" + (index + 1), foodId,
                    food.getName(), times.get(index), perMeal, normalizeUnit(request.getUnit()), percentage));
        }
        return new FeedingRecommendationDTO("rec-" + UUID.randomUUID(), "rule", properties.getFeeding().getPolicyVersion(),
                request.getTargetCalories(), request.getDailyTargetAmount(), normalizeUnit(request.getUnit()),
                request.getDailyTargetAmount() != null || request.getTargetCalories() != null, items,
                List.of("建议仅作为计划草稿，请结合包装建议和实际记录确认"),
                List.of("这不是处方或固定减重方案", "未提供目标量时不会虚构每餐克数"));
    }

    @Transactional
    public FeedingPlanDTO create(SaveFeedingPlanRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        petApplicationService.checkPetOwnership(request.getPetId());
        if (request.getItems() == null || request.getItems().isEmpty()
                || request.getItems().size() > properties.getFeeding().getMaxItemsPerPlan()) {
            throw new BusinessException(ErrorCode.FEEDING_PLAN_INVALID, "计划项数量超过限制");
        }
        Map<Long, FoodCard> foods = loadActiveFoods(request.getItems().stream().map(SaveFeedingPlanRequest.ItemRequest::getFoodId).toList(),
                request.getPetId());
        FeedingPlan plan;
        try {
            List<FeedingPlanItem> items = new java.util.ArrayList<>();
            for (int index = 0; index < request.getItems().size(); index++) {
                SaveFeedingPlanRequest.ItemRequest item = request.getItems().get(index);
                FoodCard food = foods.get(item.getFoodId());
                items.add(new FeedingPlanItem(null, item.getItemKey() == null ? "meal-" + (index + 1) : item.getItemKey(),
                        food.getId(), item.getMealTime(), item.getAmount(), normalizeUnit(item.getUnit()),
                        item.getPercentage(), item.getCycleType() == null ? DEFAULT_CYCLE : item.getCycleType(),
                        item.getCycleValue(), nutritionSnapshot(food), index));
            }
            plan = FeedingPlan.create(userId, request.getPetId(), request.getName(), request.getStartDate(),
                    request.getEndDate(), request.getDailyTargetCalories(), request.getDailyTargetAmount(), request.getUnit(),
                    request.getRecommendationSource() == null ? "user_input" : request.getRecommendationSource(),
                    request.getPolicyVersion() == null ? properties.getFeeding().getPolicyVersion() : request.getPolicyVersion(),
                    request.getNotes(), items);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.FEEDING_PLAN_INVALID, ex.getMessage());
        }
        log.info("喂食计划创建成功: userId={}, petId={}, itemCount={}", userId, request.getPetId(), plan.getItems().size());
        return toDTO(repository.save(plan));
    }

    public List<FeedingPlanDTO> list(Long petId, String status) {
        petApplicationService.checkPetOwnership(petId);
        return repository.findByPetId(StpUtil.getLoginIdAsLong(), petId, status).stream().map(this::toDTO).toList();
    }

    public FeedingPlanDTO get(Long id) {
        return toDTO(findOwned(id));
    }

    public FeedingChecklistDTO today(Long id, LocalDate date) {
        FeedingPlan plan = findOwned(id);
        if (!plan.appliesOn(date)) {
            return new FeedingChecklistDTO(id, date, plan.getStatus().getCode(), List.of(), 0, 0);
        }
        List<FeedingRecord> records = feedingRecordRepository.findByPetIdAndDate(plan.getPetId(), date);
        Map<Long, FeedingRecord> completed = new HashMap<>();
        records.stream().filter(record -> id.equals(record.getFeedingPlanId()) && record.getFeedingPlanItemId() != null)
                .forEach(record -> completed.putIfAbsent(record.getFeedingPlanItemId(), record));
        List<FeedingChecklistDTO.ItemDTO> items = plan.getItems().stream().map(item -> {
            FeedingRecord record = completed.get(item.id());
            String status = record == null ? "pending" : "completed";
            FoodCard food = foodCardRepository.findByIdAndUserId(item.foodId(), plan.getUserId()).orElse(null);
            return new FeedingChecklistDTO.ItemDTO(item.id(), item.itemKey(), item.foodId(),
                    food == null ? "已删除食品" : food.getName(),
                    LocalDateTime.of(date, item.mealTime()), item.amount(), item.unit(), status,
                    record == null ? null : record.getId(), record == null ? null : record.getFeedingTime());
        }).toList();
        int completedCount = (int) items.stream().filter(item -> "completed".equals(item.completionStatus())).count();
        return new FeedingChecklistDTO(id, date, plan.getStatus().getCode(), items, completedCount, items.size());
    }

    @Transactional
    public FeedingPlanDTO pause(Long id) { return changeStatus(id, FeedingPlanStatus.PAUSED); }

    @Transactional
    public FeedingPlanDTO resume(Long id) { return changeStatus(id, FeedingPlanStatus.ACTIVE); }

    @Transactional
    public FeedingPlanDTO complete(Long id) {
        FeedingPlan plan = findOwned(id);
        try { plan.complete(); } catch (IllegalStateException ex) {
            throw new BusinessException(ErrorCode.FEEDING_PLAN_INVALID, ex.getMessage());
        }
        return toDTO(repository.save(plan));
    }

    private FeedingPlanDTO changeStatus(Long id, FeedingPlanStatus status) {
        FeedingPlan plan = findOwned(id);
        try {
            if (status == FeedingPlanStatus.PAUSED) plan.pause(); else plan.resume();
        } catch (IllegalStateException ex) {
            throw new BusinessException(ErrorCode.FEEDING_PLAN_INVALID, ex.getMessage());
        }
        return toDTO(repository.save(plan));
    }

    private Map<Long, FoodCard> loadActiveFoods(List<Long> foodIds, Long petId) {
        Long userId = StpUtil.getLoginIdAsLong();
        Map<Long, FoodCard> foods = new HashMap<>();
        for (Long foodId : foodIds) {
            FoodCard food = foodCardRepository.findByIdAndUserId(foodId, userId)
                    .filter(item -> item.getPetId().equals(petId) && item.canBeUsedForNewPlan())
                    .orElseThrow(() -> new BusinessException(ErrorCode.FEEDING_PLAN_INVALID, "食品卡不可用于当前计划"));
            foods.put(foodId, food);
        }
        return foods;
    }

    private String nutritionSnapshot(FoodCard food) {
        try {
            Map<String, Object> snapshot = new HashMap<>();
            snapshot.put("foodName", food.getName());
            snapshot.put("energyKcal", food.getEnergyKcal());
            snapshot.put("energyUnit", food.getEnergyUnit());
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.FEEDING_PLAN_INVALID, "营养快照生成失败");
        }
    }

    private FeedingPlan findOwned(Long id) {
        return repository.findByIdAndUserId(id, StpUtil.getLoginIdAsLong())
                .orElseThrow(() -> new BusinessException(ErrorCode.FEEDING_PLAN_NOT_FOUND));
    }

    private FeedingPlanDTO toDTO(FeedingPlan plan) {
        return new FeedingPlanDTO(plan.getId(), plan.getPetId(), plan.getName(), plan.getStartDate(), plan.getEndDate(),
                plan.getDailyTargetCalories(), plan.getDailyTargetAmount(), plan.getUnit(), plan.getRecommendationSource(),
                plan.getPolicyVersion(), plan.getVersion(), plan.getStatus().getCode(), plan.getAcceptedAt(), plan.getNotes(),
                plan.getItems().stream().map(item -> new FeedingPlanDTO.ItemDTO(item.id(), item.itemKey(), item.foodId(),
                        item.mealTime(), item.amount(), item.unit(), item.percentage(), item.cycleType(), item.cycleValue(),
                        item.sortOrder())).toList(), plan.getCreatedAt(), plan.getUpdatedAt());
    }

    private String normalizeUnit(String value) {
        return value == null || value.isBlank() ? DEFAULT_UNIT : value.trim().toLowerCase();
    }
}
