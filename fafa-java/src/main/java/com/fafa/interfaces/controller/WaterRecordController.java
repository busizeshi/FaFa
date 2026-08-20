package com.fafa.interfaces.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fafa.application.dto.water.*;
import com.fafa.application.service.WaterRecordApplicationService;
import com.fafa.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 饮水记录接口
 */
@RestController
@RequestMapping("/api/records/water")
@RequiredArgsConstructor
public class WaterRecordController {
    
    private final WaterRecordApplicationService waterRecordApplicationService;
    
    /**
     * 创建饮水记录
     */
    @PostMapping
    public Result<Long> createWaterRecord(@Validated @RequestBody CreateWaterRecordRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        Long recordId = waterRecordApplicationService.createWaterRecord(userId, request);
        return Result.success(recordId);
    }
    
    /**
     * 查询饮水记录列表
     */
    @GetMapping
    public Result<List<WaterRecordResponse>> listWaterRecords(
            @RequestParam Long petId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        Long userId = StpUtil.getLoginIdAsLong();
        List<WaterRecordResponse> records = waterRecordApplicationService.listWaterRecords(userId, petId, startDate, endDate);
        return Result.success(records);
    }
    
    /**
     * 查询饮水记录详情
     */
    @GetMapping("/{id}")
    public Result<WaterRecordResponse> getWaterRecord(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        WaterRecordResponse record = waterRecordApplicationService.getWaterRecord(userId, id);
        return Result.success(record);
    }
    
    /**
     * 删除饮水记录
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteWaterRecord(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        waterRecordApplicationService.deleteWaterRecord(userId, id);
        return Result.success();
    }
    
    /**
     * 统计饮水量
     */
    @GetMapping("/statistics")
    public Result<WaterStatisticsResponse> getWaterStatistics(
            @RequestParam Long petId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        Long userId = StpUtil.getLoginIdAsLong();
        WaterStatisticsResponse statistics = waterRecordApplicationService.getWaterStatistics(userId, petId, startDate, endDate);
        return Result.success(statistics);
    }
}
