package com.fafa.interfaces.controller;

import com.fafa.application.dto.feeding.FeedingAnalysisDTO;
import com.fafa.application.service.FeedingAnalysisApplicationService;
import com.fafa.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** 动态喂养分析接口。 */
@RestController
@RequestMapping("/api/feeding-analysis")
@RequiredArgsConstructor
@Tag(name = "喂养分析", description = "喂食、体重和每日状态的事实统计")
public class FeedingAnalysisController {

    private final FeedingAnalysisApplicationService service;

    @GetMapping
    @Operation(summary = "查询动态喂养分析")
    public Result<FeedingAnalysisDTO> get(@RequestParam Long petId,
                                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                          LocalDate startDate,
                                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                          LocalDate endDate) {
        return Result.ok(service.analyze(petId, startDate, endDate));
    }
}
