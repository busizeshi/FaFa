package com.fafa.interfaces.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fafa.application.dto.event.*;
import com.fafa.application.service.EventRecordApplicationService;
import com.fafa.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 事件记录接口
 */
@RestController
@RequestMapping("/api/records/event")
@RequiredArgsConstructor
public class EventRecordController {
    
    private final EventRecordApplicationService eventRecordApplicationService;
    
    /**
     * 创建事件记录
     */
    @PostMapping
    public Result<Long> createEventRecord(@Validated @RequestBody CreateEventRecordRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        Long recordId = eventRecordApplicationService.createEventRecord(userId, request);
        return Result.success(recordId);
    }
    
    /**
     * 查询事件记录列表
     */
    @GetMapping
    public Result<List<EventRecordResponse>> listEventRecords(
            @RequestParam Long petId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        Long userId = StpUtil.getLoginIdAsLong();
        List<EventRecordResponse> records = eventRecordApplicationService.listEventRecords(
                userId, petId, eventType, startDate, endDate);
        return Result.success(records);
    }
    
    /**
     * 查询事件记录详情
     */
    @GetMapping("/{id}")
    public Result<EventRecordResponse> getEventRecord(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        EventRecordResponse record = eventRecordApplicationService.getEventRecord(userId, id);
        return Result.success(record);
    }
    
    /**
     * 更新事件记录
     */
    @PutMapping("/{id}")
    public Result<Void> updateEventRecord(
            @PathVariable Long id,
            @Validated @RequestBody UpdateEventRecordRequest request
    ) {
        Long userId = StpUtil.getLoginIdAsLong();
        eventRecordApplicationService.updateEventRecord(userId, id, request);
        return Result.success();
    }
    
    /**
     * 删除事件记录
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteEventRecord(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        eventRecordApplicationService.deleteEventRecord(userId, id);
        return Result.success();
    }
}
