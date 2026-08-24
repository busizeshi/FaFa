package com.fafa.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fafa.domain.model.medical.MedicationRecord;
import com.fafa.domain.model.medical.MedicationRecordStatus;
import com.fafa.domain.repository.MedicationRecordRepository;
import com.fafa.infrastructure.persistence.mapper.MedicationRecordMapper;
import com.fafa.infrastructure.persistence.po.MedicationRecordPO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MedicationRecordRepositoryImpl implements MedicationRecordRepository {

    private final MedicationRecordMapper mapper;

    @Override
    public MedicationRecord save(MedicationRecord record) {
        MedicationRecordPO po = toPO(record);
        try {
            if (po.getId() == null) {
                mapper.insert(po);
            } else {
                mapper.updateById(po);
            }
        } catch (DuplicateKeyException ex) {
            throw new IllegalStateException("同一用药计划执行时段已存在记录", ex);
        }
        return toDomain(mapper.selectById(po.getId()));
    }

    @Override
    public Optional<MedicationRecord> findByPlanIdAndScheduledAt(Long planId,
                                                                   LocalDateTime scheduledAt) {
        LambdaQueryWrapper<MedicationRecordPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MedicationRecordPO::getMedicationPlanId, planId)
                .eq(MedicationRecordPO::getScheduledAt, scheduledAt);
        return Optional.ofNullable(toDomain(mapper.selectOne(wrapper)));
    }

    @Override
    public Optional<MedicationRecord> findById(Long id) {
        return Optional.ofNullable(toDomain(mapper.selectById(id)));
    }

    @Override
    public List<MedicationRecord> findByPetIdAndTimeRange(Long petId, LocalDateTime start,
                                                          LocalDateTime end) {
        LambdaQueryWrapper<MedicationRecordPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MedicationRecordPO::getPetId, petId)
                .ge(MedicationRecordPO::getScheduledAt, start)
                .lt(MedicationRecordPO::getScheduledAt, end)
                .orderByAsc(MedicationRecordPO::getScheduledAt);
        return mapper.selectList(wrapper).stream().map(this::toDomain).toList();
    }

    private MedicationRecord toDomain(MedicationRecordPO po) {
        if (po == null) {
            return null;
        }
        return MedicationRecord.restore(po.getId(), po.getUserId(), po.getPetId(),
                po.getMedicationPlanId(), po.getReminderId(), po.getScheduledAt(), po.getTakenAt(),
                po.getDosageValue(), po.getDosageUnit(),
                MedicationRecordStatus.fromCode(po.getStatus()), po.getNotes(), po.getOperatorId(),
                po.getCreatedAt(), po.getUpdatedAt());
    }

    private MedicationRecordPO toPO(MedicationRecord record) {
        MedicationRecordPO po = new MedicationRecordPO();
        po.setId(record.getId());
        po.setUserId(record.getUserId());
        po.setPetId(record.getPetId());
        po.setMedicationPlanId(record.getMedicationPlanId());
        po.setReminderId(record.getReminderId());
        po.setScheduledAt(record.getScheduledAt());
        po.setTakenAt(record.getTakenAt());
        po.setDosageValue(record.getDosageValue());
        po.setDosageUnit(record.getDosageUnit());
        po.setStatus(record.getStatus().getCode());
        po.setNotes(record.getNotes());
        po.setOperatorId(record.getOperatorId());
        return po;
    }
}
