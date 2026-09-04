package com.fafa.interfaces.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fafa.application.dto.report.GenerateReportCommand;
import com.fafa.application.dto.report.ReportDTO;
import com.fafa.application.dto.report.ReportSummaryDTO;
import com.fafa.application.service.ReportApplicationService;
import com.fafa.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "宠物报告", description = "周期报告生成、查询和删除")
public class ReportController {
    private final ReportApplicationService reportService;

    @PostMapping("/generate")
    @Operation(summary = "生成周期报告")
    public Result<ReportDTO> generate(@RequestBody GenerateBody body) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(reportService.generate(new GenerateReportCommand(userId, body.petId, body.periodType,
                body.anchorDate, Boolean.TRUE.equals(body.force), "manual")));
    }

    @GetMapping
    @Operation(summary = "查询报告列表")
    public Result<List<ReportSummaryDTO>> list(
                                               @Parameter(description = "宠物ID", required = true, example = "1") @RequestParam Long petId,
                                               @Parameter(description = "页码，从1开始", example = "1") @RequestParam(defaultValue = "1") int page,
                                               @Parameter(description = "每页报告数", example = "20") @RequestParam(defaultValue = "20") int size) {
        return Result.ok(reportService.list(StpUtil.getLoginIdAsLong(), petId, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询报告详情")
    public Result<ReportDTO> get(
            @Parameter(description = "报告ID", required = true, example = "1") @PathVariable Long id) {
        return Result.ok(reportService.get(StpUtil.getLoginIdAsLong(), id));
    }

    @PostMapping("/{id}/regenerate")
    @Operation(summary = "重新生成报告")
    public Result<ReportDTO> regenerate(
            @Parameter(description = "报告ID", required = true, example = "1") @PathVariable Long id) {
        ReportDTO old = reportService.get(StpUtil.getLoginIdAsLong(), id);
        return Result.ok(reportService.generate(new GenerateReportCommand(StpUtil.getLoginIdAsLong(), old.petId(),
                old.periodType(), old.periodStart(), true, "regenerate")));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除报告")
    public Result<Void> delete(
            @Parameter(description = "报告ID", required = true, example = "1") @PathVariable Long id) {
        reportService.delete(StpUtil.getLoginIdAsLong(), id);
        return Result.ok();
    }

    @Data
    @io.swagger.v3.oas.annotations.media.Schema(description = "生成周期报告请求")
    public static class GenerateBody {
        @io.swagger.v3.oas.annotations.media.Schema(description = "所属宠物ID", example = "1")
        private Long petId;
        @io.swagger.v3.oas.annotations.media.Schema(description = "报告周期类型", example = "month")
        private String periodType = "month";
        @io.swagger.v3.oas.annotations.media.Schema(description = "报告周期锚点日期；不传时由服务端推断", example = "2026-08-31")
        private LocalDate anchorDate;
        @io.swagger.v3.oas.annotations.media.Schema(description = "是否强制重新生成", example = "false")
        private Boolean force = false;
    }
}
