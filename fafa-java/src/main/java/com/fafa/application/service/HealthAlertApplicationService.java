package com.fafa.application.service;

import cn.dev33.satoken.stp.StpUtil;
import com.fafa.application.dto.medical.HealthAlertDTO;
import com.fafa.common.BusinessException;
import com.fafa.common.ErrorCode;
import com.fafa.domain.model.health.*;
import com.fafa.domain.model.collaboration.PetPermission;
import com.fafa.domain.model.medical.MedicationRecord;
import com.fafa.domain.model.record.DailyStatus;
import com.fafa.domain.repository.DailyStatusRepository;
import com.fafa.domain.repository.HealthAlertRepository;
import com.fafa.domain.repository.MedicationRecordRepository;
import com.fafa.domain.service.HealthRuleEngine;
import com.fafa.infrastructure.config.FaFaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** 健康规则事实加载、预警生命周期和用户权限协调服务。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthAlertApplicationService {

    private final HealthAlertRepository alertRepository;
    private final DailyStatusRepository dailyStatusRepository;
    private final MedicationRecordRepository medicationRecordRepository;
    private final PetApplicationService petApplicationService;
    private final HealthRuleEngine healthRuleEngine;
    private final FaFaProperties properties;
    private final PetAccessApplicationService petAccessApplicationService;

    public List<HealthAlertDTO> list(Long petId, String status, int limit) {
        petAccessApplicationService.require(currentUserId(), petId, PetPermission.READ_MEDICAL);
        return alertRepository.findByPetId(petId, status, safeLimit(limit)).stream()
                .map(this::toDTO).toList();
    }

    public List<HealthAlertDTO> listForUser(Long userId, Long petId, String status, int limit) {
        petApplicationService.checkPetOwnership(userId, petId);
        return alertRepository.findByPetId(petId, status, safeLimit(limit)).stream()
                .map(this::toDTO).toList();
    }

    public HealthAlertDTO get(Long id) {
        HealthAlert alert = alertRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.HEALTH_ALERT_NOT_FOUND));
        petAccessApplicationService.require(currentUserId(), alert.getPetId(), PetPermission.READ_MEDICAL);
        return toDTO(alert);
    }

    @Transactional
    public HealthAlertDTO acknowledge(Long id) {
        HealthAlert alert = ownedAlert(id);
        try {
            alert.acknowledge(LocalDateTime.now());
        } catch (IllegalStateException ex) {
            throw new BusinessException(ErrorCode.HEALTH_ALERT_STATE_INVALID, ex.getMessage());
        }
        return toDTO(alertRepository.save(alert));
    }

    @Transactional
    public HealthAlertDTO resolve(Long id) {
        HealthAlert alert = ownedAlert(id);
        alert.resolve(LocalDateTime.now());
        return toDTO(alertRepository.save(alert));
    }

    @Transactional
    public List<HealthAlertDTO> evaluate(Long petId, LocalDate asOfDate) {
        Long userId = currentUserId();
        return evaluateForUser(userId, petId, asOfDate).stream().map(this::toDTO).toList();
    }

    @Transactional
    public List<HealthAlert> evaluateForUser(Long userId, Long petId, LocalDate asOfDate) {
        petApplicationService.checkPetOwnership(userId, petId);
        LocalDate end = asOfDate == null ? LocalDate.now() : asOfDate;
        if (end.isAfter(LocalDate.now())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "评估日期不能晚于今天");
        }
        int windowDays = properties.getHealthAlert().getDailyWindowDays();
        LocalDate start = end.minusDays(windowDays - 1L);
        List<DailyStatus> dailyStatuses = dailyStatusRepository.findByPetIdAndDateRange(petId, start, end);
        List<MedicationRecord> medicationRecords = medicationRecordRepository.findByPetIdAndTimeRange(
                petId, start.atStartOfDay(), end.plusDays(1).atStartOfDay());
        HealthRuleContext context = new HealthRuleContext(userId, petId, start, end,
                dailyStatuses, medicationRecords);
        List<HealthAlertDraft> drafts = healthRuleEngine.evaluate(context);
        List<HealthAlert> saved = drafts.stream().map(draft -> saveDraft(userId, petId, draft)).toList();
        resolveMissingAlerts(petId, drafts.stream().map(HealthAlertDraft::fingerprint).collect(Collectors.toSet()));
        log.info("健康预警评估完成: userId={}, petId={}, alertCount={}", userId, petId, saved.size());
        return saved;
    }

    private HealthAlert saveDraft(Long userId, Long petId, HealthAlertDraft draft) {
        HealthAlert alert = alertRepository.findByFingerprint(petId, draft.ruleCode(), draft.fingerprint())
                .orElseGet(() -> HealthAlert.create(userId, petId, draft, LocalDateTime.now()));
        if (alert.getId() != null) {
            alert.redetect(LocalDateTime.now());
        }
        return alertRepository.save(alert);
    }

    private void resolveMissingAlerts(Long petId, Set<String> fingerprints) {
        List<HealthAlert> activeAlerts = alertRepository.findActiveByPetId(petId);
        activeAlerts.stream()
                .filter(alert -> !fingerprints.contains(alert.getFingerprint()))
                .forEach(alert -> alert.resolve(LocalDateTime.now()));
        activeAlerts.stream()
                .filter(alert -> !fingerprints.contains(alert.getFingerprint()))
                .forEach(alertRepository::save);
    }

    private HealthAlert ownedAlert(Long id) {
        Long userId = currentUserId();
        HealthAlert alert = alertRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.HEALTH_ALERT_NOT_FOUND));
        if (!userId.equals(alert.getUserId())) {
            throw new BusinessException(ErrorCode.HEALTH_ALERT_NOT_FOUND);
        }
        return alert;
    }

    private HealthAlertDTO toDTO(HealthAlert alert) {
        List<HealthAlertDTO.EvidenceDTO> evidence = alert.getEvidence().stream()
                .map(item -> new HealthAlertDTO.EvidenceDTO(item.sourceType(), item.sourceId(),
                        item.field(), item.value(), item.observedDate())).toList();
        return new HealthAlertDTO(alert.getId(), alert.getPetId(), alert.getRuleCode(),
                alert.getSeverity().getCode(), alert.getTitle(), alert.getMessage(),
                alert.getStatus().getCode(), alert.getObservedFrom(), alert.getObservedTo(),
                evidence, alert.getLastDetectedAt());
    }

    private int safeLimit(int limit) {
        return Math.min(Math.max(limit, 1), 100);
    }

    private Long currentUserId() {
        return StpUtil.getLoginIdAsLong();
    }
}
