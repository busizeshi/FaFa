package com.fafa.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fafa.domain.model.health.*;
import com.fafa.domain.repository.HealthAlertRepository;
import com.fafa.infrastructure.persistence.mapper.HealthAlertMapper;
import com.fafa.infrastructure.persistence.po.HealthAlertPO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class HealthAlertRepositoryImpl implements HealthAlertRepository {

    private final HealthAlertMapper mapper;
    private final ObjectMapper objectMapper;

    @Override
    public HealthAlert save(HealthAlert alert) {
        HealthAlertPO po = toPO(alert);
        if (po.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        return toDomain(mapper.selectById(po.getId()));
    }

    @Override
    public Optional<HealthAlert> findById(Long id) {
        return Optional.ofNullable(toDomain(mapper.selectById(id)));
    }

    @Override
    public Optional<HealthAlert> findByFingerprint(Long petId, String ruleCode, String fingerprint) {
        LambdaQueryWrapper<HealthAlertPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HealthAlertPO::getPetId, petId)
                .eq(HealthAlertPO::getRuleCode, ruleCode)
                .eq(HealthAlertPO::getFingerprint, fingerprint);
        return Optional.ofNullable(toDomain(mapper.selectOne(wrapper)));
    }

    @Override
    public List<HealthAlert> findByPetId(Long petId, String status, int limit) {
        LambdaQueryWrapper<HealthAlertPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HealthAlertPO::getPetId, petId);
        if (status != null && !status.isBlank()) {
            wrapper.eq(HealthAlertPO::getStatus, status);
        }
        wrapper.last("ORDER BY CASE severity WHEN 'red' THEN 0 ELSE 1 END, last_detected_at DESC "
                + "LIMIT " + Math.max(1, limit));
        return mapper.selectList(wrapper).stream().map(this::toDomain).toList();
    }

    @Override
    public List<HealthAlert> findActiveByPetId(Long petId) {
        LambdaQueryWrapper<HealthAlertPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HealthAlertPO::getPetId, petId)
                .in(HealthAlertPO::getStatus, HealthAlertStatus.OPEN.getCode(),
                        HealthAlertStatus.ACKNOWLEDGED.getCode());
        return mapper.selectList(wrapper).stream().map(this::toDomain).toList();
    }

    private HealthAlert toDomain(HealthAlertPO po) {
        if (po == null) {
            return null;
        }
        return HealthAlert.restore(po.getId(), po.getUserId(), po.getPetId(), po.getRuleCode(),
                HealthAlertSeverity.fromCode(po.getSeverity()), po.getTitle(), po.getMessage(),
                readEvidence(po.getEvidenceJson()), po.getObservedFrom(), po.getObservedTo(),
                po.getFingerprint(), HealthAlertStatus.fromCode(po.getStatus()),
                po.getFirstDetectedAt(), po.getLastDetectedAt(), po.getAcknowledgedAt(),
                po.getResolvedAt(), po.getCreatedAt(), po.getUpdatedAt());
    }

    private HealthAlertPO toPO(HealthAlert alert) {
        HealthAlertPO po = new HealthAlertPO();
        po.setId(alert.getId());
        po.setUserId(alert.getUserId());
        po.setPetId(alert.getPetId());
        po.setRuleCode(alert.getRuleCode());
        po.setSeverity(alert.getSeverity().getCode());
        po.setTitle(alert.getTitle());
        po.setMessage(alert.getMessage());
        po.setEvidenceJson(writeEvidence(alert.getEvidence()));
        po.setObservedFrom(alert.getObservedFrom());
        po.setObservedTo(alert.getObservedTo());
        po.setFingerprint(alert.getFingerprint());
        po.setStatus(alert.getStatus().getCode());
        po.setFirstDetectedAt(alert.getFirstDetectedAt());
        po.setLastDetectedAt(alert.getLastDetectedAt());
        po.setAcknowledgedAt(alert.getAcknowledgedAt());
        po.setResolvedAt(alert.getResolvedAt());
        return po;
    }

    private List<HealthEvidence> readEvidence(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception ex) {
            throw new IllegalStateException("健康预警证据 JSON 无效", ex);
        }
    }

    private String writeEvidence(List<HealthEvidence> evidence) {
        try {
            return objectMapper.writeValueAsString(evidence);
        } catch (Exception ex) {
            throw new IllegalStateException("健康预警证据 JSON 序列化失败", ex);
        }
    }
}
