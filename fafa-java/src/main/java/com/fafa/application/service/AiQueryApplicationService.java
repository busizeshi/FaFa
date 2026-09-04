package com.fafa.application.service;

import com.fafa.application.dto.chat.*;
import com.fafa.common.BusinessException;
import com.fafa.common.ErrorCode;
import com.fafa.domain.model.event.EventRecord;
import com.fafa.domain.model.food.FoodCard;
import com.fafa.domain.model.feeding.FeedingPlan;
import com.fafa.domain.model.feeding.FeedingPlanItem;
import com.fafa.domain.model.pet.Pet;
import com.fafa.domain.model.record.*;
import com.fafa.domain.model.reminder.ReminderCompletion;
import com.fafa.domain.repository.*;
import com.fafa.application.dto.expense.ExpenseDTO;
import com.fafa.application.dto.expense.ExpenseStatisticsDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Agent 只读数据应用服务。
 *
 * 该服务是 Python 查询业务数据的唯一入口，所有方法都显式接收 userId，
 * 不依赖 Python 传入的宠物上下文作为授权依据。
 */
@Service
@RequiredArgsConstructor
public class AiQueryApplicationService {

    private static final int MAX_RANGE_DAYS = 366;

    private final PetRepository petRepository;
    private final DailyStatusRepository dailyStatusRepository;
    private final FeedingRecordRepository feedingRecordRepository;
    private final WeightRecordRepository weightRecordRepository;
    private final EventRecordRepository eventRecordRepository;
    private final ReminderCompletionRepository reminderCompletionRepository;
    private final com.fafa.domain.repository.FoodCardRepository foodCardRepository;
    private final com.fafa.domain.repository.FeedingPlanRepository feedingPlanRepository;
    private final ExpenseApplicationService expenseApplicationService;
    private final ExpenseStatisticsApplicationService expenseStatisticsApplicationService;

    public AiPetProfileDTO getPetProfile(Long userId, Long petId) {
        Pet pet = ownedPet(userId, petId);
        WeightRecord latest = weightRecordRepository.findLatestByPetId(petId).orElse(null);
        return new AiPetProfileDTO(pet.getId(), pet.getName(), pet.getType().getCode(), pet.getBreed(),
                pet.getGender().getCode(), pet.getBirthDate(),
                latest != null ? latest.getWeight() : pet.getWeight(),
                latest != null ? latest.getRecordDate() : null);
    }

    public AiDailyStatusDTO getDailyStatus(Long userId, Long petId, LocalDate start, LocalDate end) {
        validateRange(start, end);
        ownedPet(userId, petId);
        List<DailyStatus> records = dailyStatusRepository.findByPetIdAndDateRange(petId, start, end);
        List<AiDailyStatusDTO.AiDailyStatusRecordDTO> result = records.stream()
                .map(this::toDailyStatus)
                .toList();
        int totalDays = (int) ChronoUnit.DAYS.between(start, end) + 1;
        int refusedDays = (int) records.stream()
                .filter(record -> record.getAppetite() != null
                        && "refused".equals(record.getAppetite().getCode()))
                .count();
        return new AiDailyStatusDTO(petId, start, end, result, records.size(),
                Math.max(totalDays - records.size(), 0), refusedDays);
    }

    public AiWeightHistoryDTO getWeightHistory(Long userId, Long petId,
                                               LocalDate start, LocalDate end) {
        validateRange(start, end);
        ownedPet(userId, petId);
        List<AiWeightHistoryDTO.AiWeightRecordDTO> records = weightRecordRepository
                .findByPetIdAndDateRange(petId, start, end).stream()
                .map(record -> new AiWeightHistoryDTO.AiWeightRecordDTO(record.getId(),
                        record.getRecordDate(), record.getWeight(), record.getNotes()))
                .toList();
        return new AiWeightHistoryDTO(petId, start, end, records);
    }

    public AiFeedingHistoryDTO getFeedingHistory(Long userId, Long petId,
                                                  LocalDate start, LocalDate end) {
        validateRange(start, end);
        ownedPet(userId, petId);
        List<AiFeedingHistoryDTO.AiFeedingRecordDTO> records = feedingRecordRepository
                .findByPetIdAndDateRange(petId, start, end).stream()
                .map(record -> new AiFeedingHistoryDTO.AiFeedingRecordDTO(record.getId(),
                        record.getFeedingTime(), record.getFoodType().getCode(), record.getAmount(),
                        record.getUnit(), record.getNotes()))
                .toList();
        return new AiFeedingHistoryDTO(petId, start, end, records, records.size());
    }

    public AiEventHistoryDTO getEventHistory(Long userId, Long petId,
                                             LocalDate start, LocalDate end) {
        validateRange(start, end);
        ownedPet(userId, petId);
        List<AiEventHistoryDTO.AiEventDTO> records = eventRecordRepository
                .findByPetIdAndDateRange(petId, start, end).stream()
                .map(this::toEvent)
                .toList();
        return new AiEventHistoryDTO(petId, start, end, records, records.size());
    }

    public AiReminderHistoryDTO getReminderHistory(Long userId, Long petId,
                                                   LocalDate start, LocalDate end) {
        validateRange(start, end);
        ownedPet(userId, petId);
        List<AiReminderHistoryDTO.AiReminderCompletionDTO> records = reminderCompletionRepository
                .findByUserIdAndTimeBetween(userId, start.atStartOfDay(), end.plusDays(1).atStartOfDay())
                .stream()
                .filter(completion -> petId.equals(completion.getPetId()))
                .map(this::toReminder)
                .toList();
        return new AiReminderHistoryDTO(petId, start, end, records, records.size());
    }

    /** AI 只读查询食品卡，返回字段保持可审计的事实结构。 */
    public AiFoodQueryDTO getFoodInfo(Long userId, Long petId, String status) {
        ownedPet(userId, petId);
        List<AiFoodQueryDTO.FoodFactDTO> records = foodCardRepository.findByUserIdAndPetId(userId, petId, status)
                .stream().map(this::toFoodFact).toList();
        return new AiFoodQueryDTO(records, records.size());
    }

    /** AI 只读查询喂食计划。 */
    public AiFeedingPlanQueryDTO getFeedingPlans(Long userId, Long petId, String status) {
        ownedPet(userId, petId);
        List<AiFeedingPlanQueryDTO.PlanFactDTO> records = feedingPlanRepository.findByPetId(userId, petId, status)
                .stream().map(this::toPlanFact).toList();
        return new AiFeedingPlanQueryDTO(records, records.size());
    }

    /** AI 只读查询开支事实，不暴露任何写入能力。 */
    public AiExpenseQueryDTO getExpenses(Long userId, Long petId, LocalDate start, LocalDate end) {
        List<ExpenseDTO> records = expenseApplicationService.listForUser(userId, petId, start, end);
        return new AiExpenseQueryDTO(records, records.size());
    }

    /** AI 只读查询开支统计。 */
    public ExpenseStatisticsDTO getExpenseStatistics(Long userId, Long petId, String period,
                                                     LocalDate anchorDate) {
        return expenseStatisticsApplicationService.statisticsForUser(userId, petId, period, anchorDate);
    }

    private Pet ownedPet(Long userId, Long petId) {
        if (userId == null || petId == null) {
            throw new BusinessException(ErrorCode.AI_CHAT_PET_FORBIDDEN);
        }
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AI_CHAT_PET_FORBIDDEN));
        if (!userId.equals(pet.getUserId())) {
            throw new BusinessException(ErrorCode.AI_CHAT_PET_FORBIDDEN);
        }
        return pet;
    }

    private void validateRange(LocalDate start, LocalDate end) {
        if (start == null || end == null || end.isBefore(start)
                || ChronoUnit.DAYS.between(start, end) >= MAX_RANGE_DAYS) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "查询日期范围必须为 1 到 " + MAX_RANGE_DAYS + " 天");
        }
    }

    private AiDailyStatusDTO.AiDailyStatusRecordDTO toDailyStatus(DailyStatus record) {
        return new AiDailyStatusDTO.AiDailyStatusRecordDTO(record.getRecordDate(),
                code(record.getAppetite()), code(record.getEnergy()), code(record.getWaterIntake()),
                code(record.getDefecation()), code(record.getVomiting()), code(record.getUrination()),
                record.getAppetiteAmount(), record.getWaterAmount(), record.getNotes());
    }

    private AiEventHistoryDTO.AiEventDTO toEvent(EventRecord record) {
        return new AiEventHistoryDTO.AiEventDTO(record.getId(), record.getSource().getCode(),
                record.getEventType().getCode(), record.getTitle(), record.getContent(),
                record.getEventDate(), record.getTags());
    }

    private AiReminderHistoryDTO.AiReminderCompletionDTO toReminder(ReminderCompletion completion) {
        return new AiReminderHistoryDTO.AiReminderCompletionDTO(completion.getId(),
                completion.getReminderId(), completion.getReminderTitle(), completion.getReminderType(),
                completion.getCompletionTime(), completion.getNotes());
    }

    private String code(Appetite value) { return value == null ? null : value.getCode(); }
    private String code(Energy value) { return value == null ? null : value.getCode(); }
    private String code(WaterIntake value) { return value == null ? null : value.getCode(); }
    private String code(Defecation value) { return value == null ? null : value.getCode(); }
    private String code(Vomiting value) { return value == null ? null : value.getCode(); }
    private String code(Urination value) { return value == null ? null : value.getCode(); }

    private AiFoodQueryDTO.FoodFactDTO toFoodFact(FoodCard food) {
        return new AiFoodQueryDTO.FoodFactDTO(food.getId(), food.getName(), food.getBrand(),
                food.getFoodType().getCode(), food.getStatus().getCode(), food.getProteinPercent(),
                food.getFatPercent(), food.getEnergyKcal(), food.getEnergyUnit());
    }

    private AiFeedingPlanQueryDTO.PlanFactDTO toPlanFact(FeedingPlan plan) {
        List<AiFeedingPlanQueryDTO.ItemFactDTO> items = plan.getItems().stream()
                .map(this::toPlanItemFact)
                .toList();
        return new AiFeedingPlanQueryDTO.PlanFactDTO(plan.getId(), plan.getName(), plan.getStartDate(),
                plan.getEndDate(), plan.getStatus().getCode(), plan.getDailyTargetAmount(),
                plan.getDailyTargetCalories(), items);
    }

    private AiFeedingPlanQueryDTO.ItemFactDTO toPlanItemFact(FeedingPlanItem item) {
        return new AiFeedingPlanQueryDTO.ItemFactDTO(item.id(), item.itemKey(), item.foodId(), item.mealTime(),
                item.amount(), item.unit(), item.percentage(), item.cycleType(), item.cycleValue(),
                item.nutritionSnapshot(), item.sortOrder());
    }
}
