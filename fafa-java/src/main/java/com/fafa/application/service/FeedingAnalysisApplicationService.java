package com.fafa.application.service;

import cn.dev33.satoken.stp.StpUtil;
import com.fafa.application.dto.feeding.FeedingAnalysisDTO;
import com.fafa.common.BusinessException;
import com.fafa.common.ErrorCode;
import com.fafa.domain.model.record.DailyStatus;
import com.fafa.domain.model.record.FeedingRecord;
import com.fafa.domain.model.record.WeightRecord;
import com.fafa.domain.repository.DailyStatusRepository;
import com.fafa.domain.repository.FeedingRecordRepository;
import com.fafa.domain.repository.WeightRecordRepository;
import com.fafa.infrastructure.config.FaFaProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 动态喂养分析应用服务，首版只返回 Java 计算的确定性统计。 */
@Service
@RequiredArgsConstructor
public class FeedingAnalysisApplicationService {

    private final FeedingRecordRepository feedingRecordRepository;
    private final WeightRecordRepository weightRecordRepository;
    private final DailyStatusRepository dailyStatusRepository;
    private final PetApplicationService petApplicationService;
    private final FaFaProperties properties;

    public FeedingAnalysisDTO analyze(Long petId, LocalDate startDate, LocalDate endDate) {
        petApplicationService.checkPetOwnership(petId);
        validateRange(startDate, endDate);
        List<FeedingRecord> feedings = feedingRecordRepository.findByPetIdAndDateRange(petId, startDate, endDate);
        List<WeightRecord> weights = weightRecordRepository.findByPetIdAndDateRange(petId, startDate, endDate);
        List<DailyStatus> statuses = dailyStatusRepository.findByPetIdAndDateRange(petId, startDate, endDate);
        FeedingAnalysisDTO.FactsDTO facts = buildFacts(feedings, weights, statuses);
        List<FeedingAnalysisDTO.ObservationDTO> observations = buildObservations(feedings, facts);
        return new FeedingAnalysisDTO(petId, new FeedingAnalysisDTO.RangeDTO(startDate, endDate), facts,
                observations, List.of("复核包装建议和当前活动变化", "软便或拒食持续时咨询兽医"),
                List.of("未记录不等于未发生", "结果是数据观察，不是诊断或减量处方"),
                "java_stats", properties.getFeeding().getPolicyVersion());
    }

    private FeedingAnalysisDTO.FactsDTO buildFacts(List<FeedingRecord> feedings,
                                                   List<WeightRecord> weights,
                                                   List<DailyStatus> statuses) {
        Map<LocalDate, BigDecimal> dailyAmount = new LinkedHashMap<>();
        String unit = null;
        for (FeedingRecord record : feedings) {
            if (record.getAmount() == null) continue;
            if (unit == null) unit = record.getUnit();
            if (!unit.equals(record.getUnit())) continue;
            dailyAmount.merge(record.getFeedingTime().toLocalDate(), record.getAmount(), BigDecimal::add);
        }
        BigDecimal average = dailyAmount.isEmpty() ? null
                : dailyAmount.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(dailyAmount.size()), 2, RoundingMode.HALF_UP);
        WeightRecord first = weights.stream().min(Comparator.comparing(WeightRecord::getRecordDate)).orElse(null);
        WeightRecord last = weights.stream().max(Comparator.comparing(WeightRecord::getRecordDate)).orElse(null);
        BigDecimal change = first == null || last == null ? null : last.getWeight().subtract(first.getWeight());
        int planned = (int) feedings.stream().filter(item -> item.getFeedingPlanId() != null).count();
        int appetiteNormal = (int) statuses.stream().filter(item -> item.getAppetite() != null
                && "normal".equals(item.getAppetite().getCode())).count();
        int soft = (int) statuses.stream().filter(item -> item.getDefecation() != null
                && "soft".equals(item.getDefecation().getCode())).count();
        int refusal = (int) statuses.stream().filter(item -> item.getAppetite() != null
                && "refused".equals(item.getAppetite().getCode())).count();
        return new FeedingAnalysisDTO.FactsDTO(dailyAmount.size(), average, unit, planned,
                null, new FeedingAnalysisDTO.WeightDTO(
                first == null ? null : first.getWeight(), last == null ? null : last.getWeight(), change,
                weights.size()), new FeedingAnalysisDTO.DailyStatusFactsDTO(appetiteNormal, soft, refusal));
    }

    private List<FeedingAnalysisDTO.ObservationDTO> buildObservations(List<FeedingRecord> feedings,
                                                                       FeedingAnalysisDTO.FactsDTO facts) {
        List<FeedingAnalysisDTO.ObservationDTO> result = new ArrayList<>();
        if (!feedings.isEmpty() && facts.weight().sampleCount() > 0) {
            result.add(new FeedingAnalysisDTO.ObservationDTO("amount_vs_weight",
                    "记录期间喂食量和体重均有数据，可继续观察趋势",
                    List.of("feeding_record", "weight_record")));
        }
        if (facts.dailyStatus().softStoolDays() > 0 || facts.dailyStatus().refusalDays() > 0) {
            result.add(new FeedingAnalysisDTO.ObservationDTO("reaction",
                    "记录中出现软便或拒食，请结合换粮过程和其他事实复核",
                    List.of("daily_status", "feeding_record")));
        }
        if (result.isEmpty()) {
            result.add(new FeedingAnalysisDTO.ObservationDTO("insufficient",
                    "当前范围内可用关联事实较少，暂不生成趋势判断", List.of("feeding_record")));
        }
        return result;
    }

    private void validateRange(LocalDate start, LocalDate end) {
        if (start == null || end == null || start.isAfter(end)
                || java.time.temporal.ChronoUnit.DAYS.between(start, end) > properties.getFeeding().getMaxRangeDays()) {
            throw new BusinessException(ErrorCode.FEEDING_ANALYSIS_RANGE_INVALID);
        }
    }
}
