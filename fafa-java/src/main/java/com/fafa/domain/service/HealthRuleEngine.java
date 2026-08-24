package com.fafa.domain.service;

import com.fafa.domain.model.health.*;
import com.fafa.domain.model.medical.MedicationRecord;
import com.fafa.domain.model.medical.MedicationRecordStatus;
import com.fafa.domain.model.record.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 纯规则健康预警引擎，不访问仓储、不调用 AI。 */
public class HealthRuleEngine {

    private final int increasedWaterDays;
    private final int appetiteEnergyDeclineDays;
    private final int diarrheaDays;
    private final int missedMedicationCount;

    public HealthRuleEngine(HealthRuleThresholds thresholds) {
        this.increasedWaterDays = thresholds.increasedWaterDays();
        this.appetiteEnergyDeclineDays = thresholds.appetiteEnergyDeclineDays();
        this.diarrheaDays = thresholds.diarrheaDays();
        this.missedMedicationCount = thresholds.missedMedicationCount();
    }

    public List<HealthAlertDraft> evaluate(HealthRuleContext context) {
        List<HealthAlertDraft> result = new ArrayList<>();
        addIfPresent(result, refusalWithVomiting(context));
        addIfPresent(result, appetiteEnergyDecline(context));
        addIfPresent(result, increasedWater(context));
        addIfPresent(result, diarrhea(context));
        addIfPresent(result, vomitingMultiple(context));
        addIfPresent(result, missedMedication(context));
        return result;
    }

    private Optional<HealthAlertDraft> refusalWithVomiting(HealthRuleContext context) {
        return context.dailyStatuses().stream()
                .filter(item -> item.getAppetite() == Appetite.REFUSED
                        && item.getVomiting() != null && item.getVomiting() != Vomiting.NONE)
                .findFirst()
                .map(item -> draft("REFUSAL_WITH_VOMITING", HealthAlertSeverity.RED,
                        "今日出现需要关注的组合变化",
                        "记录显示今日出现拒食与呕吐组合，建议尽快联系兽医。",
                        List.of(evidence(item, "appetite", item.getAppetite().getCode()),
                                evidence(item, "vomiting", item.getVomiting().getCode())),
                        item.getRecordDate(), item.getRecordDate()));
    }

    private Optional<HealthAlertDraft> appetiteEnergyDecline(HealthRuleContext context) {
        return consecutiveStatuses(context.dailyStatuses(), appetiteEnergyDeclineDays,
                item -> (item.getAppetite() == Appetite.DECREASED || item.getAppetite() == Appetite.REFUSED)
                        && item.getEnergy() == Energy.LETHARGIC,
                "APPETITE_ENERGY_DECLINE_2D", HealthAlertSeverity.YELLOW,
                "连续记录出现食欲和精神变化",
                "最近连续记录中出现食欲下降且精神低迷，建议持续观察并咨询兽医。",
                item -> List.of(evidence(item, "appetite", item.getAppetite().getCode()),
                        evidence(item, "energy", item.getEnergy().getCode())));
    }

    private Optional<HealthAlertDraft> increasedWater(HealthRuleContext context) {
        return consecutiveStatuses(context.dailyStatuses(), increasedWaterDays,
                item -> item.getWaterIntake() == WaterIntake.MORE,
                "INCREASED_WATER_3D", HealthAlertSeverity.YELLOW,
                "连续记录出现饮水偏多",
                "最近连续记录中饮水偏多，建议持续观察相关变化并咨询兽医。",
                item -> List.of(evidence(item, "waterIntake", item.getWaterIntake().getCode())));
    }

    private Optional<HealthAlertDraft> diarrhea(HealthRuleContext context) {
        return consecutiveStatuses(context.dailyStatuses(), diarrheaDays,
                item -> item.getDefecation() == Defecation.DIARRHEA,
                "DIARRHEA_2D", HealthAlertSeverity.YELLOW,
                "连续记录出现腹泻",
                "最近连续记录中出现腹泻，建议持续观察并咨询兽医。",
                item -> List.of(evidence(item, "defecation", item.getDefecation().getCode())));
    }

    private Optional<HealthAlertDraft> vomitingMultiple(HealthRuleContext context) {
        return context.dailyStatuses().stream()
                .filter(item -> item.getVomiting() == Vomiting.MULTIPLE)
                .findFirst()
                .map(item -> draft("VOMITING_MULTIPLE", HealthAlertSeverity.YELLOW,
                        "记录中出现多次呕吐",
                        "记录显示当天出现多次呕吐，建议尽快关注并咨询兽医。",
                        List.of(evidence(item, "vomiting", item.getVomiting().getCode())),
                        item.getRecordDate(), item.getRecordDate()));
    }

    private Optional<HealthAlertDraft> missedMedication(HealthRuleContext context) {
        List<MedicationRecord> missed = context.medicationRecords().stream()
                .filter(item -> item.getStatus() == MedicationRecordStatus.MISSED
                        || item.getStatus() == MedicationRecordStatus.SKIPPED)
                .toList();
        if (missed.size() < missedMedicationCount) {
            return Optional.empty();
        }
        List<HealthEvidence> evidence = missed.stream().limit(10)
                .map(item -> new HealthEvidence("medication_record", String.valueOf(item.getId()),
                        "status", item.getStatus().getCode(), item.getScheduledAt().toLocalDate().toString()))
                .toList();
        return Optional.of(draft("MEDICATION_MISSED_2", HealthAlertSeverity.YELLOW,
                "用药计划有多次未完成记录",
                "记录显示用药计划有多次跳过或漏记，请按医生医嘱确认后续安排。",
                evidence, context.windowStart(), context.windowEnd()));
    }

    private Optional<HealthAlertDraft> consecutiveStatuses(List<DailyStatus> statuses, int days,
                                                            java.util.function.Predicate<DailyStatus> predicate,
                                                            String code, HealthAlertSeverity severity,
                                                            String title, String message,
                                                            Function<DailyStatus, List<HealthEvidence>> evidenceFactory) {
        Map<LocalDate, DailyStatus> matched = statuses.stream()
                .filter(predicate)
                .collect(Collectors.toMap(DailyStatus::getRecordDate, Function.identity(), (left, right) -> left));
        for (LocalDate start : matched.keySet().stream().sorted().toList()) {
            LocalDate end = start.plusDays(days - 1L);
            boolean complete = java.util.stream.IntStream.range(0, days)
                    .mapToObj(start::plusDays).allMatch(matched::containsKey);
            if (complete) {
                List<HealthEvidence> evidence = java.util.stream.IntStream.range(0, days)
                        .mapToObj(start::plusDays)
                        .flatMap(date -> evidenceFactory.apply(matched.get(date)).stream())
                        .toList();
                return Optional.of(draft(code, severity, title, message, evidence, start, end));
            }
        }
        return Optional.empty();
    }

    private HealthAlertDraft draft(String code, HealthAlertSeverity severity, String title,
                                   String message, List<HealthEvidence> evidence,
                                   LocalDate from, LocalDate to) {
        String fingerprint = fingerprint(code, from, to, evidence);
        return new HealthAlertDraft(code, severity, title, message, evidence, from, to, fingerprint);
    }

    private HealthEvidence evidence(DailyStatus item, String field, String value) {
        return new HealthEvidence("daily_status", String.valueOf(item.getId()), field, value,
                item.getRecordDate().toString());
    }

    private String fingerprint(String code, LocalDate from, LocalDate to,
                               List<HealthEvidence> evidence) {
        String raw = code + "|" + from + "|" + to + "|" + evidence.stream()
                .map(item -> String.join("|", item.sourceType(), item.sourceId(), item.field(),
                        item.value(), item.observedDate()))
                .sorted().collect(Collectors.joining(";"));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte value : digest) {
                result.append(String.format("%02x", value));
            }
            return result.toString();
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 不可用", ex);
        }
    }

    private void addIfPresent(List<HealthAlertDraft> result, Optional<HealthAlertDraft> draft) {
        draft.ifPresent(result::add);
    }
}
