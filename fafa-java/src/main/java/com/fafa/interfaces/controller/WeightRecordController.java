package com.fafa.interfaces.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fafa.application.dto.weight.*;
import com.fafa.application.service.WeightRecordApplicationService;
import com.fafa.common.response.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 体重记录接口
 */
@RestController
@RequestMapping("/api/records/weight")
@RequiredArgsConstructor
public class WeightRecordController {
    
    private final WeightRecordApplicationService weightRecordApplicationService;
    
    /**
     * 创建体重记录
     */
    @PostMapping
    public Result<Long> createWeightRecord(@Validated @RequestBody CreateWeightRecordRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        Long recordId = weightRecordApplicationService.createWeightRecord(userId, request);
        return Result.success(recordId);
    }
    
    /**
     * 查询体重记录列表
     */
    @GetMapping
    public Result<List<WeightRecordResponse>> listWeightRecords(
            @RequestParam Long petId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        Long userId = StpUtil.getLoginIdAsLong();
        List<WeightRecordResponse> records = weightRecordApplicationService.listWeightRecords(userId, petId, startDate, endDate);
        return Result.success(records);
    }
    
    /**
     * 查询体重记录详情
     */
    @GetMapping("/{id}")
    public Result<WeightRecordResponse> getWeightRecord(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        WeightRecordResponse record = weightRecordApplicationService.getWeightRecord(userId, id);
        return Result.success(record);
    }
    
    /**
     * 更新体重记录
     */
    @PutMapping("/{id}")
    public Result<Void> updateWeightRecord(
            @PathVariable Long id,
            @Validated @RequestBody UpdateWeightRecordRequest request
    ) {
        Long userId = StpUtil.getLoginIdAsLong();
        weightRecordApplicationService.updateWeightRecord(userId, id, request);
        return Result.success();
    }
    
    /**
     * 删除体重记录
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteWeightRecord(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        weightRecordApplicationService.deleteWeightRecord(userId, id);
        return Result.success();
    }
    
    /**
     * 获取体重趋势分析
     */
    @GetMapping("/trend")
    public Result<WeightTrendResponse> getWeightTrend(
            @RequestParam Long petId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        Long userId = StpUtil.getLoginIdAsLong();
        WeightTrendResponse trend = weightRecordApplicationService.getWeightTrend(userId, petId, startDate, endDate);
        return Result.success(trend);
    }
}
