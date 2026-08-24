package com.fafa.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fafa.domain.model.medical.*;
import com.fafa.domain.repository.MedicationPlanRepository;
import com.fafa.infrastructure.persistence.mapper.MedicationPlanMapper;
import com.fafa.infrastructure.persistence.po.MedicationPlanPO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MedicationPlanRepositoryImpl implements MedicationPlanRepository {

    private final MedicationPlanMapper mapper;
    private final ObjectMapper objectMapper;

    @Override
    public MedicationPlan save(MedicationPlan plan) {
        MedicationPlanPO po = toPO(plan);
        if (po.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        return toDomain(mapper.selectById(po.getId()));
    }

    @Override
    public Optional<MedicationPlan> findById(Long id) {
        return Optional.ofNullable(toDomain(mapper.selectById(id)));
    }

    @Override
    public List<MedicationPlan> findByPetId(Long petId, String status) {
        LambdaQueryWrapper<MedicationPlanPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MedicationPlanPO::getPetId, petId);
        if (status != null && !status.isBlank()) {
            wrapper.eq(MedicationPlanPO::getStatus, status);
        }
        wrapper.orderByDesc(MedicationPlanPO::getStartDate)
                .orderByDesc(MedicationPlanPO::getId);
        return mapper.selectList(wrapper).stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private MedicationPlan toDomain(MedicationPlanPO po) {
        if (po == null) {
            return null;
        }
        return MedicationPlan.restore(po.getId(), po.getUserId(), po.getPetId(), po.getSourceVisitId(),
                po.getMedicationName(), po.getSpecification(), po.getPurpose(), po.getDosageValue(),
                po.getDosageUnit(), MedicationFrequencyType.fromCode(po.getFrequencyType()),
                po.getFrequencyValue(), readTimes(po.getAdministrationTimes()),
                MealRelation.fromCode(po.getMealRelation()), po.getStartDate(), po.getEndDate(),
                MedicationPlanStatus.fromCode(po.getStatus()), po.getNotes(),
                Boolean.TRUE.equals(po.getIsDeleted()), po.getCreatedAt(), po.getUpdatedAt());
    }

    private MedicationPlanPO toPO(MedicationPlan plan) {
        MedicationPlanPO po = new MedicationPlanPO();
        po.setId(plan.getId());
        po.setUserId(plan.getUserId());
        po.setPetId(plan.getPetId());
        po.setSourceVisitId(plan.getSourceVisitId());
        po.setMedicationName(plan.getMedicationName());
        po.setSpecification(plan.getSpecification());
        po.setPurpose(plan.getPurpose());
        po.setDosageValue(plan.getDosageValue());
        po.setDosageUnit(plan.getDosageUnit());
        po.setFrequencyType(plan.getFrequencyType().getCode());
        po.setFrequencyValue(plan.getFrequencyValue());
        po.setAdministrationTimes(writeTimes(plan.getAdministrationTimes()));
        po.setMealRelation(plan.getMealRelation().getCode());
        po.setStartDate(plan.getStartDate());
        po.setEndDate(plan.getEndDate());
        po.setStatus(plan.getStatus().getCode());
        po.setNotes(plan.getNotes());
        po.setIsDeleted(plan.isDeleted());
        return po;
    }

    private List<LocalTime> readTimes(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception ex) {
            throw new IllegalStateException("用药执行时间 JSON 无效", ex);
        }
    }

    private String writeTimes(List<LocalTime> times) {
        try {
            return objectMapper.writeValueAsString(times);
        } catch (Exception ex) {
            throw new IllegalStateException("用药执行时间 JSON 序列化失败", ex);
        }
    }
}
