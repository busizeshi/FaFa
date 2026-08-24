package com.fafa.application.service;

import cn.dev33.satoken.stp.StpUtil;
import com.fafa.application.dto.record.SaveWeightRecordCommand;
import com.fafa.application.dto.record.WeightRecordDTO;
import com.fafa.application.dto.record.WeightTrendDTO;
import com.fafa.common.BusinessException;
import com.fafa.common.ErrorCode;
import com.fafa.domain.model.pet.Pet;
import com.fafa.domain.model.collaboration.PetPermission;
import com.fafa.domain.model.record.WeightRecord;
import com.fafa.domain.repository.PetRepository;
import com.fafa.domain.repository.WeightRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * 体重记录应用服务
 *
 * 负责协调体重记录相关业务：
 * - 新增体重记录（同事务联动更新 pet.weight 冗余字段）
 * - 最近 N 条与趋势查询（含逐条变化值）
 * - 删除（逻辑删除，不回滚 pet.weight）
 *
 * @author FaFa Team
 * @since 2.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeightRecordApplicationService {

    private static final int MAX_RANGE_DAYS = 366;
    private static final int DEFAULT_TREND_DAYS = 90;
    private static final int MAX_LIMIT = 100;

    private final WeightRecordRepository weightRecordRepository;
    private final PetRepository petRepository;
    private final PetApplicationService petApplicationService;
    private final PetAccessApplicationService petAccessApplicationService;

    /**
     * 新增体重记录，同一事务内同步 pet.weight 冗余字段
     */
    @Transactional
    public WeightRecordDTO create(SaveWeightRecordCommand command) {
        Pet pet = petApplicationService.getPetDomain(command.petId());
        petAccessApplicationService.require(getCurrentUserId(), command.petId(), PetPermission.CREATE_RECORD);
        validateNotFuture(command.recordDate());

        WeightRecord record = WeightRecord.create(
                command.petId(),
                command.recordDate(),
                command.weight(),
                command.notes(),
                getCurrentUserId()
        );

        WeightRecord saved = weightRecordRepository.save(record);

        // 联动更新宠物档案的体重冗余字段（同事务，保证一致性）
        pet.updateWeight(command.weight());
        petRepository.save(pet);

        log.info("体重记录新增成功: petId={}, date={}, weight={}kg",
                command.petId(), command.recordDate(), command.weight());
        return toDTO(saved, null);
    }

    /**
     * 最近 N 条记录（按日期倒序，每条含较上一条的变化值）
     */
    public List<WeightRecordDTO> latest(Long petId, int limit) {
        petAccessApplicationService.require(getCurrentUserId(), petId, PetPermission.READ_RECORD);
        int safeLimit = Math.max(1, Math.min(limit, MAX_LIMIT));

        // 多取一条更早的记录，保证返回列表中最早一条也能算出 change
        List<WeightRecord> records = weightRecordRepository.findLatest(petId, safeLimit + 1);

        List<WeightRecordDTO> result = new ArrayList<>();
        for (int i = 0; i < Math.min(safeLimit, records.size()); i++) {
            WeightRecord previous = i + 1 < records.size() ? records.get(i + 1) : null;
            result.add(toDTO(records.get(i), previous));
        }
        return result;
    }

    /**
     * 体重趋势（升序 + 逐条变化 + 区间总变化；区间缺省为最近 90 天）
     */
    public WeightTrendDTO trend(Long petId, LocalDate start, LocalDate end) {
        petAccessApplicationService.require(getCurrentUserId(), petId, PetPermission.READ_RECORD);

        LocalDate effectiveEnd = end != null ? end : LocalDate.now();
        LocalDate effectiveStart = start != null ? start : effectiveEnd.minusDays(DEFAULT_TREND_DAYS);
        validateRange(effectiveStart, effectiveEnd);

        List<WeightRecord> records = weightRecordRepository.findByPetIdAndDateRange(
                petId, effectiveStart, effectiveEnd);

        // 取区间前一条记录，保证区间首条也能算出 change
        WeightRecord previousOfFirst = findPreviousBefore(petId, effectiveStart);

        List<WeightRecordDTO> dtos = new ArrayList<>();
        WeightRecord previous = previousOfFirst;
        for (WeightRecord record : records) {
            dtos.add(toDTO(record, previous));
            previous = record;
        }

        WeightRecordDTO latest = dtos.isEmpty() ? null : dtos.get(dtos.size() - 1);
        BigDecimal totalChange = dtos.size() >= 2
                ? dtos.get(dtos.size() - 1).weight().subtract(dtos.get(0).weight())
                : null;

        return new WeightTrendDTO(dtos, latest, totalChange);
    }

    /**
     * 删除体重记录（逻辑删除）
     *
     * 不回滚 pet.weight：冗余字段表示「最近已知体重」，回滚语义不明确。
     */
    @Transactional
    public void delete(Long id) {
        WeightRecord record = weightRecordRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECORD_NOT_FOUND));
        petAccessApplicationService.requireOwner(getCurrentUserId(), record.getPetId());

        weightRecordRepository.deleteById(id);
        log.info("体重记录删除成功: id={}", id);
    }

    /**
     * 查找区间首条记录之前的最近一条（用于计算首条的 change）
     */
    private WeightRecord findPreviousBefore(Long petId, LocalDate start) {
        return weightRecordRepository.findLatestBefore(petId, start).orElse(null);
    }

    private void validateNotFuture(LocalDate date) {
        if (date == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "记录日期不能为空");
        }
        if (date.isAfter(LocalDate.now())) {
            throw new BusinessException(ErrorCode.RECORD_DATE_IN_FUTURE);
        }
    }

    private void validateRange(LocalDate start, LocalDate end) {
        if (start.isAfter(end)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "开始日期不能晚于结束日期");
        }
        if (ChronoUnit.DAYS.between(start, end) > MAX_RANGE_DAYS) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "查询范围不能超过 " + MAX_RANGE_DAYS + " 天");
        }
    }

    private Long getCurrentUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    /**
     * 领域对象转DTO（change 为较 previous 的变化值）
     */
    private WeightRecordDTO toDTO(WeightRecord r, WeightRecord previous) {
        return new WeightRecordDTO(
                r.getId(),
                r.getPetId(),
                r.getRecordDate(),
                r.getWeight(),
                r.getNotes(),
                r.getWeightChange(previous),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }
}
