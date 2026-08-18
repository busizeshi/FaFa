package com.fafa.interfaces.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fafa.application.service.FeedRecordApplicationService;
import com.fafa.common.result.Result;
import com.fafa.domain.model.record.FeedRecord;
import com.fafa.interfaces.dto.request.CreateFeedRecordRequest;
import com.fafa.interfaces.dto.request.UpdateFeedRecordRequest;
import com.fafa.interfaces.dto.response.FeedRecordResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 喂食记录控制器
 * 
 * @author FaFa Team
 * @since 1.0
 */
@Slf4j
@Tag(name = "喂食记录", description = "喂食记录相关接口")
@RestController
@RequestMapping("/records/feeding")
@RequiredArgsConstructor
public class FeedRecordController {
    
    private final FeedRecordApplicationService feedRecordApplicationService;
    
    @Operation(summary = "创建喂食记录", description = "完整的喂食记录")
    @PostMapping
    public Result<Long> createFeedRecord(@Valid @RequestBody CreateFeedRecordRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        Long recordId = feedRecordApplicationService.createFeedRecord(userId, request);
        return Result.success(recordId);
    }
    
    @Operation(summary = "快捷喂食", description = "使用上次记录快速记录")
    @PostMapping("/quick")
    public Result<Long> quickFeed(@RequestParam Long petId) {
        Long userId = StpUtil.getLoginIdAsLong();
        Long recordId = feedRecordApplicationService.quickFeed(userId, petId);
        return Result.success(recordId);
    }
    
    @Operation(summary = "喂食记录列表", description = "查询喂食记录列表")
    @GetMapping
    public Result<Map<String, Object>> listFeedRecords(
            @Parameter(description = "宠物ID", required = true) @RequestParam Long petId,
            @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size) {
        
        Long userId = StpUtil.getLoginIdAsLong();
        
        List<FeedRecord> records = feedRecordApplicationService.listFeedRecords(userId, petId, startDate, endDate, page, size);
        int total = feedRecordApplicationService.countFeedRecords(userId, petId, startDate, endDate);
        
        List<FeedRecordResponse> responses = records.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        
        Map<String, Object> result = new HashMap<>();
        result.put("records", responses);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        
        return Result.success(result);
    }
    
    @Operation(summary = "喂食记录详情", description = "查询单条记录")
    @GetMapping("/{id}")
    public Result<FeedRecordResponse> getFeedRecord(
            @Parameter(description = "记录ID", required = true) @PathVariable Long id) {
        
        Long userId = StpUtil.getLoginIdAsLong();
        FeedRecord feedRecord = feedRecordApplicationService.getFeedRecord(userId, id);
        FeedRecordResponse response = convertToResponse(feedRecord);
        
        return Result.success(response);
    }
    
    @Operation(summary = "更新喂食记录", description = "修改喂食记录")
    @PutMapping("/{id}")
    public Result<Void> updateFeedRecord(
            @Parameter(description = "记录ID", required = true) @PathVariable Long id,
            @Valid @RequestBody UpdateFeedRecordRequest request) {
        
        Long userId = StpUtil.getLoginIdAsLong();
        feedRecordApplicationService.updateFeedRecord(userId, id, request);
        
        return Result.success();
    }
    
    @Operation(summary = "删除喂食记录", description = "删除指定记录")
    @DeleteMapping("/{id}")
    public Result<Void> deleteFeedRecord(
            @Parameter(description = "记录ID", required = true) @PathVariable Long id) {
        
        Long userId = StpUtil.getLoginIdAsLong();
        feedRecordApplicationService.deleteFeedRecord(userId, id);
        
        return Result.success();
    }
    
    /**
     * 转换为响应对象
     */
    private FeedRecordResponse convertToResponse(FeedRecord feedRecord) {
        return FeedRecordResponse.builder()
                .id(feedRecord.getRecordId().getValue())
                .petId(feedRecord.getPetId())
                .feedTime(feedRecord.getFeedTime())
                .foodName(feedRecord.getFoodName())
                .foodType(feedRecord.getFoodType())
                .amount(feedRecord.getAmount())
                .unit(feedRecord.getUnit())
                .brand(feedRecord.getBrand())
                .remarks(feedRecord.getRemarks())
                .createdAt(feedRecord.getCreatedAt())
                .build();
    }
}
