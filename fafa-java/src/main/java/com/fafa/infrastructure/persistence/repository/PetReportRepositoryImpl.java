package com.fafa.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fafa.domain.model.report.PetReport;
import com.fafa.domain.model.report.ReportPeriod;
import com.fafa.domain.model.report.ReportPeriodType;
import com.fafa.domain.repository.PetReportRepository;
import com.fafa.infrastructure.persistence.mapper.PetReportMapper;
import com.fafa.infrastructure.persistence.po.PetReportPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PetReportRepositoryImpl implements PetReportRepository {
    private final PetReportMapper mapper;

    @Override public PetReport save(PetReport report) {
        PetReportPO po = toPO(report);
        if (po.getId() == null) mapper.insert(po); else mapper.updateById(po);
        return toDomain(mapper.selectById(po.getId()));
    }

    @Override public Optional<PetReport> findByIdAndUserId(Long id, Long userId) {
        return Optional.ofNullable(toDomain(mapper.selectOne(new LambdaQueryWrapper<PetReportPO>()
                .eq(PetReportPO::getId, id).eq(PetReportPO::getUserId, userId))));
    }

    @Override public Optional<PetReport> findByPeriod(Long userId, Long petId, ReportPeriod period) {
        return Optional.ofNullable(toDomain(mapper.selectOne(new LambdaQueryWrapper<PetReportPO>()
                .eq(PetReportPO::getUserId, userId).eq(PetReportPO::getPetId, petId)
                .eq(PetReportPO::getPeriodType, period.type().getCode())
                .eq(PetReportPO::getPeriodStart, period.startDate())
                .eq(PetReportPO::getPeriodEnd, period.endDate()))));
    }

    @Override public List<PetReport> findByUserIdAndPetId(Long userId, Long petId, int offset, int limit) {
        return mapper.selectList(new LambdaQueryWrapper<PetReportPO>()
                .eq(PetReportPO::getUserId, userId).eq(PetReportPO::getPetId, petId)
                .orderByDesc(PetReportPO::getPeriodEnd).last("LIMIT " + offset + "," + limit))
                .stream().map(this::toDomain).toList();
    }

    @Override public void delete(PetReport report) { mapper.deleteById(report.getId()); }

    private PetReportPO toPO(PetReport item) {
        PetReportPO po = new PetReportPO(); po.setId(item.getId()); po.setUserId(item.getUserId());
        po.setPetId(item.getPetId()); po.setPeriodType(item.getPeriodType().getCode());
        po.setPeriodStart(item.getPeriodStart()); po.setPeriodEnd(item.getPeriodEnd());
        po.setDataStatus(item.getDataStatus()); po.setStatus(item.getStatus());
        po.setSnapshotVersion(item.getSnapshotVersion()); po.setCalculationVersion(item.getCalculationVersion());
        po.setFactSnapshotJson(item.getFactSnapshotJson()); po.setContentJson(item.getContentJson());
        po.setContentMode(item.getContentMode()); po.setModelName(item.getModelName());
        po.setPromptVersion(item.getPromptVersion()); po.setGenerationTrigger(item.getGenerationTrigger());
        po.setGenerationVersion(item.getGenerationVersion()); po.setLastErrorCode(item.getLastErrorCode());
        po.setLastErrorMessage(item.getLastErrorMessage()); po.setGeneratedAt(item.getGeneratedAt());
        po.setIsDeleted(item.isDeleted()); po.setCreatedAt(item.getCreatedAt()); po.setUpdatedAt(item.getUpdatedAt());
        return po;
    }

    private PetReport toDomain(PetReportPO po) {
        if (po == null) return null;
        return PetReport.restore(po.getId(), po.getUserId(), po.getPetId(),
                ReportPeriodType.fromCode(po.getPeriodType()), po.getPeriodStart(), po.getPeriodEnd(),
                po.getDataStatus(), po.getStatus(), po.getSnapshotVersion(), po.getCalculationVersion(),
                po.getFactSnapshotJson(), po.getContentJson(), po.getContentMode(), po.getModelName(),
                po.getPromptVersion(), po.getGenerationTrigger(), po.getGenerationVersion() == null ? 1 : po.getGenerationVersion(),
                po.getLastErrorCode(), po.getLastErrorMessage(), po.getGeneratedAt(),
                Boolean.TRUE.equals(po.getIsDeleted()), po.getCreatedAt(), po.getUpdatedAt());
    }
}
