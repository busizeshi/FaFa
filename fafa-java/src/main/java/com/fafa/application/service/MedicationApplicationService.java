package com.fafa.application.service;

import cn.dev33.satoken.stp.StpUtil;
import com.fafa.application.dto.medical.*;
import com.fafa.common.BusinessException;
import com.fafa.common.ErrorCode;
import com.fafa.domain.model.medical.*;
import com.fafa.domain.model.collaboration.PetPermission;
import com.fafa.domain.model.reminder.IntervalType;
import com.fafa.domain.model.reminder.Reminder;
import com.fafa.domain.model.reminder.ReminderCompletion;
import com.fafa.domain.repository.MedicalVisitRepository;
import com.fafa.domain.repository.MedicationPlanRepository;
import com.fafa.domain.repository.MedicationRecordRepository;
import com.fafa.domain.repository.ReminderRepository;
import com.fafa.infrastructure.config.FaFaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** 用药计划、提醒同步和实际用药记录应用服务。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MedicationApplicationService {

    private static final String REMINDER_TITLE_PREFIX = "用药：";

    private final MedicationPlanRepository planRepository;
    private final MedicationRecordRepository recordRepository;
    private final ReminderRepository reminderRepository;
    private final MedicalVisitRepository visitRepository;
    private final PetApplicationService petApplicationService;
    private final FaFaProperties properties;
    private final PetAccessApplicationService petAccessApplicationService;

    @Transactional
    public MedicationPlanDTO create(MedicationPlanCommand command) {
        Long userId = currentUserId();
        var pet = petApplicationService.checkPetOwnership(command.petId());
        validateSourceVisit(userId, command.petId(), command.sourceVisitId());
        MedicationPlan plan = createDomain(userId, pet.getId(), command);
        MedicationPlan saved = planRepository.save(plan);
        createReminders(saved, LocalDateTime.now());
        log.info("用药计划创建成功: userId={}, petId={}, planId={}", userId, pet.getId(), saved.getId());
        return toDTO(saved);
    }

    public List<MedicationPlanDTO> list(Long petId, String status) {
        petAccessApplicationService.require(currentUserId(), petId, PetPermission.READ_MEDICAL);
        return planRepository.findByPetId(petId, status).stream().map(this::toDTO).toList();
    }

    public List<MedicationPlanDTO> listForUser(Long userId, Long petId, String status) {
        petApplicationService.checkPetOwnership(userId, petId);
        return planRepository.findByPetId(petId, status).stream().map(this::toDTO).toList();
    }

    public MedicationPlanDTO getById(Long id) {
        return toDTO(ownedPlan(id));
    }

    @Transactional
    public MedicationPlanDTO update(Long id, MedicationPlanCommand command) {
        MedicationPlan plan = ownedPlan(id);
        if (!plan.getPetId().equals(command.petId())) {
            throw new BusinessException(ErrorCode.MEDICATION_PLAN_INVALID, "宠物不能修改");
        }
        try {
            plan.update(command.medicationName(), command.specification(), command.purpose(),
                    command.dosageValue(), command.dosageUnit(), parseFrequency(command.frequencyType()),
                    command.frequencyValue(), command.administrationTimes(),
                    parseMealRelation(command.mealRelation()), command.startDate(), command.endDate(),
                    command.notes(), LocalDate.now());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            throw new BusinessException(ErrorCode.MEDICATION_PLAN_INVALID, ex.getMessage());
        }
        reminderRepository.deleteByMedicationPlanId(plan.getId());
        MedicationPlan saved = planRepository.save(plan);
        if (saved.getStatus() == MedicationPlanStatus.ACTIVE) {
            createReminders(saved, LocalDateTime.now());
        }
        return toDTO(saved);
    }

    @Transactional
    public MedicationPlanDTO pause(Long id) {
        MedicationPlan plan = ownedPlan(id);
        try {
            plan.pause();
        } catch (IllegalStateException ex) {
            throw new BusinessException(ErrorCode.MEDICATION_PLAN_STATE_INVALID, ex.getMessage());
        }
        MedicationPlan saved = planRepository.save(plan);
        reminderRepository.findByMedicationPlanId(id).forEach(reminder -> {
            try {
                reminder.pause();
                reminderRepository.save(reminder);
            } catch (IllegalStateException ignored) {
                log.debug("跳过已结束用药提醒: reminderId={}", reminder.getId());
            }
        });
        return toDTO(saved);
    }

    @Transactional
    public MedicationPlanDTO resume(Long id) {
        MedicationPlan plan = ownedPlan(id);
        try {
            plan.resume();
        } catch (IllegalStateException ex) {
            throw new BusinessException(ErrorCode.MEDICATION_PLAN_STATE_INVALID, ex.getMessage());
        }
        MedicationPlan saved = planRepository.save(plan);
        reminderRepository.findByMedicationPlanId(id).forEach(reminder -> {
            try {
                reminder.resume();
                reminderRepository.save(reminder);
            } catch (IllegalStateException ignored) {
                log.debug("跳过不可恢复用药提醒: reminderId={}", reminder.getId());
            }
        });
        return toDTO(saved);
    }

    @Transactional
    public MedicationPlanDTO complete(Long id) {
        MedicationPlan plan = ownedPlan(id);
        try {
            plan.complete();
        } catch (IllegalStateException ex) {
            throw new BusinessException(ErrorCode.MEDICATION_PLAN_STATE_INVALID, ex.getMessage());
        }
        MedicationPlan saved = planRepository.save(plan);
        reminderRepository.deleteByMedicationPlanId(id);
        return toDTO(saved);
    }

    @Transactional
    public void delete(Long id) {
        MedicationPlan plan = ownedPlan(id);
        plan.delete();
        planRepository.save(plan);
        reminderRepository.deleteByMedicationPlanId(id);
    }

    @Transactional
    public MedicationRecordDTO saveRecord(MedicationRecordCommand command) {
        Long userId = currentUserId();
        petApplicationService.checkPetOwnership(command.petId());
        MedicationPlan plan = ownedPlan(command.medicationPlanId());
        if (!plan.getPetId().equals(command.petId())) {
            throw new BusinessException(ErrorCode.MEDICATION_PLAN_NOT_FOUND);
        }
        MedicationRecord existing = recordRepository
                .findByPlanIdAndScheduledAt(plan.getId(), command.scheduledAt()).orElse(null);
        if (existing != null) {
            return toDTO(existing);
        }
        try {
            MedicationRecord record = MedicationRecord.create(userId, plan.getPetId(), plan.getId(),
                    null, command.scheduledAt(), command.takenAt(), command.dosageValue(),
                    command.dosageUnit(), parseRecordStatus(command.status()), command.notes(),
                    userId, LocalDateTime.now(), properties.getMedical().getMedicationFutureToleranceMinutes());
            return toDTO(recordRepository.save(record));
        } catch (IllegalArgumentException | IllegalStateException | DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.MEDICATION_RECORD_INVALID, ex.getMessage());
        }
    }

    public List<MedicationRecordDTO> records(Long petId, LocalDate startDate, LocalDate endDate) {
        petAccessApplicationService.require(currentUserId(), petId, PetPermission.READ_MEDICAL);
        return recordsForUser(currentUserId(), petId, startDate, endDate);
    }

    public List<MedicationRecordDTO> recordsForUser(Long userId, Long petId,
                                                     LocalDate startDate, LocalDate endDate) {
        petApplicationService.checkPetOwnership(userId, petId);
        if (startDate == null || endDate == null || startDate.isAfter(endDate)
                || endDate.isAfter(LocalDate.now())
                || ChronoUnit.DAYS.between(startDate, endDate) > properties.getMedical().getVisitMaxRangeDays()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "用药查询日期范围无效");
        }
        return recordRepository.findByPetIdAndTimeRange(petId, startDate.atStartOfDay(),
                        endDate.plusDays(1).atStartOfDay()).stream().map(this::toDTO).toList();
    }

    /** 提醒完成事务内调用：把 medication 提醒转换为 taken 记录。 */
    @Transactional
    public MedicationRecord recordFromReminder(Reminder reminder, LocalDateTime scheduledAt,
                                               ReminderCompletion completion) {
        MedicationPlan plan = planRepository.findById(reminder.getMedicationId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEDICATION_PLAN_NOT_FOUND));
        MedicationRecord existing = recordRepository.findByPlanIdAndScheduledAt(plan.getId(), scheduledAt)
                .orElse(null);
        if (existing != null) {
            return existing;
        }
        MedicationRecord record = MedicationRecord.create(plan.getUserId(), plan.getPetId(), plan.getId(),
                reminder.getId(), scheduledAt, completion.getCompletionTime(), plan.getDosageValue(),
                plan.getDosageUnit(), MedicationRecordStatus.TAKEN, completion.getNotes(),
                completion.getUserId(), LocalDateTime.now(),
                properties.getMedical().getMedicationFutureToleranceMinutes());
        return recordRepository.save(record);
    }

    private MedicationPlan createDomain(Long userId, Long petId, MedicationPlanCommand command) {
        try {
            return MedicationPlan.create(userId, petId, command.sourceVisitId(), command.medicationName(),
                    command.specification(), command.purpose(), command.dosageValue(), command.dosageUnit(),
                    parseFrequency(command.frequencyType()), command.frequencyValue(),
                    command.administrationTimes(), parseMealRelation(command.mealRelation()),
                    command.startDate(), command.endDate(), command.notes(), LocalDate.now());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.MEDICATION_PLAN_INVALID, ex.getMessage());
        }
    }

    private void createReminders(MedicationPlan plan, LocalDateTime now) {
        if (plan.getFrequencyType() == MedicationFrequencyType.AS_NEEDED) {
            return;
        }
        IntervalType intervalType = toReminderInterval(plan.getFrequencyType());
        for (LocalTime time : plan.getAdministrationTimes()) {
            LocalDateTime next = nextOccurrence(plan, time, now);
            if (plan.getEndDate() != null && next.toLocalDate().isAfter(plan.getEndDate())) {
                continue;
            }
            Reminder reminder = Reminder.createMedication(plan.getPetId(), plan.getUserId(), plan.getId(),
                    REMINDER_TITLE_PREFIX + plan.getMedicationName(), buildReminderDescription(plan),
                    intervalType, reminderIntervalValue(plan), next, now);
            reminderRepository.save(reminder);
        }
    }

    private LocalDateTime nextOccurrence(MedicationPlan plan, LocalTime time, LocalDateTime now) {
        LocalDateTime candidate = plan.getStartDate().atTime(time);
        if (plan.getFrequencyType() == MedicationFrequencyType.WEEKLY) {
            int targetDay = plan.getFrequencyValue();
            while (candidate.getDayOfWeek().getValue() != targetDay || !candidate.isAfter(now)) {
                candidate = candidate.plusDays(1);
            }
            return candidate;
        }
        long days = plan.getFrequencyType() == MedicationFrequencyType.DAILY
                ? 1L : plan.getFrequencyValue();
        while (!candidate.isAfter(now)) {
            candidate = candidate.plusDays(days);
        }
        return candidate;
    }

    private String buildReminderDescription(MedicationPlan plan) {
        return "每次 " + plan.getDosageValue() + plan.getDosageUnit()
                + "，" + plan.getMealRelation().getCode();
    }

    private IntervalType toReminderInterval(MedicationFrequencyType type) {
        return switch (type) {
            case DAILY, EVERY_DAYS -> IntervalType.DAYS;
            case WEEKLY -> IntervalType.WEEKLY;
            case AS_NEEDED -> IntervalType.ONCE;
        };
    }

    private Integer reminderIntervalValue(MedicationPlan plan) {
        return plan.getFrequencyType() == MedicationFrequencyType.DAILY
                ? 1 : plan.getFrequencyValue();
    }

    private MedicationPlan ownedPlan(Long id) {
        Long userId = currentUserId();
        MedicationPlan plan = planRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEDICATION_PLAN_NOT_FOUND));
        if (!userId.equals(plan.getUserId())) {
            throw new BusinessException(ErrorCode.MEDICATION_PLAN_NOT_FOUND);
        }
        return plan;
    }

    private void validateSourceVisit(Long userId, Long petId, Long visitId) {
        if (visitId == null) {
            return;
        }
        var visit = visitRepository.findById(visitId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEDICAL_RECORD_NOT_FOUND));
        if (!userId.equals(visit.getUserId()) || !petId.equals(visit.getPetId())) {
            throw new BusinessException(ErrorCode.MEDICAL_RECORD_NOT_FOUND);
        }
    }

    private MedicationFrequencyType parseFrequency(String code) {
        try {
            return MedicationFrequencyType.fromCode(code);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.MEDICATION_PLAN_INVALID, ex.getMessage());
        }
    }

    private MealRelation parseMealRelation(String code) {
        try {
            return MealRelation.fromCode(code);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.MEDICATION_PLAN_INVALID, ex.getMessage());
        }
    }

    private MedicationRecordStatus parseRecordStatus(String code) {
        try {
            return MedicationRecordStatus.fromCode(code);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.MEDICATION_RECORD_INVALID, ex.getMessage());
        }
    }

    private MedicationPlanDTO toDTO(MedicationPlan plan) {
        List<Long> reminderIds = reminderRepository.findByMedicationPlanId(plan.getId()).stream()
                .map(Reminder::getId).toList();
        return new MedicationPlanDTO(plan.getId(), plan.getPetId(), plan.getSourceVisitId(),
                plan.getMedicationName(), plan.getSpecification(), plan.getPurpose(), plan.getDosageValue(),
                plan.getDosageUnit(), plan.getFrequencyType().getCode(), plan.getFrequencyValue(),
                plan.getAdministrationTimes(), plan.getMealRelation().getCode(), plan.getStartDate(),
                plan.getEndDate(), plan.getStatus().getCode(), reminderIds, plan.getCreatedAt(),
                plan.getUpdatedAt());
    }

    private MedicationRecordDTO toDTO(MedicationRecord record) {
        return new MedicationRecordDTO(record.getId(), record.getPetId(), record.getMedicationPlanId(),
                record.getReminderId(), record.getScheduledAt(), record.getTakenAt(), record.getDosageValue(),
                record.getDosageUnit(), record.getStatus().getCode(), record.getNotes(), record.getCreatedAt());
    }

    private Long currentUserId() {
        return StpUtil.getLoginIdAsLong();
    }
}
