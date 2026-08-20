package com.fafa.interfaces.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fafa.application.dto.excretion.*;
import com.fafa.application.service.ExcretionRecordApplicationService;
import com.fafa.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 排便记录接口
 */
@RestController
@RequestMapping("/api/records/excretion")
@RequiredArgsConstructor
public class ExcretionRecordController {
    
    private final ExcretionRecordApplicationService excretionRecordApplicationService;
    
    /**
     * 创建排便记录
     */
    @PostMapping
    public Result<Long> createExcretionRecord(@Validated @RequestBody CreateExcretionRecordRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        Long recordId = excretionRecordApplicationService.createExcretionRecord(userId, request);
        return Result.success(recordId);
    }
    
    /**
     * 查询排便记录列表
     */
    @GetMapping
    public Result<List<ExcretionRecordResponse>> listExcretionRecords(
            @RequestParam Long petId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        Long userId = StpUtil.getLoginIdAsLong();
        List<ExcretionRecordResponse> records = excretionRecordApplicationService.listExcretionRecords(userId, petId, startDate, endDate);
        return Result.success(records);
    }
    
    /**
     * 查询排便记录详情
     */
    @GetMapping("/{id}")
    public Result<ExcretionRecordResponse> getExcretionRecord(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        ExcretionRecordResponse record = excretionRecordApplicationService.getExcretionRecord(userId, id);
        return Result.success(record);
    }
    
    /**
     * 删除排便记录
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteExcretionRecord(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        excretionRecordApplicationService.deleteExcretionRecord(userId, id);
        return Result.success();
    }
    
    /**
     * 统计排便次数
     */
    @GetMapping("/statistics")
    public Result<ExcretionStatisticsResponse> getExcretionStatistics(
            @RequestParam Long petId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        Long userId = StpUtil.getLoginIdAsLong();
        ExcretionStatisticsResponse statistics = excretionRecordApplicationService.getExcretionStatistics(
                userId, petId, type, startDate, endDate);
        return Result.success(statistics);
    }
}
