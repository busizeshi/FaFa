package com.fafa.application.service;

import cn.dev33.satoken.stp.StpUtil;
import com.fafa.application.dto.PageDTO;
import com.fafa.application.dto.medical.*;
import com.fafa.application.dto.media.MediaDTO;
import com.fafa.common.BusinessException;
import com.fafa.common.ErrorCode;
import com.fafa.domain.model.event.EventRecord;
import com.fafa.domain.model.collaboration.PetPermission;
import com.fafa.domain.model.medical.MedicalVisit;
import com.fafa.domain.model.medical.MedicalVisitAttachment;
import com.fafa.domain.model.reminder.ReminderCompletion;
import com.fafa.domain.repository.*;
import com.fafa.infrastructure.config.FaFaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** 就医档案、附件关系和医疗时间线应用服务。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MedicalVisitApplicationService {

    private final MedicalVisitRepository medicalVisitRepository;
    private final MedicalVisitAttachmentRepository attachmentRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final EventRecordRepository eventRecordRepository;
    private final ReminderCompletionRepository reminderCompletionRepository;
    private final MedicationRecordRepository medicationRecordRepository;
    private final MediaApplicationService mediaApplicationService;
    private final PetApplicationService petApplicationService;
    private final FaFaProperties properties;
    private final ExpenseApplicationService expenseApplicationService;
    private final PetAccessApplicationService petAccessApplicationService;

    @Transactional
    public MedicalVisitDTO create(MedicalVisitCommand command) {
        Long userId = currentUserId();
        var pet = petApplicationService.checkPetOwnership(command.petId());
        MedicalVisit visit = createDomain(userId, pet.getId(), command);
        MedicalVisit saved = medicalVisitRepository.save(visit);
        expenseApplicationService.syncMedicalVisitExpense(saved);
        log.info("就医档案创建成功: userId={}, petId={}, visitId={}", userId, pet.getId(), saved.getId());
        return toDTO(saved, userId);
    }

    public PageDTO<MedicalVisitDTO> list(Long petId, LocalDate startDate, LocalDate endDate,
                                         int page, int size) {
        Long userId = currentUserId();
        petAccessApplicationService.require(userId, petId, PetPermission.READ_MEDICAL);
        DateRange range = validateRange(startDate, endDate, properties.getMedical().getVisitMaxRangeDays());
        int safePage = Math.max(1, page);
        int safeSize = Math.min(Math.max(1, size), 50);
        int offset = (safePage - 1) * safeSize;
        List<MedicalVisitDTO> records = medicalVisitRepository
                .findByPetIdAndDateRange(petId, range.start(), range.end(), offset, safeSize)
                .stream().map(visit -> toDTO(visit, userId)).toList();
        long total = medicalVisitRepository.countByPetIdAndDateRange(petId, range.start(), range.end());
        return PageDTO.of(records, total, safePage, safeSize);
    }

    public MedicalVisitDTO getById(Long id) {
        Long userId = currentUserId();
        return toDTO(accessibleVisit(id, userId), userId);
    }

    public MedicalVisitDTO getByIdForUser(Long userId, Long id) {
        return toDTO(ownedVisit(id, userId), userId);
    }

    public List<MedicalVisitDTO> listForUser(Long userId, Long petId,
                                             LocalDate startDate, LocalDate endDate) {
        petApplicationService.checkPetOwnership(userId, petId);
        DateRange range = validateRange(startDate, endDate, properties.getMedical().getSummaryMaxRangeDays());
        return medicalVisitRepository.findByPetIdAndDateRange(petId, range.start(), range.end(), 0, 100)
                .stream().map(visit -> toDTO(visit, userId)).toList();
    }

    @Transactional
    public MedicalVisitDTO update(Long id, MedicalVisitCommand command) {
        Long userId = currentUserId();
        MedicalVisit visit = ownedVisit(id, userId);
        try {
            visit.update(command.hospitalName(), command.doctorName(), command.chiefComplaint(),
                    command.diagnosis(), command.prescription(), command.treatment(),
                    command.totalCost(), command.notes());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.MEDICAL_RECORD_INVALID, ex.getMessage());
        }
        MedicalVisit saved = medicalVisitRepository.save(visit);
        expenseApplicationService.syncMedicalVisitExpense(saved);
        return toDTO(saved, userId);
    }

    @Transactional
    public void delete(Long id) {
        MedicalVisit visit = ownedVisit(id, currentUserId());
        medicalVisitRepository.deleteById(visit.getId());
        expenseApplicationService.removeMedicalVisitExpense(visit.getUserId(), visit.getId());
        log.info("就医档案删除成功: userId={}, visitId={}", visit.getUserId(), visit.getId());
    }

    @Transactional
    public MedicalVisitDTO attach(Long visitId, AttachMedicalMediaCommand command) {
        Long userId = currentUserId();
        MedicalVisit visit = ownedVisit(visitId, userId);
        List<Long> mediaIds = command.mediaIds() == null ? List.of() : command.mediaIds().stream().distinct().toList();
        if (mediaIds.isEmpty() || mediaIds.size() > 9) {
            throw new BusinessException(ErrorCode.MEDICAL_ATTACHMENT_INVALID);
        }
        if (mediaAssetRepository.findByIds(userId, mediaIds).size() != mediaIds.size()) {
            throw new BusinessException(ErrorCode.MEDICAL_ATTACHMENT_INVALID, "存在不属于当前用户的素材");
        }
        for (int index = 0; index < mediaIds.size(); index++) {
            Long mediaId = mediaIds.get(index);
            if (!attachmentRepository.existsByVisitIdAndMediaId(visitId, mediaId)) {
                attachmentRepository.save(MedicalVisitAttachment.create(userId, visitId, mediaId,
                        command.attachmentType(), index));
            }
        }
        return toDTO(visit, userId);
    }

    @Transactional
    public void detach(Long visitId, Long mediaId) {
        ownedVisit(visitId, currentUserId());
        attachmentRepository.deleteByVisitIdAndMediaId(visitId, mediaId);
    }

    public List<MedicalTimelineItemDTO> timeline(Long petId, LocalDate startDate, LocalDate endDate) {
        Long userId = currentUserId();
        petAccessApplicationService.require(userId, petId, PetPermission.READ_MEDICAL);
        return timelineForUser(userId, petId, startDate, endDate);
    }

    public List<MedicalTimelineItemDTO> timelineForUser(Long userId, Long petId,
                                                         LocalDate startDate, LocalDate endDate) {
        petApplicationService.checkPetOwnership(userId, petId);
        DateRange range = validateRange(startDate, endDate, properties.getMedical().getVisitMaxRangeDays());
        List<MedicalTimelineItemDTO> items = new ArrayList<>();
        medicalVisitRepository.findByPetIdAndDateRange(petId, range.start(), range.end(), 0, 200)
                .forEach(visit -> items.add(new MedicalTimelineItemDTO(
                        "medical_visit", visit.getId(), visit.getVisitDate(), visit.getHospitalName(),
                        visit.getChiefComplaint(), "recorded")));
        eventRecordRepository.findByPetIdAndDateRange(petId, range.start(), range.end()).stream()
                .filter(EventRecord::isManual)
                .forEach(event -> items.add(new MedicalTimelineItemDTO(
                        event.getEventType().getCode(), event.getId(), event.getEventDate(),
                        event.getTitle(), event.getContent(), "recorded")));
        reminderCompletionRepository.findByPetIdAndTimeBetween(userId, petId,
                        range.start().atStartOfDay(), range.end().plusDays(1).atStartOfDay()).stream()
                .filter(completion -> List.of("vaccine", "deworming", "checkup")
                        .contains(completion.getReminderType()))
                .forEach(completion -> items.add(new MedicalTimelineItemDTO(
                        completion.getReminderType(), completion.getReminderId(),
                        completion.getCompletionTime().toLocalDate(), completion.getReminderTitle(),
                        completion.getNotes(), "completed")));
        medicationRecordRepository.findByPetIdAndTimeRange(petId, range.start().atStartOfDay(),
                        range.end().plusDays(1).atStartOfDay()).forEach(record ->
                items.add(new MedicalTimelineItemDTO("medication", record.getId(),
                        record.getScheduledAt().toLocalDate(), "用药：" + record.getDosageValue()
                                + record.getDosageUnit(), record.getNotes(), record.getStatus().getCode())));
        return items.stream().sorted(Comparator.comparing(MedicalTimelineItemDTO::eventDate).reversed()).toList();
    }

    private MedicalVisit createDomain(Long userId, Long petId, MedicalVisitCommand command) {
        try {
            return MedicalVisit.create(userId, petId, command.visitDate(), command.hospitalName(),
                    command.doctorName(), command.chiefComplaint(), command.diagnosis(),
                    command.prescription(), command.treatment(), command.totalCost(), command.notes(),
                    LocalDate.now());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.MEDICAL_RECORD_INVALID, ex.getMessage());
        }
    }

    private MedicalVisit ownedVisit(Long id, Long userId) {
        MedicalVisit visit = medicalVisitRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEDICAL_RECORD_NOT_FOUND));
        if (!userId.equals(visit.getUserId())) {
            throw new BusinessException(ErrorCode.MEDICAL_RECORD_NOT_FOUND);
        }
        return visit;
    }

    private MedicalVisit accessibleVisit(Long id, Long userId) {
        MedicalVisit visit = medicalVisitRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEDICAL_RECORD_NOT_FOUND));
        petAccessApplicationService.require(userId, visit.getPetId(), PetPermission.READ_MEDICAL);
        return visit;
    }

    private MedicalVisitDTO toDTO(MedicalVisit visit, Long userId) {
        List<MediaDTO> attachments = attachmentRepository.findByVisitId(visit.getId()).stream()
                .map(item -> mediaApplicationService.getByIdForAccess(userId, item.getMediaId()))
                .toList();
        return new MedicalVisitDTO(visit.getId(), visit.getPetId(), visit.getVisitDate(),
                visit.getHospitalName(), visit.getDoctorName(), visit.getChiefComplaint(),
                visit.getDiagnosis(), visit.getPrescription(), visit.getTreatment(),
                visit.getTotalCost(), visit.getNotes(), attachments, visit.getCreatedAt(),
                visit.getUpdatedAt());
    }

    private DateRange validateRange(LocalDate start, LocalDate end, int maxDays) {
        if (start == null || end == null || start.isAfter(end)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "日期范围无效");
        }
        if (end.isAfter(LocalDate.now())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "结束日期不能晚于今天");
        }
        if (ChronoUnit.DAYS.between(start, end) > maxDays) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "查询范围不能超过 " + maxDays + " 天");
        }
        return new DateRange(start, end);
    }

    private Long currentUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    private record DateRange(LocalDate start, LocalDate end) {
    }
}
