package com.fafa.interfaces.controller;

import com.fafa.application.dto.chat.*;
import com.fafa.application.service.AiQueryApplicationService;
import com.fafa.application.service.MediaApplicationService;
import com.fafa.application.dto.media.SemanticSearchCommand;
import com.fafa.application.service.HealthAlertApplicationService;
import com.fafa.application.service.MedicalVisitApplicationService;
import com.fafa.application.service.MedicationApplicationService;
import com.fafa.application.service.ReportApplicationService;
import com.fafa.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/** Python Agent 使用的 Java 内部只读数据接口。 */
@RestController
@RequestMapping("/internal/ai")
@RequiredArgsConstructor
@Tag(name = "AI 内部工具", description = "仅供 fafa-python Agent 使用")
public class InternalAiQueryController {

    private final AiQueryApplicationService aiQueryApplicationService;
    private final MediaApplicationService mediaApplicationService;
    private final MedicalVisitApplicationService medicalVisitApplicationService;
    private final MedicationApplicationService medicationApplicationService;
    private final HealthAlertApplicationService healthAlertApplicationService;
    private final ReportApplicationService reportApplicationService;

    @GetMapping("/pets/{petId}/profile")
    @Operation(summary = "AI 查询宠物资料")
    public AiPetProfileDTO getProfile(@PathVariable Long petId, @RequestParam Long userId) {
        return aiQueryApplicationService.getPetProfile(userId, petId);
    }

    @GetMapping("/pets/{petId}/daily-status")
    @Operation(summary = "AI 查询每日状态")
    public AiDailyStatusDTO getDailyStatus(@PathVariable Long petId, @RequestParam Long userId,
                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                           LocalDate startDate,
                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                           LocalDate endDate) {
        return aiQueryApplicationService.getDailyStatus(userId, petId, startDate, endDate);
    }

    @GetMapping("/pets/{petId}/weight")
    @Operation(summary = "AI 查询体重历史")
    public AiWeightHistoryDTO getWeight(@PathVariable Long petId, @RequestParam Long userId,
                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                        LocalDate startDate,
                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                        LocalDate endDate) {
        return aiQueryApplicationService.getWeightHistory(userId, petId, startDate, endDate);
    }

    @GetMapping("/pets/{petId}/feeding")
    @Operation(summary = "AI 查询喂食历史")
    public AiFeedingHistoryDTO getFeeding(@PathVariable Long petId, @RequestParam Long userId,
                                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                          LocalDate startDate,
                                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                          LocalDate endDate) {
        return aiQueryApplicationService.getFeedingHistory(userId, petId, startDate, endDate);
    }

    @GetMapping("/pets/{petId}/events")
    @Operation(summary = "AI 查询成长事件")
    public AiEventHistoryDTO getEvents(@PathVariable Long petId, @RequestParam Long userId,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                       LocalDate startDate,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                       LocalDate endDate) {
        return aiQueryApplicationService.getEventHistory(userId, petId, startDate, endDate);
    }

    @GetMapping("/pets/{petId}/reminders/history")
    @Operation(summary = "AI 查询提醒完成历史")
    public AiReminderHistoryDTO getReminderHistory(@PathVariable Long petId, @RequestParam Long userId,
                                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                   LocalDate startDate,
                                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                   LocalDate endDate) {
        return aiQueryApplicationService.getReminderHistory(userId, petId, startDate, endDate);
    }

    @PostMapping("/media/search")
    @Operation(summary = "AI 查询素材")
    public AiMediaSearchDTO searchMedia(@Valid @RequestBody AiMediaSearchRequest request) {
        var result = mediaApplicationService.semanticSearchForUser(request.userId(),
                new SemanticSearchCommand(request.query(), request.petId(), request.startDate(),
                        request.endDate(), request.limit()));
        return new AiMediaSearchDTO(result.media(), true, "ai_search", null);
    }

    @GetMapping("/media/{mediaId}")
    @Operation(summary = "AI 查询素材详情")
    public com.fafa.application.dto.media.MediaDTO getMedia(
            @PathVariable Long mediaId, @RequestParam Long userId) {
        return mediaApplicationService.getByIdForUser(userId, mediaId);
    }

    @GetMapping("/pets/{petId}/medical-visits")
    @Operation(summary = "AI 查询就医档案")
    public Map<String, Object> getMedicalVisits(@PathVariable Long petId, @RequestParam Long userId,
                                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                 LocalDate startDate,
                                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                 LocalDate endDate) {
        return Map.of("records", medicalVisitApplicationService.listForUser(userId, petId, startDate, endDate));
    }

    @GetMapping("/pets/{petId}/medication-plans")
    @Operation(summary = "AI 查询用药计划")
    public Map<String, Object> getMedicationPlans(@PathVariable Long petId, @RequestParam Long userId,
                                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                   LocalDate startDate,
                                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                   LocalDate endDate) {
        return Map.of("records", medicationApplicationService.listForUser(userId, petId, null),
                "executions", medicationApplicationService.recordsForUser(userId, petId, startDate, endDate));
    }

    @GetMapping("/pets/{petId}/expenses")
    @Operation(summary = "AI 查询开支记录")
    public Map<String, Object> getExpenses(@PathVariable Long petId, @RequestParam Long userId,
                                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                            LocalDate startDate,
                                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                            LocalDate endDate) {
        return aiQueryApplicationService.getExpenses(userId, petId, startDate, endDate);
    }

    @GetMapping("/pets/{petId}/expense-statistics")
    @Operation(summary = "AI 查询开支统计")
    public com.fafa.application.dto.expense.ExpenseStatisticsDTO getExpenseStatistics(
            @PathVariable Long petId, @RequestParam Long userId,
            @RequestParam(defaultValue = "year") String period,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate anchorDate) {
        return aiQueryApplicationService.getExpenseStatistics(userId, petId, period, anchorDate);
    }

    @GetMapping("/pets/{petId}/medication-records")
    @Operation(summary = "AI 查询实际用药记录")
    public Map<String, Object> getMedicationRecords(@PathVariable Long petId, @RequestParam Long userId,
                                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                     LocalDate startDate,
                                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                     LocalDate endDate) {
        return Map.of("records", medicationApplicationService.recordsForUser(userId, petId, startDate, endDate));
    }

    @GetMapping("/pets/{petId}/health-alerts")
    @Operation(summary = "AI 查询健康预警")
    public Map<String, Object> getHealthAlerts(@PathVariable Long petId, @RequestParam Long userId,
                                               @RequestParam(required = false) String status) {
        return Map.of("records", healthAlertApplicationService.listForUser(userId, petId, status, 50));
    }

    @GetMapping("/pets/{petId}/foods")
    @Operation(summary = "AI 查询食品卡")
    public Map<String, Object> getFoods(@PathVariable Long petId, @RequestParam Long userId,
                                        @RequestParam(required = false) String status) {
        return aiQueryApplicationService.getFoodInfo(userId, petId, status);
    }

    @GetMapping("/pets/{petId}/feeding-plans")
    @Operation(summary = "AI 查询喂食计划")
    public Map<String, Object> getFeedingPlans(@PathVariable Long petId, @RequestParam Long userId,
                                               @RequestParam(required = false) String status) {
        return aiQueryApplicationService.getFeedingPlans(userId, petId, status);
    }

    @GetMapping("/pets/{petId}/reports")
    @Operation(summary = "AI 查询宠物周期报告")
    public Map<String, Object> getReports(@PathVariable Long petId, @RequestParam Long userId,
                                          @RequestParam(required = false) String periodType,
                                          @RequestParam(defaultValue = "5") int limit) {
        return Map.of("records", reportApplicationService.readForAgent(userId, petId, periodType, limit));
    }
}
