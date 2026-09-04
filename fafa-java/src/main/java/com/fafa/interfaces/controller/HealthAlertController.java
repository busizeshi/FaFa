package com.fafa.interfaces.controller;

import com.fafa.application.dto.medical.HealthAlertDTO;
import com.fafa.application.service.HealthAlertApplicationService;
import com.fafa.common.Result;
import com.fafa.interfaces.dto.medical.HealthEvaluateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** 健康预警查询与状态处理接口。 */
@RestController
@RequestMapping("/api/health-alerts")
@RequiredArgsConstructor
@Tag(name = "健康预警", description = "基于每日状态和用药记录的规则预警")
public class HealthAlertController {

    private final HealthAlertApplicationService service;

    @GetMapping
    @Operation(summary = "查询健康预警")
    public Result<List<HealthAlertDTO>> list(
                                             @Parameter(description = "宠物ID", required = true, example = "1") @RequestParam Long petId,
                                             @Parameter(description = "预警状态过滤：open=未处理，acknowledged=已确认，resolved=已解决", example = "open") @RequestParam(required = false) String status,
                                             @Parameter(description = "最多返回的预警数量", example = "20") @RequestParam(defaultValue = "20") int limit) {
        return Result.ok(service.list(petId, status, limit));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询预警详情")
    public Result<HealthAlertDTO> get(
            @Parameter(description = "预警ID", required = true, example = "1") @PathVariable Long id) {
        return Result.ok(service.get(id));
    }

    @PostMapping("/evaluate")
    @Operation(summary = "立即评估健康预警")
    public Result<List<HealthAlertDTO>> evaluate(@Valid @RequestBody HealthEvaluateRequest request) {
        return Result.ok(service.evaluate(request.getPetId(), request.getAsOfDate()));
    }

    @PostMapping("/{id}/acknowledge")
    @Operation(summary = "确认预警")
    public Result<HealthAlertDTO> acknowledge(
            @Parameter(description = "预警ID", required = true, example = "1") @PathVariable Long id) {
        return Result.ok(service.acknowledge(id));
    }

    @PostMapping("/{id}/resolve")
    @Operation(summary = "解决预警")
    public Result<HealthAlertDTO> resolve(
            @Parameter(description = "预警ID", required = true, example = "1") @PathVariable Long id) {
        return Result.ok(service.resolve(id));
    }
}
