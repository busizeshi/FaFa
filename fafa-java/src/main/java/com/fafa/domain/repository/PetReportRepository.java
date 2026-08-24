package com.fafa.domain.repository;

import com.fafa.domain.model.report.PetReport;
import com.fafa.domain.model.report.ReportPeriod;

import java.util.List;
import java.util.Optional;

public interface PetReportRepository {
    PetReport save(PetReport report);
    Optional<PetReport> findByIdAndUserId(Long id, Long userId);
    Optional<PetReport> findByPeriod(Long userId, Long petId, ReportPeriod period);
    List<PetReport> findByUserIdAndPetId(Long userId, Long petId, int offset, int limit);
    void delete(PetReport report);
}
