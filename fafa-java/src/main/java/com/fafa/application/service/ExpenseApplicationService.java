package com.fafa.application.service;

import cn.dev33.satoken.stp.StpUtil;
import com.fafa.application.dto.PageDTO;
import com.fafa.application.dto.expense.ExpenseCategoryDTO;
import com.fafa.application.dto.expense.ExpenseCommand;
import com.fafa.application.dto.expense.ExpenseDTO;
import com.fafa.common.BusinessException;
import com.fafa.common.ErrorCode;
import com.fafa.domain.model.expense.ExpenseCategory;
import com.fafa.domain.model.expense.ExpenseRecord;
import com.fafa.domain.model.expense.Money;
import com.fafa.domain.model.food.FoodCard;
import com.fafa.domain.model.medical.MedicalVisit;
import com.fafa.domain.model.media.MediaAsset;
import com.fafa.domain.repository.ExpenseRecordRepository;
import com.fafa.domain.repository.FoodCardRepository;
import com.fafa.domain.repository.MediaAssetRepository;
import com.fafa.infrastructure.config.FaFaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

/** 手工开支写入、归属校验和医疗来源同步应用服务。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseApplicationService {

    private final ExpenseRecordRepository repository;
    private final PetApplicationService petApplicationService;
    private final FoodCardRepository foodCardRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final FaFaProperties properties;

    @Transactional
    public ExpenseDTO create(ExpenseCommand command) {
        Long userId = currentUserId();
        ExpenseRecord record = buildManual(userId, command);
        ExpenseRecord saved = repository.save(record);
        log.info("手工开支创建成功: userId={}, petId={}, expenseId={}", userId, saved.getPetId(), saved.getId());
        return toDTO(saved);
    }

    public PageDTO<ExpenseDTO> list(Long petId, LocalDate startDate, LocalDate endDate,
                                    String categoryCode, int page, int size) {
        Long userId = currentUserId();
        ensurePet(userId, petId);
        DateRange range = validateRange(startDate, endDate);
        ExpenseCategory category = parseCategory(categoryCode, true);
        int safePage = Math.max(1, page);
        int safeSize = Math.min(Math.max(1, size), 50);
        int offset = (safePage - 1) * safeSize;
        List<ExpenseDTO> records = repository.findByUserIdAndPetIdAndDateRange(userId, petId,
                        range.start(), range.end(), category, offset, safeSize)
                .stream().map(this::toDTO).toList();
        long total = repository.countByUserIdAndPetIdAndDateRange(userId, petId,
                range.start(), range.end(), category);
        return PageDTO.of(records, total, safePage, safeSize);
    }

    public ExpenseDTO get(Long id) {
        return toDTO(findOwned(id, currentUserId()));
    }

    public ExpenseDTO getForUser(Long userId, Long id) {
        return toDTO(findOwned(id, userId));
    }

    @Transactional
    public ExpenseDTO update(Long id, ExpenseCommand command) {
        Long userId = currentUserId();
        ExpenseRecord record = findOwned(id, userId);
        if (!record.isEditableByUser()) {
            throw new BusinessException(ErrorCode.EXPENSE_AUTO_RECORD_IMMUTABLE);
        }
        ExpenseRecord replacement = buildManual(userId, command);
        if (!record.getPetId().equals(replacement.getPetId())) {
            throw new BusinessException(ErrorCode.EXPENSE_FIELD_INVALID, "开支所属宠物不能修改");
        }
        try {
            record.updateManual(replacement.getCategory(), replacement.getMoney(), replacement.getExpenseDate(),
                    replacement.getTitle(), replacement.getNotes(), replacement.getReceiptMediaId(),
                    replacement.getFoodCardId(), replacement.getFoodNameSnapshot(), LocalDate.now());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            throw new BusinessException(ErrorCode.EXPENSE_FIELD_INVALID, ex.getMessage());
        }
        return toDTO(repository.save(record));
    }

    @Transactional
    public void delete(Long id) {
        Long userId = currentUserId();
        ExpenseRecord record = findOwned(id, userId);
        if (!record.isEditableByUser()) {
            throw new BusinessException(ErrorCode.EXPENSE_AUTO_RECORD_IMMUTABLE);
        }
        repository.deleteManualById(record.getId(), userId);
        log.info("手工开支删除成功: userId={}, expenseId={}", userId, id);
    }

    public List<ExpenseCategoryDTO> categories() {
        return Arrays.stream(ExpenseCategory.values())
                .map(item -> new ExpenseCategoryDTO(item.getCode(), item.getLabel(), item.getSortOrder()))
                .toList();
    }

    /** 医疗档案与开支在同一事务内同步，避免重复生成或遗留孤儿自动账单。 */
    @Transactional
    public void syncMedicalVisitExpense(MedicalVisit visit) {
        if (!properties.getExpense().isAutoSyncMedical()) {
            return;
        }
        if (visit.getTotalCost() == null || visit.getTotalCost().signum() == 0) {
            repository.deleteMedicalSource(visit.getUserId(), visit.getId());
            return;
        }
        try {
            BigDecimal amount = visit.getTotalCost().setScale(2, RoundingMode.HALF_UP);
            if (amount.compareTo(properties.getExpense().getMaxAmount()) > 0) {
                throw new IllegalArgumentException("医疗费用超过单笔开支上限");
            }
            Money money = new Money(amount, properties.getExpense().getDefaultCurrency());
            String title = medicalTitle(visit.getHospitalName());
            var existing = repository.findByMedicalVisitIdIncludingDeleted(visit.getUserId(), visit.getId());
            if (existing.isPresent()) {
                ExpenseRecord record = existing.get();
                record.syncMedicalVisit(money, visit.getVisitDate(), title, "由就医档案自动同步", LocalDate.now());
                if (record.isDeleted()) {
                    record.restoreDeleted();
                    repository.restoreMedical(record);
                } else {
                    repository.save(record);
                }
            } else {
                repository.save(ExpenseRecord.createFromMedicalVisit(visit.getUserId(), visit.getPetId(),
                        visit.getId(), money, visit.getVisitDate(), title, "由就医档案自动同步", LocalDate.now()));
            }
        } catch (RuntimeException ex) {
            log.error("医疗费用同步失败: userId={}, visitId={}", visit.getUserId(), visit.getId(), ex);
            if (ex instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException(ErrorCode.EXPENSE_MEDICAL_SYNC_FAILED, ex.getMessage());
        }
    }

    @Transactional
    public void removeMedicalVisitExpense(Long userId, Long medicalVisitId) {
        if (properties.getExpense().isAutoSyncMedical()) {
            repository.deleteMedicalSource(userId, medicalVisitId);
        }
    }

    public List<ExpenseDTO> listForUser(Long userId, Long petId, LocalDate startDate, LocalDate endDate) {
        ensurePet(userId, petId);
        DateRange range = validateRange(startDate, endDate);
        return repository.findByUserIdAndPetIdAndDateRange(userId, petId, range.start(), range.end(),
                        null, 0, 200)
                .stream().map(this::toDTO).toList();
    }

    private ExpenseRecord buildManual(Long userId, ExpenseCommand command) {
        if (command == null) {
            throw new BusinessException(ErrorCode.EXPENSE_FIELD_INVALID);
        }
        ensurePet(userId, command.petId());
        ExpenseCategory category = parseCategory(command.category(), false);
        String currency = normalizeCurrency(command.currency());
        BigDecimal amount = normalizeAmount(command.amount());
        Long foodCardId = command.foodCardId();
        String foodNameSnapshot = null;
        if (foodCardId != null) {
            FoodCard foodCard = foodCardRepository.findByIdAndUserId(foodCardId, userId)
                    .filter(item -> item.getPetId().equals(command.petId()))
                    .orElseThrow(() -> new BusinessException(ErrorCode.EXPENSE_FIELD_INVALID,
                            "食品卡不存在或不属于当前宠物"));
            foodNameSnapshot = foodCard.getName();
        }
        validateReceipt(userId, command.petId(), command.receiptMediaId());
        try {
            return ExpenseRecord.createManual(userId, command.petId(), category,
                    new Money(amount, currency), command.expenseDate(), command.title(), command.notes(),
                    command.receiptMediaId(), foodCardId, foodNameSnapshot, LocalDate.now());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.EXPENSE_FIELD_INVALID, ex.getMessage());
        }
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0 || amount.compareTo(properties.getExpense().getMaxAmount()) > 0) {
            throw new BusinessException(ErrorCode.EXPENSE_FIELD_INVALID, "金额必须大于0且不超过配置上限");
        }
        try {
            return amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw new BusinessException(ErrorCode.EXPENSE_FIELD_INVALID, "金额最多保留两位小数");
        }
    }

    private String normalizeCurrency(String currency) {
        String value = currency == null || currency.isBlank()
                ? properties.getExpense().getDefaultCurrency() : currency.trim().toUpperCase();
        if (!properties.getExpense().getDefaultCurrency().equalsIgnoreCase(value)) {
            throw new BusinessException(ErrorCode.EXPENSE_CURRENCY_UNSUPPORTED);
        }
        return value;
    }

    private ExpenseCategory parseCategory(String code, boolean optional) {
        if (optional && (code == null || code.isBlank())) {
            return null;
        }
        try {
            return ExpenseCategory.fromCode(code);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.EXPENSE_CATEGORY_INVALID, ex.getMessage());
        }
    }

    private void validateReceipt(Long userId, Long petId, Long mediaId) {
        if (mediaId == null) {
            return;
        }
        List<MediaAsset> media = mediaAssetRepository.findByIds(userId, List.of(mediaId));
        if (media.size() != 1 || !petId.equals(media.get(0).getPetId())) {
            throw new BusinessException(ErrorCode.EXPENSE_FIELD_INVALID, "小票素材不存在或不属于当前宠物");
        }
    }

    private ExpenseRecord findOwned(Long id, Long userId) {
        return repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXPENSE_NOT_FOUND));
    }

    private void ensurePet(Long userId, Long petId) {
        if (petId == null) {
            throw new BusinessException(ErrorCode.EXPENSE_FIELD_INVALID, "宠物不能为空");
        }
        try {
            petApplicationService.checkPetOwnership(userId, petId);
        } catch (RuntimeException ex) {
            throw new BusinessException(ErrorCode.EXPENSE_NOT_FOUND, "宠物不存在或无权访问");
        }
    }

    private DateRange validateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)
                || endDate.isAfter(LocalDate.now())
                || ChronoUnit.DAYS.between(startDate, endDate) > properties.getExpense().getMaxRangeDays()) {
            throw new BusinessException(ErrorCode.EXPENSE_RANGE_INVALID);
        }
        return new DateRange(startDate, endDate);
    }

    private ExpenseDTO toDTO(ExpenseRecord record) {
        return new ExpenseDTO(record.getId(), record.getPetId(), record.getCategory().getCode(),
                record.getCategory().getLabel(), record.getMoney().amount(), record.getMoney().currency(),
                record.getExpenseDate(), record.getTitle(), record.getNotes(), record.getReceiptMediaId(),
                record.getFoodCardId(), record.getFoodNameSnapshot(), record.getMedicalVisitId(),
                record.getSourceType().getCode(), record.isSystemGenerated(), record.isEditableByUser(),
                record.getCreatedAt(), record.getUpdatedAt());
    }

    private String medicalTitle(String hospitalName) {
        String title = "就诊费用 - " + hospitalName;
        return title.length() <= 100 ? title : title.substring(0, 100);
    }

    private Long currentUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    private record DateRange(LocalDate start, LocalDate end) {
    }
}
