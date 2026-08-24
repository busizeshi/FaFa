package com.fafa.application.service;

import cn.dev33.satoken.stp.StpUtil;
import com.fafa.application.dto.record.FeedingRecordDTO;
import com.fafa.application.dto.record.SaveFeedingRecordCommand;
import com.fafa.common.BusinessException;
import com.fafa.common.ErrorCode;
import com.fafa.domain.model.record.FeedingRecord;
import com.fafa.domain.model.record.FoodType;
import com.fafa.domain.model.collaboration.PetPermission;
import com.fafa.domain.repository.FeedingRecordRepository;
import com.fafa.domain.repository.FeedingPlanRepository;
import com.fafa.domain.repository.FoodCardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 喂食记录应用服务
 *
 * 负责协调喂食记录相关业务：
 * - 新增/修改/删除喂食记录（事件流，一天多条）
 * - 当天列表与范围列表查询
 *
 * @author FaFa Team
 * @since 2.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedingRecordApplicationService {

    private static final int MAX_RANGE_DAYS = 366;
    /** 喂食时间容差：允许比当前时间晚 5 分钟（覆盖「刚喂完手速提交」） */
    private static final long FUTURE_TOLERANCE_MINUTES = 5;

    private final FeedingRecordRepository feedingRecordRepository;
    private final FeedingPlanRepository feedingPlanRepository;
    private final FoodCardRepository foodCardRepository;
    private final PetApplicationService petApplicationService;
    private final PetAccessApplicationService petAccessApplicationService;

    /**
     * 新增喂食记录
     */
    @Transactional
    public FeedingRecordDTO create(SaveFeedingRecordCommand command) {
        petAccessApplicationService.require(getCurrentUserId(), command.petId(), PetPermission.CREATE_RECORD);
        validateFeedingTime(command.feedingTime());
        validateFoodAndPlan(command);
        FeedingRecord duplicate = findDuplicatePlanRecord(command);
        if (duplicate != null) {
            return toDTO(duplicate);
        }

        FeedingRecord record = FeedingRecord.create(
                command.petId(),
                command.feedingTime(),
                parseFoodType(command.foodType()),
                command.amount(),
                command.unit(),
                command.foodId(),
                command.feedingPlanId(),
                command.feedingPlanItemId(),
                command.plannedAt(),
                command.foodNameSnapshot(),
                command.notes(),
                getCurrentUserId()
        );

        FeedingRecord saved = feedingRecordRepository.save(record);
        log.info("喂食记录新增成功: petId={}, time={}, foodType={}",
                command.petId(), command.feedingTime(), command.foodType());
        return toDTO(saved);
    }

    /**
     * 修改喂食记录
     */
    @Transactional
    public FeedingRecordDTO update(Long id, SaveFeedingRecordCommand command) {
        FeedingRecord record = findOwnedRecord(id);
        petAccessApplicationService.requireOwner(getCurrentUserId(), record.getPetId());
        validateFeedingTime(command.feedingTime());
        validateFoodAndPlan(command);

        record.update(
                command.feedingTime(),
                parseFoodType(command.foodType()),
                command.amount(),
                command.unit(),
                command.foodId(),
                command.feedingPlanId(),
                command.feedingPlanItemId(),
                command.plannedAt(),
                command.foodNameSnapshot(),
                command.notes()
        );

        FeedingRecord saved = feedingRecordRepository.save(record);
        log.info("喂食记录更新成功: id={}", id);
        return toDTO(saved);
    }

    /**
     * 删除喂食记录（逻辑删除）
     */
    @Transactional
    public void delete(Long id) {
        FeedingRecord record = findOwnedRecord(id);
        petAccessApplicationService.requireOwner(getCurrentUserId(), record.getPetId());
        feedingRecordRepository.deleteById(id);
        log.info("喂食记录删除成功: id={}", id);
    }

    /**
     * 当天喂食列表（按时间升序）
     */
    public List<FeedingRecordDTO> listByDate(Long petId, LocalDate date) {
        petAccessApplicationService.require(getCurrentUserId(), petId, PetPermission.READ_RECORD);
        return feedingRecordRepository.findByPetIdAndDate(petId, date).stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * 范围列表（按时间升序，范围 ≤366 天）
     */
    public List<FeedingRecordDTO> listByRange(Long petId, LocalDate start, LocalDate end) {
        petAccessApplicationService.require(getCurrentUserId(), petId, PetPermission.READ_RECORD);
        validateRange(start, end);
        return feedingRecordRepository.findByPetIdAndDateRange(petId, start, end).stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * 查询并校验记录归属（记录不存在或宠物不属于当前用户均拒绝）
     */
    private FeedingRecord findOwnedRecord(Long id) {
        FeedingRecord record = feedingRecordRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECORD_NOT_FOUND));
        petApplicationService.checkPetOwnership(record.getPetId());
        return record;
    }

    private FoodType parseFoodType(String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "食物类型不能为空");
        }
        try {
            return FoodType.fromCode(code);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "食物类型取值非法: " + code);
        }
    }

    /** 喂食时间不能晚于当前时间（5 分钟容差） */
    private void validateFeedingTime(LocalDateTime feedingTime) {
        if (feedingTime == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "喂食时间不能为空");
        }
        if (feedingTime.isAfter(LocalDateTime.now().plusMinutes(FUTURE_TOLERANCE_MINUTES))) {
            throw new BusinessException(ErrorCode.FEEDING_TIME_IN_FUTURE);
        }
    }

    private void validateRange(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "起止日期不能为空");
        }
        if (start.isAfter(end)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "开始日期不能晚于结束日期");
        }
        if (ChronoUnit.DAYS.between(start, end) > MAX_RANGE_DAYS) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "查询范围不能超过 " + MAX_RANGE_DAYS + " 天");
        }
    }

    private Long getCurrentUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    private void validateFoodAndPlan(SaveFeedingRecordCommand command) {
        Long userId = getCurrentUserId();
        if (command.foodId() != null) {
            foodCardRepository.findByIdAndUserId(command.foodId(), userId)
                    .filter(food -> food.getPetId().equals(command.petId()))
                    .orElseThrow(() -> new BusinessException(ErrorCode.FOOD_NOT_FOUND));
        }
        if (command.feedingPlanId() == null && command.feedingPlanItemId() == null) {
            return;
        }
        if (command.feedingPlanId() == null || command.feedingPlanItemId() == null) {
            throw new BusinessException(ErrorCode.FEEDING_PLAN_INVALID, "计划关联字段必须同时提供");
        }
        feedingPlanRepository.findByIdAndUserId(command.feedingPlanId(), userId)
                .filter(plan -> plan.getPetId().equals(command.petId())
                        && plan.getItems().stream().anyMatch(item -> command.feedingPlanItemId().equals(item.id())))
                .orElseThrow(() -> new BusinessException(ErrorCode.FEEDING_PLAN_INVALID, "计划项不存在"));
    }

    private FeedingRecord findDuplicatePlanRecord(SaveFeedingRecordCommand command) {
        if (command.feedingPlanId() == null || command.feedingPlanItemId() == null) {
            return null;
        }
        return feedingRecordRepository.findByPetIdAndDate(command.petId(), command.feedingTime().toLocalDate())
                .stream()
                .filter(record -> command.feedingPlanId().equals(record.getFeedingPlanId()))
                .filter(record -> command.feedingPlanItemId().equals(record.getFeedingPlanItemId()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 领域对象转DTO
     */
    private FeedingRecordDTO toDTO(FeedingRecord r) {
        return new FeedingRecordDTO(
                r.getId(),
                r.getPetId(),
                r.getFeedingTime(),
                r.getFoodType().getCode(),
                r.getAmount(),
                r.getUnit(),
                r.getFoodId(),
                r.getFeedingPlanId(),
                r.getFeedingPlanItemId(),
                r.getPlannedAt(),
                r.getFoodNameSnapshot(),
                r.getNotes(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }
}
