package com.fafa.interfaces.controller;

import com.fafa.application.dto.PageDTO;
import com.fafa.application.dto.medical.*;
import com.fafa.application.service.MedicalSummaryApplicationService;
import com.fafa.application.service.MedicalVisitApplicationService;
import com.fafa.application.service.MedicationApplicationService;
import com.fafa.common.Result;
import com.fafa.interfaces.dto.medical.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** 就医档案、用药计划、实际用药和医疗时间线接口。 */
@RestController
@RequestMapping("/api/medical")
@RequiredArgsConstructor
@Tag(name = "医疗健康", description = "就医档案、用药计划、用药记录和就医摘要")
public class MedicalController {

    private final MedicalVisitApplicationService visitService;
    private final MedicationApplicationService medicationService;
    private final MedicalSummaryApplicationService summaryService;

    @PostMapping("/visits")
    @Operation(summary = "创建就医档案")
    public Result<MedicalVisitDTO> createVisit(@Valid @RequestBody CreateMedicalVisitRequest request) {
        return Result.ok(visitService.create(new MedicalVisitCommand(request.getPetId(), request.getVisitDate(),
                request.getHospitalName(), request.getDoctorName(), request.getChiefComplaint(),
                request.getDiagnosis(), request.getPrescription(), request.getTreatment(),
                request.getTotalCost(), request.getNotes())));
    }

    @GetMapping("/visits")
    @Operation(summary = "分页查询就医档案")
    public Result<PageDTO<MedicalVisitDTO>> listVisits(
                                                       @Parameter(description = "宠物ID", required = true, example = "1") @RequestParam Long petId,
                                                       @Parameter(description = "查询开始日期", required = true, example = "2026-08-01") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                       LocalDate startDate,
                                                       @Parameter(description = "查询结束日期", required = true, example = "2026-08-31") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                       LocalDate endDate,
                                                       @Parameter(description = "页码，从1开始", example = "1") @RequestParam(defaultValue = "1") int page,
                                                       @Parameter(description = "每页记录数", example = "20") @RequestParam(defaultValue = "20") int size) {
        return Result.ok(visitService.list(petId, startDate, endDate, page, size));
    }

    @GetMapping("/visits/{id}")
    @Operation(summary = "查询就医档案详情")
    public Result<MedicalVisitDTO> getVisit(
            @Parameter(description = "就医档案ID", required = true, example = "1") @PathVariable Long id) {
        return Result.ok(visitService.getById(id));
    }

    @PutMapping("/visits/{id}")
    @Operation(summary = "修改就医档案")
    public Result<MedicalVisitDTO> updateVisit(
                                               @Parameter(description = "就医档案ID", required = true, example = "1") @PathVariable Long id,
                                               @Valid @RequestBody UpdateMedicalVisitRequest request) {
        return Result.ok(visitService.update(id, new MedicalVisitCommand(null, null,
                request.getHospitalName(), request.getDoctorName(), request.getChiefComplaint(),
                request.getDiagnosis(), request.getPrescription(), request.getTreatment(),
                request.getTotalCost(), request.getNotes())));
    }

    @DeleteMapping("/visits/{id}")
    @Operation(summary = "删除就医档案")
    public Result<Void> deleteVisit(
            @Parameter(description = "就医档案ID", required = true, example = "1") @PathVariable Long id) {
        visitService.delete(id);
        return Result.ok();
    }

    @PostMapping("/visits/{id}/attachments")
    @Operation(summary = "关联就医素材")
    public Result<MedicalVisitDTO> attach(
                                          @Parameter(description = "就医档案ID", required = true, example = "1") @PathVariable Long id,
                                          @Valid @RequestBody AttachMedicalMediaRequest request) {
        return Result.ok(visitService.attach(id, new AttachMedicalMediaCommand(request.getMediaIds(),
                request.getAttachmentType())));
    }

    @DeleteMapping("/visits/{id}/attachments/{mediaId}")
    @Operation(summary = "解除就医素材关联")
    public Result<Void> detach(
                               @Parameter(description = "就医档案ID", required = true, example = "1") @PathVariable Long id,
                               @Parameter(description = "素材ID", required = true, example = "1001") @PathVariable Long mediaId) {
        visitService.detach(id, mediaId);
        return Result.ok();
    }

    @GetMapping("/timeline")
    @Operation(summary = "查询医疗时间线")
    public Result<List<MedicalTimelineItemDTO>> timeline(
                                                         @Parameter(description = "宠物ID", required = true, example = "1") @RequestParam Long petId,
                                                         @Parameter(description = "时间线开始日期", required = true, example = "2026-08-01") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                         LocalDate startDate,
                                                         @Parameter(description = "时间线结束日期", required = true, example = "2026-08-31") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                         LocalDate endDate) {
        return Result.ok(visitService.timeline(petId, startDate, endDate));
    }

    @PostMapping("/medication-plans")
    @Operation(summary = "创建用药计划")
    public Result<MedicationPlanDTO> createPlan(@Valid @RequestBody CreateMedicationPlanRequest request) {
        return Result.ok(medicationService.create(toCommand(request)));
    }

    @GetMapping("/medication-plans")
    @Operation(summary = "查询用药计划")
    public Result<List<MedicationPlanDTO>> listPlans(
                                                     @Parameter(description = "宠物ID", required = true, example = "1") @RequestParam Long petId,
                                                     @Parameter(description = "用药计划状态过滤，可选", example = "active") @RequestParam(required = false) String status) {
        return Result.ok(medicationService.list(petId, status));
    }

    @GetMapping("/medication-plans/{id}")
    @Operation(summary = "查询用药计划详情")
    public Result<MedicationPlanDTO> getPlan(
            @Parameter(description = "用药计划ID", required = true, example = "1") @PathVariable Long id) {
        return Result.ok(medicationService.getById(id));
    }

    @PutMapping("/medication-plans/{id}")
    @Operation(summary = "修改用药计划")
    public Result<MedicationPlanDTO> updatePlan(
                                                @Parameter(description = "用药计划ID", required = true, example = "1") @PathVariable Long id,
                                                @Valid @RequestBody UpdateMedicationPlanRequest request) {
        return Result.ok(medicationService.update(id, toCommand(request)));
    }

    @PostMapping("/medication-plans/{id}/pause")
    @Operation(summary = "暂停用药计划")
    public Result<MedicationPlanDTO> pausePlan(
            @Parameter(description = "用药计划ID", required = true, example = "1") @PathVariable Long id) {
        return Result.ok(medicationService.pause(id));
    }

    @PostMapping("/medication-plans/{id}/resume")
    @Operation(summary = "恢复用药计划")
    public Result<MedicationPlanDTO> resumePlan(
            @Parameter(description = "用药计划ID", required = true, example = "1") @PathVariable Long id) {
        return Result.ok(medicationService.resume(id));
    }

    @PostMapping("/medication-plans/{id}/complete")
    @Operation(summary = "结束用药计划")
    public Result<MedicationPlanDTO> completePlan(
            @Parameter(description = "用药计划ID", required = true, example = "1") @PathVariable Long id) {
        return Result.ok(medicationService.complete(id));
    }

    @DeleteMapping("/medication-plans/{id}")
    @Operation(summary = "删除用药计划")
    public Result<Void> deletePlan(
            @Parameter(description = "用药计划ID", required = true, example = "1") @PathVariable Long id) {
        medicationService.delete(id);
        return Result.ok();
    }

    @PostMapping("/medication-records")
    @Operation(summary = "保存实际用药记录")
    public Result<MedicationRecordDTO> saveMedicationRecord(
            @Valid @RequestBody CreateMedicationRecordRequest request) {
        return Result.ok(medicationService.saveRecord(new MedicationRecordCommand(request.getPetId(),
                request.getMedicationPlanId(), request.getScheduledAt(), request.getTakenAt(),
                request.getStatus(), request.getDosageValue(), request.getDosageUnit(), request.getNotes())));
    }

    @GetMapping("/medication-records")
    @Operation(summary = "查询实际用药记录")
    public Result<List<MedicationRecordDTO>> listMedicationRecords(
            @Parameter(description = "宠物ID", required = true, example = "1") @RequestParam Long petId,
            @Parameter(description = "查询开始日期", required = true, example = "2026-08-01") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "查询结束日期", required = true, example = "2026-08-31") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.ok(medicationService.records(petId, startDate, endDate));
    }

    @PostMapping("/summaries")
    @Operation(summary = "生成就诊摘要")
    public Result<MedicalSummaryDTO> summary(@Valid @RequestBody CreateMedicalSummaryRequest request) {
        return Result.ok(summaryService.generate(new MedicalSummaryCommand(request.getPetId(),
                request.getStartDate(), request.getEndDate(), request.isIncludeMedia())));
    }

    private MedicationPlanCommand toCommand(CreateMedicationPlanRequest request) {
        return new MedicationPlanCommand(request.getPetId(), request.getSourceVisitId(), request.getMedicationName(),
                request.getSpecification(), request.getPurpose(), request.getDosageValue(), request.getDosageUnit(),
                request.getFrequencyType(), request.getFrequencyValue(), request.getAdministrationTimes(),
                request.getMealRelation(), request.getStartDate(), request.getEndDate(), request.getNotes());
    }

    private MedicationPlanCommand toCommand(UpdateMedicationPlanRequest request) {
        return new MedicationPlanCommand(request.getPetId(), null, request.getMedicationName(),
                request.getSpecification(), request.getPurpose(), request.getDosageValue(), request.getDosageUnit(),
                request.getFrequencyType(), request.getFrequencyValue(), request.getAdministrationTimes(),
                request.getMealRelation(), request.getStartDate(), request.getEndDate(), request.getNotes());
    }
}
