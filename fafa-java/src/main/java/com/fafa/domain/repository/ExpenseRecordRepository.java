package com.fafa.domain.repository;

import com.fafa.domain.model.expense.ExpenseCategory;
import com.fafa.domain.model.expense.ExpenseRecord;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** 开支记录仓储，所有查询都必须带用户和宠物归属边界。 */
public interface ExpenseRecordRepository {

    ExpenseRecord save(ExpenseRecord record);

    Optional<ExpenseRecord> findByIdAndUserId(Long id, Long userId);

    List<ExpenseRecord> findByUserIdAndPetIdAndDateRange(Long userId, Long petId,
                                                         LocalDate startDate, LocalDate endDate,
                                                         ExpenseCategory category, int offset, int limit);

    long countByUserIdAndPetIdAndDateRange(Long userId, Long petId, LocalDate startDate,
                                           LocalDate endDate, ExpenseCategory category);

    List<ExpenseRecord> findActiveByUserIdAndPetIdAndDateRange(Long userId, Long petId,
                                                               LocalDate startDate, LocalDate endDate);

    Optional<ExpenseRecord> findByMedicalVisitId(Long userId, Long medicalVisitId);

    Optional<ExpenseRecord> findByMedicalVisitIdIncludingDeleted(Long userId, Long medicalVisitId);

    void restoreMedical(ExpenseRecord record);

    void deleteManualById(Long id, Long userId);

    void deleteMedicalSource(Long userId, Long medicalVisitId);
}
