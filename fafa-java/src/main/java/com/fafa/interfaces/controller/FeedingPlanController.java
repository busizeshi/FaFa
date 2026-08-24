package com.fafa.interfaces.controller;

import com.fafa.application.dto.feeding.FeedingChecklistDTO;
import com.fafa.application.dto.feeding.FeedingPlanDTO;
import com.fafa.application.dto.feeding.FeedingRecommendationDTO;
import com.fafa.application.service.FeedingPlanApplicationService;
import com.fafa.common.Result;
import com.fafa.interfaces.dto.feeding.FeedingRecommendationRequest;
import com.fafa.interfaces.dto.feeding.SaveFeedingPlanRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** 喂食计划接口。 */
@RestController
@RequestMapping("/api/feeding-plans")
@RequiredArgsConstructor
@Tag(name = "喂食计划", description = "建议预览、计划确认和今日清单")
public class FeedingPlanController {

    private final FeedingPlanApplicationService service;

    @PostMapping("/recommendations")
    @Operation(summary = "生成喂食建议预览")
    public Result<FeedingRecommendationDTO> recommend(@Valid @RequestBody FeedingRecommendationRequest request) {
        return Result.ok(service.recommend(request));
    }

    @PostMapping
    @Operation(summary = "保存喂食计划")
    public Result<FeedingPlanDTO> create(@Valid @RequestBody SaveFeedingPlanRequest request) {
        return Result.ok(service.create(request));
    }

    @GetMapping
    @Operation(summary = "查询喂食计划列表")
    public Result<List<FeedingPlanDTO>> list(@RequestParam Long petId,
                                             @RequestParam(required = false) String status) {
        return Result.ok(service.list(petId, status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询喂食计划详情")
    public Result<FeedingPlanDTO> get(@PathVariable Long id) {
        return Result.ok(service.get(id));
    }

    @GetMapping("/{id}/today")
    @Operation(summary = "查询指定日期喂食清单")
    public Result<FeedingChecklistDTO> today(@PathVariable Long id,
                                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                             LocalDate date) {
        return Result.ok(service.today(id, date));
    }

    @PostMapping("/{id}/pause")
    @Operation(summary = "暂停喂食计划")
    public Result<FeedingPlanDTO> pause(@PathVariable Long id) {
        return Result.ok(service.pause(id));
    }

    @PostMapping("/{id}/resume")
    @Operation(summary = "恢复喂食计划")
    public Result<FeedingPlanDTO> resume(@PathVariable Long id) {
        return Result.ok(service.resume(id));
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "完成喂食计划")
    public Result<FeedingPlanDTO> complete(@PathVariable Long id) {
        return Result.ok(service.complete(id));
    }
}
