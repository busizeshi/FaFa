package com.fafa.interfaces.controller;

import com.fafa.application.dto.record.DailyStatusDTO;
import com.fafa.application.dto.record.DimensionConfigDTO;
import com.fafa.application.dto.record.SaveDailyStatusCommand;
import com.fafa.application.service.DailyRecordApplicationService;
import com.fafa.common.Result;
import com.fafa.interfaces.dto.record.SaveDailyStatusRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 每日打卡接口控制器
 *
 * @author FaFa Team
 * @since 2.0
 */
@RestController
@RequestMapping("/api/daily-records")
@RequiredArgsConstructor
@Tag(name = "每日打卡", description = "每日状态记录：upsert、一切正常、同上次、维度配置")
public class DailyRecordController {

    private final DailyRecordApplicationService dailyRecordApplicationService;

    @GetMapping("/dimensions")
    @Operation(summary = "物种维度配置", description = "打卡页渲染用：维度顺序/必填/提示/选项")
    public Result<List<DimensionConfigDTO>> getDimensions(
            @Parameter(description = "宠物类型: cat/dog/other，缺省按 other") @RequestParam(required = false) String petType
    ) {
        return Result.ok(dailyRecordApplicationService.getDimensionConfigs(petType));
    }

    @PostMapping
    @Operation(summary = "保存每日状态", description = "upsert 语义，同日重复提交覆盖")
    public Result<DailyStatusDTO> saveStatus(@Valid @RequestBody SaveDailyStatusRequest request) {
        SaveDailyStatusCommand command = new SaveDailyStatusCommand(
                request.getPetId(),
                request.getRecordDate(),
                request.getAppetite(),
                request.getEnergy(),
                request.getWaterIntake(),
                request.getDefecation(),
                request.getVomiting(),
                request.getUrination(),
                request.getAppetiteAmount(),
                request.getWaterAmount(),
                request.getNotes()
        );
        return Result.ok(dailyRecordApplicationService.upsertStatus(command));
    }

    @PostMapping("/record-normal")
    @Operation(summary = "一键记录一切正常", description = "按物种填正常档，1 次点击完成")
    public Result<DailyStatusDTO> recordNormal(
            @Parameter(description = "宠物ID") @RequestParam Long petId,
            @Parameter(description = "记录日期") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return Result.ok(dailyRecordApplicationService.recordNormal(petId, date));
    }

    @GetMapping("/latest")
    @Operation(summary = "最近一条记录", description = "before 可选，「同上次」预览回填用；未记录返回 data=null")
    public Result<DailyStatusDTO> getLatest(
            @Parameter(description = "宠物ID") @RequestParam Long petId,
            @Parameter(description = "严格早于该日期（可选，缺省含今天）") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate before
    ) {
        return Result.ok(dailyRecordApplicationService.getLatest(petId, before).orElse(null));
    }

    @PostMapping("/copy-last")
    @Operation(summary = "一键同上次", description = "复制严格早于目标日期的最近一条，不复制备注")
    public Result<DailyStatusDTO> copyLast(
            @Parameter(description = "宠物ID") @RequestParam Long petId,
            @Parameter(description = "目标日期") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return Result.ok(dailyRecordApplicationService.copyFromLast(petId, date));
    }

    @GetMapping
    @Operation(summary = "查询某天记录", description = "未记录返回 data=null，前端显示灰格")
    public Result<DailyStatusDTO> getByDate(
            @Parameter(description = "宠物ID") @RequestParam Long petId,
            @Parameter(description = "记录日期") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return Result.ok(dailyRecordApplicationService.getByDate(petId, date).orElse(null));
    }

    @GetMapping("/list")
    @Operation(summary = "范围列表", description = "按日期升序，范围不超过366天")
    public Result<List<DailyStatusDTO>> listByRange(
            @Parameter(description = "宠物ID") @RequestParam Long petId,
            @Parameter(description = "开始日期") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return Result.ok(dailyRecordApplicationService.listByRange(petId, startDate, endDate));
    }

    @DeleteMapping
    @Operation(summary = "删除某天记录", description = "物理删除，当天回到未记录状态")
    public Result<Void> delete(
            @Parameter(description = "宠物ID") @RequestParam Long petId,
            @Parameter(description = "记录日期") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        dailyRecordApplicationService.delete(petId, date);
        return Result.ok();
    }
}
