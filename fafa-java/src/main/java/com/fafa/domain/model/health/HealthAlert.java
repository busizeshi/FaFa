package com.fafa.domain.model.health;

import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/** 持久化的规则预警聚合，负责状态转移。 */
@Getter
public final class HealthAlert {

    private Long id;
    private final Long userId;
    private final Long petId;
    private final String ruleCode;
    private final HealthAlertSeverity severity;
    private final String title;
    private final String message;
    private final List<HealthEvidence> evidence;
    private final LocalDate observedFrom;
    private final LocalDate observedTo;
    private final String fingerprint;
    private HealthAlertStatus status;
    private final LocalDateTime firstDetectedAt;
    private LocalDateTime lastDetectedAt;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private HealthAlert(Long id, Long userId, Long petId, String ruleCode,
                        HealthAlertSeverity severity, String title, String message,
                        List<HealthEvidence> evidence, LocalDate observedFrom,
                        LocalDate observedTo, String fingerprint, HealthAlertStatus status,
                        LocalDateTime firstDetectedAt, LocalDateTime lastDetectedAt,
                        LocalDateTime acknowledgedAt, LocalDateTime resolvedAt,
                        LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.petId = petId;
        this.ruleCode = ruleCode;
        this.severity = severity;
        this.title = title;
        this.message = message;
        this.evidence = evidence == null ? List.of() : List.copyOf(evidence);
        this.observedFrom = observedFrom;
        this.observedTo = observedTo;
        this.fingerprint = fingerprint;
        this.status = status;
        this.firstDetectedAt = firstDetectedAt;
        this.lastDetectedAt = lastDetectedAt;
        this.acknowledgedAt = acknowledgedAt;
        this.resolvedAt = resolvedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static HealthAlert create(Long userId, Long petId, HealthAlertDraft draft,
                                     LocalDateTime now) {
        Objects.requireNonNull(userId, "userId 不能为空");
        Objects.requireNonNull(petId, "petId 不能为空");
        Objects.requireNonNull(draft, "预警草稿不能为空");
        return new HealthAlert(null, userId, petId, draft.ruleCode(), draft.severity(),
                draft.title(), draft.message(), draft.evidence(), draft.observedFrom(),
                draft.observedTo(), draft.fingerprint(), HealthAlertStatus.OPEN, now, now,
                null, null, null, null);
    }

    public static HealthAlert restore(Long id, Long userId, Long petId, String ruleCode,
                                      HealthAlertSeverity severity, String title, String message,
                                      List<HealthEvidence> evidence, LocalDate observedFrom,
                                      LocalDate observedTo, String fingerprint,
                                      HealthAlertStatus status, LocalDateTime firstDetectedAt,
                                      LocalDateTime lastDetectedAt, LocalDateTime acknowledgedAt,
                                      LocalDateTime resolvedAt, LocalDateTime createdAt,
                                      LocalDateTime updatedAt) {
        return new HealthAlert(id, userId, petId, ruleCode, severity, title, message, evidence,
                observedFrom, observedTo, fingerprint, status, firstDetectedAt,
                lastDetectedAt, acknowledgedAt, resolvedAt, createdAt, updatedAt);
    }

    public void redetect(LocalDateTime now) {
        lastDetectedAt = now;
        if (status == HealthAlertStatus.RESOLVED || status == HealthAlertStatus.EXPIRED) {
            status = HealthAlertStatus.OPEN;
            resolvedAt = null;
            acknowledgedAt = null;
        }
    }

    public void acknowledge(LocalDateTime now) {
        if (status != HealthAlertStatus.OPEN) {
            throw new IllegalStateException("当前预警不可确认");
        }
        status = HealthAlertStatus.ACKNOWLEDGED;
        acknowledgedAt = now;
    }

    public void resolve(LocalDateTime now) {
        if (status == HealthAlertStatus.RESOLVED || status == HealthAlertStatus.EXPIRED) {
            return;
        }
        status = HealthAlertStatus.RESOLVED;
        resolvedAt = now;
    }
}
