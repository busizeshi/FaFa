package com.fafa.interfaces.controller;

import com.fafa.application.dto.record.FeedingRecordDTO;
import com.fafa.application.dto.record.SaveFeedingRecordCommand;
import com.fafa.application.service.FeedingRecordApplicationService;
import com.fafa.common.Result;
import com.fafa.interfaces.dto.record.SaveFeedingRequest;
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
 * 喂食记录接口控制器
 *
 * @author FaFa Team
 * @since 2.0
 */
@RestController
@RequestMapping("/api/feeding-records")
@RequiredArgsConstructor
@Tag(name = "喂食记录", description = "喂食事件流：一天多条，精确到分钟")
public class FeedingRecordController {

    private final FeedingRecordApplicationService feedingRecordApplicationService;

    @PostMapping
    @Operation(summary = "新增喂食记录")
    public Result<FeedingRecordDTO> create(@Valid @RequestBody SaveFeedingRequest request) {
        return Result.ok(feedingRecordApplicationService.create(toCommand(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改喂食记录")
    public Result<FeedingRecordDTO> update(
            @Parameter(description = "记录ID", required = true, example = "1") @PathVariable Long id,
            @Valid @RequestBody SaveFeedingRequest request
    ) {
        return Result.ok(feedingRecordApplicationService.update(id, toCommand(request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除喂食记录", description = "逻辑删除")
    public Result<Void> delete(@Parameter(description = "记录ID", required = true, example = "1") @PathVariable Long id) {
        feedingRecordApplicationService.delete(id);
        return Result.ok();
    }

    @GetMapping
    @Operation(summary = "当天喂食列表", description = "按时间升序")
    public Result<List<FeedingRecordDTO>> listByDate(
            @Parameter(description = "宠物ID", required = true, example = "1") @RequestParam Long petId,
            @Parameter(description = "日期", required = true, example = "2026-08-20") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return Result.ok(feedingRecordApplicationService.listByDate(petId, date));
    }

    @GetMapping("/range")
    @Operation(summary = "范围列表", description = "按时间升序，范围不超过366天")
    public Result<List<FeedingRecordDTO>> listByRange(
            @Parameter(description = "宠物ID", required = true, example = "1") @RequestParam Long petId,
            @Parameter(description = "开始日期", required = true, example = "2026-08-01") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期", required = true, example = "2026-08-31") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return Result.ok(feedingRecordApplicationService.listByRange(petId, startDate, endDate));
    }

    private SaveFeedingRecordCommand toCommand(SaveFeedingRequest request) {
        return new SaveFeedingRecordCommand(
                request.getPetId(),
                request.getFeedingTime(),
                request.getFoodType(),
                request.getAmount(),
                request.getUnit(),
                request.getFoodId(),
                request.getFeedingPlanId(),
                request.getFeedingPlanItemId(),
                request.getPlannedAt(),
                request.getFoodNameSnapshot(),
                request.getNotes()
        );
    }
}
