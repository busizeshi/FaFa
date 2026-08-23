package com.fafa.interfaces.controller;

import com.fafa.application.dto.reminder.*;
import com.fafa.application.service.ReminderApplicationService;
import com.fafa.common.Result;
import com.fafa.interfaces.dto.reminder.CompleteReminderRequest;
import com.fafa.interfaces.dto.reminder.CreateReminderRequest;
import com.fafa.interfaces.dto.reminder.UpdateReminderRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 提醒接口控制器
 *
 * @author FaFa Team
 * @since 3.0
 */
@RestController
@RequestMapping("/api/reminders")
@RequiredArgsConstructor
@Tag(name = "提醒", description = "提醒系统：周期任务定义、完成闭环、今日提醒")
public class ReminderController {

    private final ReminderApplicationService reminderApplicationService;

    @PostMapping
    @Operation(summary = "创建提醒")
    public Result<ReminderDTO> create(@Valid @RequestBody CreateReminderRequest request) {
        return Result.ok(reminderApplicationService.create(new CreateReminderCommand(
                request.getPetId(),
                request.getType(),
                request.getTitle(),
                request.getDescription(),
                request.getIntervalType(),
                request.getIntervalValue(),
                request.getNextRemindTime()
        )));
    }

    @GetMapping("/today")
    @Operation(summary = "今日提醒", description = "待办（含逾期）+ 今日已完成，跨宠物合并视图")
    public Result<List<TodayReminderDTO>> today() {
        return Result.ok(reminderApplicationService.today());
    }

    @GetMapping
    @Operation(summary = "宠物提醒列表", description = "status 可选过滤：pending/paused/finished")
    public Result<List<ReminderDTO>> listByPet(
            @Parameter(description = "宠物ID") @RequestParam Long petId,
            @Parameter(description = "状态过滤（可选）") @RequestParam(required = false) String status
    ) {
        return Result.ok(reminderApplicationService.listByPet(petId, status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "提醒详情", description = "编辑页回显")
    public Result<ReminderDTO> getById(@Parameter(description = "提醒ID") @PathVariable Long id) {
        return Result.ok(reminderApplicationService.getById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "编辑提醒", description = "petId 不可改")
    public Result<ReminderDTO> update(
            @Parameter(description = "提醒ID") @PathVariable Long id,
            @Valid @RequestBody UpdateReminderRequest request
    ) {
        return Result.ok(reminderApplicationService.update(id, new UpdateReminderCommand(
                request.getType(),
                request.getTitle(),
                request.getDescription(),
                request.getIntervalType(),
                request.getIntervalValue(),
                request.getNextRemindTime()
        )));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除提醒", description = "逻辑删除，完成历史保留")
    public Result<Void> delete(@Parameter(description = "提醒ID") @PathVariable Long id) {
        reminderApplicationService.delete(id);
        return Result.ok();
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "完成提醒", description = "完成闭环：生成事件记录 + 完成历史 + 推进下期")
    public Result<ReminderDTO> complete(
            @Parameter(description = "提醒ID") @PathVariable Long id,
            @Valid @RequestBody(required = false) CompleteReminderRequest request
    ) {
        CompleteReminderRequest req = request != null ? request : new CompleteReminderRequest();
        return Result.ok(reminderApplicationService.complete(id, new CompleteReminderCommand(
                req.getCompletionTime(),
                req.getNotes(),
                req.getPhotoUrls()
        )));
    }

    @PostMapping("/{id}/pause")
    @Operation(summary = "暂停提醒")
    public Result<ReminderDTO> pause(@Parameter(description = "提醒ID") @PathVariable Long id) {
        return Result.ok(reminderApplicationService.pause(id));
    }

    @PostMapping("/{id}/resume")
    @Operation(summary = "恢复提醒")
    public Result<ReminderDTO> resume(@Parameter(description = "提醒ID") @PathVariable Long id) {
        return Result.ok(reminderApplicationService.resume(id));
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "完成历史", description = "时间倒序")
    public Result<List<CompletionDTO>> history(@Parameter(description = "提醒ID") @PathVariable Long id) {
        return Result.ok(reminderApplicationService.history(id));
    }
}
