package com.fafa.domain.repository;

import com.fafa.domain.model.medical.MedicalVisit;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MedicalVisitRepository {

    MedicalVisit save(MedicalVisit visit);

    Optional<MedicalVisit> findById(Long id);

    List<MedicalVisit> findByPetIdAndDateRange(Long petId, LocalDate start, LocalDate end,
                                               int offset, int limit);

    long countByPetIdAndDateRange(Long petId, LocalDate start, LocalDate end);

    void deleteById(Long id);
}
