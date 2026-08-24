package com.fafa.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fafa.domain.model.expense.ExpenseCategory;
import com.fafa.domain.model.expense.ExpenseRecord;
import com.fafa.domain.model.expense.ExpenseSourceType;
import com.fafa.domain.model.expense.Money;
import com.fafa.domain.repository.ExpenseRecordRepository;
import com.fafa.infrastructure.persistence.mapper.ExpenseRecordMapper;
import com.fafa.infrastructure.persistence.po.ExpenseRecordPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** 开支记录仓储实现。 */
@Repository
@RequiredArgsConstructor
public class ExpenseRecordRepositoryImpl implements ExpenseRecordRepository {

    private final ExpenseRecordMapper mapper;

    @Override
    public ExpenseRecord save(ExpenseRecord record) {
        ExpenseRecordPO po = toPO(record);
        if (po.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        return toDomain(mapper.selectById(po.getId()));
    }

    @Override
    public Optional<ExpenseRecord> findByIdAndUserId(Long id, Long userId) {
        LambdaQueryWrapper<ExpenseRecordPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExpenseRecordPO::getId, id).eq(ExpenseRecordPO::getUserId, userId);
        return Optional.ofNullable(toDomain(mapper.selectOne(wrapper)));
    }

    @Override
    public List<ExpenseRecord> findByUserIdAndPetIdAndDateRange(Long userId, Long petId,
                                                                LocalDate startDate, LocalDate endDate,
                                                                ExpenseCategory category, int offset, int limit) {
        LambdaQueryWrapper<ExpenseRecordPO> wrapper = baseRange(userId, petId, startDate, endDate);
        if (category != null) {
            wrapper.eq(ExpenseRecordPO::getCategory, category.getCode());
        }
        wrapper.orderByDesc(ExpenseRecordPO::getExpenseDate).orderByDesc(ExpenseRecordPO::getId)
                .last("LIMIT " + offset + "," + limit);
        return mapper.selectList(wrapper).stream().map(this::toDomain).toList();
    }

    @Override
    public long countByUserIdAndPetIdAndDateRange(Long userId, Long petId, LocalDate startDate,
                                                  LocalDate endDate, ExpenseCategory category) {
        LambdaQueryWrapper<ExpenseRecordPO> wrapper = baseRange(userId, petId, startDate, endDate);
        if (category != null) {
            wrapper.eq(ExpenseRecordPO::getCategory, category.getCode());
        }
        return mapper.selectCount(wrapper);
    }

    @Override
    public List<ExpenseRecord> findActiveByUserIdAndPetIdAndDateRange(Long userId, Long petId,
                                                                      LocalDate startDate,
                                                                      LocalDate endDate) {
        return mapper.selectList(baseRange(userId, petId, startDate, endDate)
                        .orderByAsc(ExpenseRecordPO::getExpenseDate).orderByAsc(ExpenseRecordPO::getId))
                .stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<ExpenseRecord> findByMedicalVisitId(Long userId, Long medicalVisitId) {
        LambdaQueryWrapper<ExpenseRecordPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExpenseRecordPO::getUserId, userId)
                .eq(ExpenseRecordPO::getMedicalVisitId, medicalVisitId);
        return Optional.ofNullable(toDomain(mapper.selectOne(wrapper)));
    }

    @Override
    public Optional<ExpenseRecord> findByMedicalVisitIdIncludingDeleted(Long userId, Long medicalVisitId) {
        return Optional.ofNullable(toDomain(mapper.selectMedicalIncludingDeleted(userId, medicalVisitId)));
    }

    @Override
    public void restoreMedical(ExpenseRecord record) {
        if (mapper.restoreMedical(toPO(record)) != 1) {
            throw new IllegalStateException("恢复医疗开支失败");
        }
    }

    @Override
    public void deleteManualById(Long id, Long userId) {
        LambdaQueryWrapper<ExpenseRecordPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExpenseRecordPO::getId, id).eq(ExpenseRecordPO::getUserId, userId);
        mapper.delete(wrapper);
    }

    @Override
    public void deleteMedicalSource(Long userId, Long medicalVisitId) {
        mapper.deleteMedicalSource(userId, medicalVisitId);
    }

    private LambdaQueryWrapper<ExpenseRecordPO> baseRange(Long userId, Long petId,
                                                           LocalDate startDate, LocalDate endDate) {
        return new LambdaQueryWrapper<ExpenseRecordPO>()
                .eq(ExpenseRecordPO::getUserId, userId)
                .eq(ExpenseRecordPO::getPetId, petId)
                .ge(ExpenseRecordPO::getExpenseDate, startDate)
                .le(ExpenseRecordPO::getExpenseDate, endDate);
    }

    private ExpenseRecord toDomain(ExpenseRecordPO po) {
        if (po == null) {
            return null;
        }
        return ExpenseRecord.restore(po.getId(), po.getUserId(), po.getPetId(),
                ExpenseCategory.fromCode(po.getCategory()), new Money(po.getAmount(), po.getCurrency()),
                po.getExpenseDate(), po.getTitle(), po.getNotes(), po.getReceiptMediaId(),
                po.getFoodCardId(), po.getFoodNameSnapshot(), po.getMedicalVisitId(),
                ExpenseSourceType.valueOf(po.getSourceType().equals("medical_visit")
                        ? "MEDICAL_VISIT" : "MANUAL"), Boolean.TRUE.equals(po.getSystemGenerated()),
                Boolean.TRUE.equals(po.getIsDeleted()), po.getCreatedAt(), po.getUpdatedAt());
    }

    private ExpenseRecordPO toPO(ExpenseRecord record) {
        ExpenseRecordPO po = new ExpenseRecordPO();
        po.setId(record.getId());
        po.setUserId(record.getUserId());
        po.setPetId(record.getPetId());
        po.setCategory(record.getCategory().getCode());
        po.setAmount(record.getMoney().amount());
        po.setCurrency(record.getMoney().currency());
        po.setExpenseDate(record.getExpenseDate());
        po.setTitle(record.getTitle());
        po.setNotes(record.getNotes());
        po.setReceiptMediaId(record.getReceiptMediaId());
        po.setFoodCardId(record.getFoodCardId());
        po.setFoodNameSnapshot(record.getFoodNameSnapshot());
        po.setMedicalVisitId(record.getMedicalVisitId());
        po.setSourceType(record.getSourceType().getCode());
        po.setSystemGenerated(record.isSystemGenerated());
        po.setIsDeleted(record.isDeleted());
        return po;
    }
}
