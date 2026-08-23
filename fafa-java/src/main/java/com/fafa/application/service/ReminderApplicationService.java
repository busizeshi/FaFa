package com.fafa.application.service;

import cn.dev33.satoken.stp.StpUtil;
import com.fafa.application.dto.reminder.*;
import com.fafa.common.BusinessException;
import com.fafa.common.ErrorCode;
import com.fafa.domain.model.event.EventRecord;
import com.fafa.domain.model.pet.Pet;
import com.fafa.domain.model.reminder.*;
import com.fafa.domain.repository.EventRecordRepository;
import com.fafa.domain.repository.ReminderCompletionRepository;
import com.fafa.domain.repository.ReminderRepository;
import com.fafa.domain.service.ReminderScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;

/**
 * 提醒应用服务
 *
 * 负责协调提醒相关业务：
 * - 提醒 CRUD 与暂停/恢复
 * - 完成闭环（完成 → 事件生成 → 历史落库 → 下期推进，事务原子性）
 * - 今日提醒视图（待办+逾期+今日已完成，跨宠物）
 * - 完成历史查询
 *
 * @author FaFa Team
 * @since 3.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderApplicationService {

    /** 完成时间容差：允许比当前时间晚 5 分钟（与喂食时间容差一致） */
    private static final long FUTURE_TOLERANCE_MINUTES = 5;

    private final ReminderRepository reminderRepository;
    private final ReminderCompletionRepository completionRepository;
    private final EventRecordRepository eventRecordRepository;
    private final ReminderScheduleService scheduleService;
    private final PetApplicationService petApplicationService;

    /**
     * 创建提醒
     */
    @Transactional
    public ReminderDTO create(CreateReminderCommand command) {
        Pet pet = petApplicationService.checkPetOwnership(command.petId());

        ReminderType type = parseEnum(ReminderType::fromCode, command.type(), "提醒类型");
        IntervalType intervalType = parseEnum(IntervalType::fromCode, command.intervalType(), "周期类型");
        validateInterval(intervalType, command.intervalValue());

        Reminder reminder = Reminder.create(
                command.petId(),
                pet.getUserId(),
                type,
                command.title(),
                command.description(),
                intervalType,
                command.intervalValue(),
                command.nextRemindTime(),
                LocalDateTime.now()
        );

        Reminder saved = reminderRepository.save(reminder);
        log.info("提醒创建成功: reminderId={}, petId={}, type={}, intervalType={}, nextRemindTime={}",
                saved.getId(), command.petId(), command.type(), command.intervalType(), command.nextRemindTime());
        return toDTO(saved, pet.getName());
    }

    /**
     * 编辑提醒（petId 不可改）
     */
    @Transactional
    public ReminderDTO update(Long id, UpdateReminderCommand command) {
        Reminder reminder = findOwnedReminder(id);

        ReminderType type = parseEnum(ReminderType::fromCode, command.type(), "提醒类型");
        IntervalType intervalType = parseEnum(IntervalType::fromCode, command.intervalType(), "周期类型");
        validateInterval(intervalType, command.intervalValue());

        try {
            reminder.update(type, command.title(), command.description(),
                    intervalType, command.intervalValue(), command.nextRemindTime(), LocalDateTime.now());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.REMINDER_TIME_IN_PAST, e.getMessage());
        }

        Reminder saved = reminderRepository.save(reminder);
        log.info("提醒更新成功: reminderId={}", id);
        return toDTO(saved, petNameOf(reminder.getPetId()));
    }

    /**
     * 删除提醒（逻辑删除，完成历史保留）
     */
    @Transactional
    public void delete(Long id) {
        findOwnedReminder(id);
        reminderRepository.deleteById(id);
        log.info("提醒删除成功: reminderId={}", id);
    }

    /**
     * 完成提醒（完成闭环核心事务）
     *
     * 业务规则：
     * 1. 仅 pending 可完成；一次性→finished；周期性→推进 nextRemindTime
     * 2. type.generatesEvent() 时生成事件记录
     * 3. 完成历史落库（含事件回链）
     */
    @Transactional
    public ReminderDTO complete(Long id, CompleteReminderCommand command) {
        Reminder reminder = findOwnedReminder(id);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime completionTime = command.completionTime() != null ? command.completionTime() : now;
        if (completionTime.isAfter(now.plusMinutes(FUTURE_TOLERANCE_MINUTES))) {
            throw new BusinessException(ErrorCode.REMINDER_COMPLETION_TIME_INVALID);
        }

        // 1. 领域完成：一次性→finished；周期性→推进 nextRemindTime
        try {
            reminder.complete(completionTime, command.notes(), scheduleService);
        } catch (IllegalStateException e) {
            throw new BusinessException(ErrorCode.REMINDER_STATE_INVALID, e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, e.getMessage());
        }
        Reminder saved = reminderRepository.save(reminder);

        // 2. 生成完成记录（先建对象，事件生成后回填回链）
        ReminderCompletion completion = ReminderCompletion.create(
                reminder, getCurrentUserId(), completionTime, command.notes(), command.photoUrls());

        // 3. 生成事件记录（白名单类型才生成）
        EventRecord event = null;
        if (reminder.getType().generatesEvent()) {
            event = eventRecordRepository.save(EventRecord.fromReminder(reminder, completion));
            completion.assignEventRecordId(event.getId());
        }

        // 4. 保存完成历史
        completionRepository.save(completion);

        log.info("提醒完成: reminderId={}, petId={}, type={}, 下次提醒={}, 事件={}",
                id, reminder.getPetId(), reminder.getType().getCode(),
                saved.getNextRemindTime(), event != null ? event.getId() : "无");
        return toDTO(saved, petNameOf(reminder.getPetId()));
    }

    /**
     * 暂停提醒
     */
    @Transactional
    public ReminderDTO pause(Long id) {
        Reminder reminder = findOwnedReminder(id);
        try {
            reminder.pause();
        } catch (IllegalStateException e) {
            throw new BusinessException(ErrorCode.REMINDER_STATE_INVALID, e.getMessage());
        }
        Reminder saved = reminderRepository.save(reminder);
        log.info("提醒暂停: reminderId={}", id);
        return toDTO(saved, petNameOf(reminder.getPetId()));
    }

    /**
     * 恢复提醒
     */
    @Transactional
    public ReminderDTO resume(Long id) {
        Reminder reminder = findOwnedReminder(id);
        try {
            reminder.resume();
        } catch (IllegalStateException e) {
            throw new BusinessException(ErrorCode.REMINDER_STATE_INVALID, e.getMessage());
        }
        Reminder saved = reminderRepository.save(reminder);
        log.info("提醒恢复: reminderId={}", id);
        return toDTO(saved, petNameOf(reminder.getPetId()));
    }

    /**
     * 提醒详情
     */
    public ReminderDTO getById(Long id) {
        Reminder reminder = findOwnedReminder(id);
        return toDTO(reminder, petNameOf(reminder.getPetId()));
    }

    /**
     * 宠物提醒列表（status 可选过滤）
     */
    public List<ReminderDTO> listByPet(Long petId, String status) {
        Pet pet = petApplicationService.checkPetOwnership(petId);

        ReminderStatus filter = null;
        if (status != null && !status.isBlank()) {
            filter = parseEnum(ReminderStatus::fromCode, status, "提醒状态");
        }

        ReminderStatus finalFilter = filter;
        return reminderRepository.findByPetId(petId).stream()
                .filter(r -> finalFilter == null || r.getStatus() == finalFilter)
                .map(r -> toDTO(r, pet.getName()))
                .toList();
    }

    /**
     * 首页今日提醒：待办（今日+逾期）与今日已完成合并
     *
     * 排序：待办在前（逾期优先→按提醒时间升序），已完成在后（按完成时间倒序）
     */
    public List<TodayReminderDTO> today() {
        Long userId = getCurrentUserId();
        LocalDate today = LocalDate.now();
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = today.plusDays(1).atStartOfDay();

        List<Reminder> due = reminderRepository.findDueByUserId(userId, dayEnd);
        List<ReminderCompletion> doneToday = completionRepository.findByUserIdAndTimeBetween(userId, dayStart, dayEnd);
        Map<Long, Pet> pets = petsById(userId);

        List<TodayReminderDTO> result = new ArrayList<>();

        // 待办项：逾期优先，其次按提醒时间升序
        due.stream()
                .sorted(Comparator
                        .comparing((Reminder r) -> !r.getNextRemindTime().isBefore(dayStart))
                        .thenComparing(Reminder::getNextRemindTime))
                .map(r -> new TodayReminderDTO(
                        toDTO(r, petName(pets, r.getPetId())),
                        r.getNextRemindTime().isBefore(dayStart),
                        false,
                        null))
                .forEach(result::add);

        // 今日已完成项：按完成时间倒序
        doneToday.stream()
                .map(c -> new TodayReminderDTO(
                        completionReminderDTO(c, petName(pets, c.getPetId())),
                        false,
                        true,
                        c.getCompletionTime()))
                .forEach(result::add);

        return result;
    }

    /**
     * 单提醒完成历史（时间倒序）
     */
    public List<CompletionDTO> history(Long reminderId) {
        Reminder reminder = findOwnedReminder(reminderId);
        Map<Long, Pet> pets = petsById(getCurrentUserId());
        return completionRepository.findByReminderId(reminderId).stream()
                .map(c -> toCompletionDTO(c, petName(pets, c.getPetId())))
                .toList();
    }

    /**
     * 查询并校验归属（提醒不存在或不属于当前用户均按不存在处理，不泄露他人提醒存在性）
     */
    private Reminder findOwnedReminder(Long id) {
        Reminder reminder = reminderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.REMINDER_NOT_FOUND));
        if (!reminder.getUserId().equals(getCurrentUserId())) {
            throw new BusinessException(ErrorCode.REMINDER_NOT_FOUND);
        }
        return reminder;
    }

    /** 周期配置合法性校验（非法转 REMINDER_INTERVAL_INVALID） */
    private void validateInterval(IntervalType intervalType, Integer intervalValue) {
        try {
            scheduleService.validateInterval(intervalType, intervalValue);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.REMINDER_INTERVAL_INVALID, e.getMessage());
        }
    }

    /** 用户全部宠物映射（petName 装配用） */
    private Map<Long, Pet> petsById(Long userId) {
        return petApplicationService.getUserPetDomains(userId).stream()
                .collect(java.util.stream.Collectors.toMap(Pet::getId, Function.identity()));
    }

    private String petName(Map<Long, Pet> pets, Long petId) {
        Pet pet = pets.get(petId);
        return pet != null ? pet.getName() : null;
    }

    private String petNameOf(Long petId) {
        return petApplicationService.getUserPetDomains(getCurrentUserId()).stream()
                .filter(p -> p.getId().equals(petId))
                .map(Pet::getName)
                .findFirst()
                .orElse(null);
    }

    /**
     * 枚举 code 解析：非法值转 PARAM_INVALID
     */
    private <E> E parseEnum(Function<String, E> parser, String code, String fieldName) {
        if (code == null || code.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, fieldName + "不能为空");
        }
        try {
            return parser.apply(code);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, fieldName + "取值非法: " + code);
        }
    }

    private Long getCurrentUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    /**
     * 领域对象转DTO
     */
    private ReminderDTO toDTO(Reminder r, String petName) {
        return new ReminderDTO(
                r.getId(),
                r.getPetId(),
                petName,
                r.getType().getCode(),
                r.getTitle(),
                r.getDescription(),
                r.getIntervalType().getCode(),
                r.getIntervalValue(),
                r.getNextRemindTime(),
                r.getStatus().getCode(),
                r.getLastCompletionTime(),
                r.getLastCompletionNotes(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }

    /**
     * 今日已完成项的提醒视图（来自完成历史快照，提醒可能已删除）
     */
    private ReminderDTO completionReminderDTO(ReminderCompletion c, String petName) {
        return new ReminderDTO(
                c.getReminderId(),
                c.getPetId(),
                petName,
                c.getReminderType(),
                c.getReminderTitle(),
                null,
                null,
                null,
                null,
                null,
                c.getCompletionTime(),
                c.getNotes(),
                c.getCreatedAt(),
                null
        );
    }

    private CompletionDTO toCompletionDTO(ReminderCompletion c, String petName) {
        return new CompletionDTO(
                c.getId(),
                c.getReminderId(),
                c.getPetId(),
                petName,
                c.getReminderTitle(),
                c.getReminderType(),
                c.getCompletionTime(),
                c.getNotes(),
                c.getPhotoUrls(),
                c.getEventRecordId(),
                c.getCreatedAt()
        );
    }
}
