package com.fafa.application.service;

import com.fafa.domain.model.pet.Pet;
import com.fafa.domain.repository.*;
import com.fafa.domain.model.report.ReportPeriod;
import com.fafa.infrastructure.config.FaFaProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/** 只读取业务事实并组装快照；此类不调用 AI，也不生成结论。 */
@Service
@RequiredArgsConstructor
public class ReportFactAssembler {
    private final PetRepository petRepository;
    private final DailyStatusRepository dailyStatusRepository;
    private final WeightRecordRepository weightRecordRepository;
    private final FeedingRecordRepository feedingRecordRepository;
    private final EventRecordRepository eventRecordRepository;
    private final ReminderCompletionRepository reminderCompletionRepository;
    private final MedicalVisitRepository medicalVisitRepository;
    private final MedicationRecordRepository medicationRecordRepository;
    private final HealthAlertRepository healthAlertRepository;
    private final ExpenseApplicationService expenseApplicationService;
    private final MediaAssetRepository mediaAssetRepository;
    private final FaFaProperties properties;

    public Map<String, Object> assemble(Long userId, Long petId, ReportPeriod period) {
        Pet pet = ownedPet(userId, petId);
        LocalDate start = period.startDate();
        LocalDate end = period.endDate().isAfter(LocalDate.now()) ? LocalDate.now() : period.endDate();
        Map<String, Object> facts = new LinkedHashMap<>();
        facts.put("schemaVersion", properties.getReport().getSnapshotVersion());
        facts.put("pet", petFacts(pet));
        facts.put("period", Map.of("type", period.type().getCode(), "startDate", start, "endDate", end));
        facts.put("dailyStatuses", dailyFacts(petId, start, end));
        facts.put("weights", weightFacts(petId, start, end));
        facts.put("feeding", feedingFacts(petId, start, end));
        facts.put("events", eventFacts(petId, start, end));
        facts.put("reminders", reminderFacts(userId, petId, start, end));
        facts.put("medicalVisits", medicalFacts(petId, start, end));
        facts.put("medications", medicationFacts(petId, start, end));
        facts.put("healthAlerts", healthAlertFacts(petId));
        facts.put("expenses", expenseFacts(userId, petId, start, end));
        facts.put("media", mediaFacts(userId, petId, start, end));
        return facts;
    }

    private Pet ownedPet(Long userId, Long petId) {
        return petRepository.findById(petId).filter(pet -> userId.equals(pet.getUserId()))
                .orElseThrow(() -> new IllegalArgumentException("宠物不存在或无权访问"));
    }

    private Map<String, Object> petFacts(Pet pet) {
        return map("id", pet.getId(), "name", pet.getName(), "type", pet.getType().getCode(),
                "breed", pet.getBreed(), "gender", pet.getGender().getCode(), "birthDate", pet.getBirthDate(),
                "currentWeight", pet.getWeight());
    }

    private List<Map<String, Object>> dailyFacts(Long petId, LocalDate start, LocalDate end) {
        return dailyStatusRepository.findByPetIdAndDateRange(petId, start, end).stream()
                .map(item -> map("date", item.getRecordDate(), "appetite", item.getAppetite() == null ? null : item.getAppetite().getCode(),
                        "energy", item.getEnergy() == null ? null : item.getEnergy().getCode(),
                        "waterIntake", item.getWaterIntake() == null ? null : item.getWaterIntake().getCode(),
                        "defecation", item.getDefecation() == null ? null : item.getDefecation().getCode(),
                        "vomiting", item.getVomiting() == null ? null : item.getVomiting().getCode(),
                        "urination", item.getUrination() == null ? null : item.getUrination().getCode(),
                        "appetiteAmount", item.getAppetiteAmount(),
                        "waterAmount", item.getWaterAmount(), "notes", item.getNotes()))
                .toList();
    }

    private List<Map<String, Object>> weightFacts(Long petId, LocalDate start, LocalDate end) {
        return weightRecordRepository.findByPetIdAndDateRange(petId, start, end).stream()
                .map(item -> map("id", item.getId(), "date", item.getRecordDate(), "weight", item.getWeight(),
                        "notes", item.getNotes())).toList();
    }

    private List<Map<String, Object>> feedingFacts(Long petId, LocalDate start, LocalDate end) {
        return feedingRecordRepository.findByPetIdAndDateRange(petId, start, end).stream()
                .map(item -> map("id", item.getId(), "time", item.getFeedingTime(),
                        "foodType", item.getFoodType().getCode(), "amount", item.getAmount(),
                        "unit", item.getUnit(), "foodName", item.getFoodNameSnapshot(), "notes", item.getNotes()))
                .toList();
    }

    private List<Map<String, Object>> eventFacts(Long petId, LocalDate start, LocalDate end) {
        return eventRecordRepository.findByPetIdAndDateRange(petId, start, end).stream()
                .map(item -> map("id", item.getId(), "date", item.getEventDate(), "source", item.getSource().getCode(),
                        "type", item.getEventType().getCode(), "title", item.getTitle(), "content", item.getContent(),
                        "tags", item.getTags())).toList();
    }

    private List<Map<String, Object>> reminderFacts(Long userId, Long petId, LocalDate start, LocalDate end) {
        return reminderCompletionRepository.findByPetIdAndTimeBetween(userId, petId,
                        start.atStartOfDay(), end.plusDays(1).atStartOfDay()).stream()
                .map(item -> map("id", item.getId(), "time", item.getCompletionTime(),
                        "title", item.getReminderTitle(), "type", item.getReminderType(), "notes", item.getNotes()))
                .toList();
    }

    private List<Map<String, Object>> medicalFacts(Long petId, LocalDate start, LocalDate end) {
        return medicalVisitRepository.findByPetIdAndDateRange(petId, start, end, 0, 200).stream()
                .map(item -> map("id", item.getId(), "date", item.getVisitDate(), "hospital", item.getHospitalName(),
                        "chiefComplaint", item.getChiefComplaint(), "diagnosis", item.getDiagnosis(),
                        "treatment", item.getTreatment(), "totalCost", item.getTotalCost(), "notes", item.getNotes()))
                .toList();
    }

    private List<Map<String, Object>> medicationFacts(Long petId, LocalDate start, LocalDate end) {
        return medicationRecordRepository.findByPetIdAndTimeRange(petId, start.atStartOfDay(), end.plusDays(1).atStartOfDay())
                .stream().map(item -> map("id", item.getId(), "scheduledAt", item.getScheduledAt(),
                        "takenAt", item.getTakenAt(), "dosage", item.getDosageValue(), "unit", item.getDosageUnit(),
                        "status", item.getStatus().getCode(), "notes", item.getNotes())).toList();
    }

    private List<Map<String, Object>> healthAlertFacts(Long petId) {
        return healthAlertRepository.findActiveByPetId(petId).stream()
                .map(item -> map("id", item.getId(), "ruleCode", item.getRuleCode(), "severity", item.getSeverity().getCode(),
                        "title", item.getTitle(), "message", item.getMessage(), "observedFrom", item.getObservedFrom(),
                        "observedTo", item.getObservedTo())).toList();
    }

    private Map<String, Object> expenseFacts(Long userId, Long petId, LocalDate start, LocalDate end) {
        var records = expenseApplicationService.listForUser(userId, petId, start, end);
        BigDecimal total = records.stream().map(item -> item.amount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        return map("count", records.size(), "totalAmount", total, "currency", "CNY", "records", records);
    }

    private List<Map<String, Object>> mediaFacts(Long userId, Long petId, LocalDate start, LocalDate end) {
        return mediaAssetRepository.search(userId, petId, null, start, end, 0,
                        properties.getReport().getMaxMediaCount()).stream()
                .map(item -> map("id", item.getId(), "mediaType", item.getMediaType().getCode(),
                        "fileName", item.getFileName(), "takenAt", item.getTakenAt(), "tags", item.getTags(),
                        "aiDescription", item.getAiDescription(), "status", item.getStatus().getCode()))
                .toList();
    }

    private Map<String, Object> map(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i + 1 < values.length; i += 2) if (values[i + 1] != null) result.put((String) values[i], values[i + 1]);
        return result;
    }

}
