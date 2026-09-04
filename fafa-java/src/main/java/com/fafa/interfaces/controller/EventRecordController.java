package com.fafa.interfaces.controller;

import com.fafa.application.dto.event.CreateEventCommand;
import com.fafa.application.dto.event.EventRecordDTO;
import com.fafa.application.dto.event.UpdateEventCommand;
import com.fafa.application.service.EventRecordApplicationService;
import com.fafa.common.Result;
import com.fafa.interfaces.dto.event.CreateEventRequest;
import com.fafa.interfaces.dto.event.UpdateEventRequest;
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
 * 事件记录接口控制器
 *
 * 提供事件范围查询及手动事件 CRUD。
 *
 * @author FaFa Team
 * @since 4.0
 */
@RestController
@RequestMapping("/api/event-records")
@RequiredArgsConstructor
@Tag(name = "事件记录", description = "成长档案事件：Phase 3 提醒完成写入，范围查询")
public class EventRecordController {

    private final EventRecordApplicationService eventRecordApplicationService;

    @PostMapping
    @Operation(summary = "创建手动事件")
    public Result<EventRecordDTO> create(@Valid @RequestBody CreateEventRequest request) {
        return Result.ok(eventRecordApplicationService.createManual(new CreateEventCommand(
                request.getPetId(), request.getEventType(), request.getTitle(), request.getContent(),
                request.getEventDate(), request.getMaterialUrls(), request.getTags())));
    }

    @PutMapping("/{id}")
    @Operation(summary = "编辑手动事件", description = "提醒完成生成的事件不可编辑")
    public Result<EventRecordDTO> update(
            @Parameter(description = "事件ID", required = true, example = "1") @PathVariable Long id,
            @Valid @RequestBody UpdateEventRequest request
    ) {
        return Result.ok(eventRecordApplicationService.updateManual(id, new UpdateEventCommand(
                request.getEventType(), request.getTitle(), request.getContent(), request.getEventDate(),
                request.getMaterialUrls(), request.getTags())));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除手动事件", description = "提醒完成生成的事件不可删除")
    public Result<Void> delete(@Parameter(description = "事件ID", required = true, example = "1") @PathVariable Long id) {
        eventRecordApplicationService.deleteManual(id);
        return Result.ok();
    }

    @GetMapping
    @Operation(summary = "事件记录范围查询", description = "日期升序，范围不超过366天")
    public Result<List<EventRecordDTO>> listByRange(
            @Parameter(description = "宠物ID", required = true, example = "1") @RequestParam Long petId,
            @Parameter(description = "开始日期", required = true, example = "2026-08-01") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期", required = true, example = "2026-08-31") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return Result.ok(eventRecordApplicationService.listByPetAndRange(petId, startDate, endDate));
    }
}
