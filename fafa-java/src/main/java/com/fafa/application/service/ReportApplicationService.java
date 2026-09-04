package com.fafa.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fafa.application.dto.chat.AiReportQueryDTO;
import com.fafa.application.dto.report.*;
import com.fafa.application.port.ReportGenerationGateway;
import com.fafa.common.BusinessException;
import com.fafa.common.ErrorCode;
import com.fafa.domain.model.pet.Pet;
import com.fafa.domain.model.report.PetReport;
import com.fafa.domain.model.report.ReportPeriod;
import com.fafa.domain.model.report.ReportPeriodType;
import com.fafa.domain.repository.PetRepository;
import com.fafa.domain.repository.PetReportRepository;
import com.fafa.infrastructure.config.FaFaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportApplicationService {
    private final PetRepository petRepository;
    private final PetReportRepository reportRepository;
    private final ReportFactAssembler factAssembler;
    private final ReportGenerationGateway generationGateway;
    private final ReportContentValidator contentValidator;
    private final QuotaApplicationService quotaApplicationService;
    private final FaFaProperties properties;
    private final ObjectMapper objectMapper;

    public ReportDTO generate(GenerateReportCommand command) {
        Pet pet = ownedPet(command.userId(), command.petId());
        ReportPeriod period = resolvePeriod(command.periodType(), command.anchorDate());
        PetReport report = reportRepository.findByPeriod(command.userId(), command.petId(), period).orElse(null);
        if (report != null && !command.force() && List.of("completed", "degraded").contains(report.getStatus())) {
            return toDTO(report);
        }
        if (report != null && "generating".equals(report.getStatus())) {
            throw new BusinessException(ErrorCode.REPORT_GENERATION_IN_PROGRESS);
        }

        String factJson = factsJson(command.userId(), command.petId(), period);
        LocalDateTime now = LocalDateTime.now();
        if (report == null) {
            report = PetReport.create(command.userId(), pet.getId(), period,
                    dataStatus(factJson), properties.getReport().getSnapshotVersion(),
                    properties.getReport().getCalculationVersion(), factJson,
                    trigger(command.trigger()), now);
            report.markGenerating(trigger(command.trigger()), now);
            report = reportRepository.save(report);
        } else {
            report.markGenerating(trigger(command.trigger()), now);
            report = reportRepository.save(report);
        }
        quotaApplicationService.tryAcquireReportContent(command.userId());
        try {
            ReportGenerationResponse generated = generationGateway.generate(new ReportGenerationRequest(
                    report.getId(), pet.getId(), period.type().getCode(), period.startDate(), period.endDate(),
                    factJson, properties.getReport().getPromptVersion()));
            contentValidator.validate(generated.content());
            report.markCompleted(write(generated.content()), safeMode(generated.mode()), generated.modelName(),
                    generated.promptVersion(), LocalDateTime.now());
        } catch (BusinessException ex) {
            quotaApplicationService.releaseReportContent(command.userId());
            ReportContentDTO fallback = deterministicContent(pet, period, factJson);
            report.markDegraded(write(fallback), "AI_GENERATION_FAILED", "AI 暂不可用，已生成事实摘要", LocalDateTime.now());
            log.warn("报告 AI 生成失败，降级为确定性摘要: reportId={}, code={}", report.getId(), ex.getErrorCode());
        } catch (RuntimeException ex) {
            quotaApplicationService.releaseReportContent(command.userId());
            ReportContentDTO fallback = deterministicContent(pet, period, factJson);
            report.markDegraded(write(fallback), "AI_GENERATION_FAILED", "AI 暂不可用，已生成事实摘要", LocalDateTime.now());
            log.warn("报告 AI 生成异常，降级为确定性摘要: reportId={}", report.getId(), ex);
        }
        return toDTO(reportRepository.save(report));
    }

    public List<ReportSummaryDTO> list(Long userId, Long petId, int page, int size) {
        ownedPet(userId, petId);
        int safeSize = Math.min(Math.max(size, 1), 50);
        int offset = Math.max(page, 1) - 1;
        return reportRepository.findByUserIdAndPetId(userId, petId, offset * safeSize, safeSize)
                .stream().map(this::toSummary).toList();
    }

    public ReportDTO get(Long userId, Long id) {
        return toDTO(reportRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REPORT_NOT_FOUND)));
    }

    public void delete(Long userId, Long id) {
        PetReport report = reportRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REPORT_NOT_FOUND));
        report.delete();
        reportRepository.save(report);
    }

    public List<AiReportQueryDTO.ReportFactDTO> readForAgent(Long userId, Long petId, String periodType, int limit) {
        ownedPet(userId, petId);
        int safeLimit = Math.min(Math.max(limit, 1), 12);
        return reportRepository.findByUserIdAndPetId(userId, petId, 0, safeLimit).stream()
                .filter(report -> periodType == null || periodType.equals(report.getPeriodType().getCode()))
                .map(report -> new AiReportQueryDTO.ReportFactDTO(report.getId(), report.getPeriodType().getCode(),
                        report.getPeriodStart(), report.getPeriodEnd(), report.getStatus(), parse(report.getContentJson())))
                .toList();
    }

    private ReportPeriod resolvePeriod(String type, LocalDate anchorDate) {
        try {
            ReportPeriod period = ReportPeriod.resolve(ReportPeriodType.fromCode(type), anchorDate);
            if (period.startDate().isAfter(LocalDate.now())
                    || ChronoUnit.DAYS.between(period.startDate(), period.endDate()) > properties.getReport().getMaxRangeDays()) {
                throw new BusinessException(ErrorCode.REPORT_PERIOD_INVALID);
            }
            return period;
        } catch (BusinessException ex) { throw ex; }
        catch (RuntimeException ex) { throw new BusinessException(ErrorCode.REPORT_PERIOD_INVALID, ex.getMessage()); }
    }

    private Pet ownedPet(Long userId, Long petId) {
        return petRepository.findById(petId).filter(item -> userId.equals(item.getUserId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.AI_CHAT_PET_FORBIDDEN));
    }

    private String factsJson(Long userId, Long petId, ReportPeriod period) {
        try { return objectMapper.writeValueAsString(factAssembler.assemble(userId, petId, period)); }
        catch (JsonProcessingException | IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.REPORT_DATA_UNAVAILABLE, "报告事实数据组装失败");
        }
    }

    private String dataStatus(String factsJson) { return factsJson.contains("dailyStatuses") ? "complete" : "partial"; }
    private String trigger(String value) { return value == null || value.isBlank() ? "manual" : value; }
    private String safeMode(String mode) { return mode == null || mode.isBlank() ? "ai" : mode; }

    private ReportContentDTO deterministicContent(Pet pet, ReportPeriod period, String factsJson) {
        JsonNode facts = parse(factsJson);
        int daily = facts.path("dailyStatuses").size();
        int feeding = facts.path("feeding").size();
        int events = facts.path("events").size();
        String name = pet.getName() == null ? "宠物" : pet.getName();
        return new ReportContentDTO(name + "的" + period.type().getCode() + "报告",
                "本周期已记录每日状态 " + daily + " 天、喂食 " + feeding + " 次、事件 " + events + " 条。",
                List.of("已纳入事实快照的数据均可在报告详情中回看"),
                List.of("继续保持规律记录，便于下个周期形成可比较趋势"), List.of(), List.of(), List.of());
    }

    private String write(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException ex) { throw new BusinessException(ErrorCode.REPORT_CONTENT_INVALID); }
    }
    private JsonNode parse(String value) {
        try { return value == null ? objectMapper.createObjectNode() : objectMapper.readTree(value); }
        catch (JsonProcessingException ex) { return objectMapper.createObjectNode(); }
    }
    private ReportDTO toDTO(PetReport item) {
        return new ReportDTO(item.getId(), item.getPetId(), item.getPeriodType().getCode(), item.getPeriodStart(),
                item.getPeriodEnd(), item.getDataStatus(), item.getStatus(), item.getSnapshotVersion(),
                item.getCalculationVersion(), parse(item.getFactSnapshotJson()), parse(item.getContentJson()),
                item.getContentMode(), item.getModelName(), item.getPromptVersion(), item.getGenerationTrigger(),
                item.getGenerationVersion(), item.getLastErrorCode(), item.getLastErrorMessage(), item.getGeneratedAt(),
                item.getCreatedAt(), item.getUpdatedAt());
    }
    private ReportSummaryDTO toSummary(PetReport item) {
        return new ReportSummaryDTO(item.getId(), item.getPetId(), item.getPeriodType().getCode(), item.getPeriodStart(),
                item.getPeriodEnd(), item.getDataStatus(), item.getStatus(), item.getContentMode(),
                item.getGenerationVersion(), item.getGeneratedAt());
    }
}
