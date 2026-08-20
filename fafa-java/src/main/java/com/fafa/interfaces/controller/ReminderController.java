package com.fafa.interfaces.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fafa.application.dto.reminder.*;
import com.fafa.application.service.ReminderApplicationService;
import com.fafa.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 提醒接口
 */
@RestController
@RequestMapping("/api/reminders")
@RequiredArgsConstructor
public class ReminderController {

    private final ReminderApplicationService reminderApplicationService;

    /**
     * 创建提醒
     */
    @PostMapping
    public Result<ReminderResponse> createReminder(@Validated @RequestBody CreateReminderRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        Long reminderId = reminderApplicationService.createReminder(userId, request);
        ReminderResponse response = reminderApplicationService.getReminderDetail(userId, reminderId);
        return Result.success(response);
    }

    /**
     * 查询提醒列表
     * 支持按 petId 或 userId 查询
     */
    @GetMapping
    public Result<List<ReminderResponse>> listReminders(
            @RequestParam(required = false) Long petId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Long userId = StpUtil.getLoginIdAsLong();
        List<ReminderResponse> reminders;
        if (petId != null) {
            reminders = reminderApplicationService.listRemindersByPet(userId, petId, status, pageNum, pageSize);
        } else {
            reminders = reminderApplicationService.listRemindersByUser(userId, status, pageNum, pageSize);
        }
        return Result.success(reminders);
    }

    /**
     * 查询提醒详情
     */
    @GetMapping("/{id}")
    public Result<ReminderResponse> getReminderDetail(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        ReminderResponse response = reminderApplicationService.getReminderDetail(userId, id);
        return Result.success(response);
    }

    /**
     * 更新提醒
     */
    @PutMapping("/{id}")
    public Result<ReminderResponse> updateReminder(
            @PathVariable Long id,
            @Validated @RequestBody UpdateReminderRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        reminderApplicationService.updateReminder(userId, id, request);
        ReminderResponse response = reminderApplicationService.getReminderDetail(userId, id);
        return Result.success(response);
    }

    /**
     * 删除提醒
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteReminder(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        reminderApplicationService.deleteReminder(userId, id);
        return Result.success();
    }

    /**
     * 完成提醒
     */
    @PutMapping("/{id}/complete")
    public Result<Void> completeReminder(
            @PathVariable Long id,
            @Validated @RequestBody CompleteReminderRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        reminderApplicationService.completeReminder(userId, id, request);
        return Result.success();
    }

    /**
     * 取消提醒
     */
    @PutMapping("/{id}/cancel")
    public Result<Void> cancelReminder(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        reminderApplicationService.cancelReminder(userId, id);
        return Result.success();
    }

    /**
     * 统计提醒数量
     */
    @GetMapping("/count")
    public Result<Map<String, Integer>> countReminders(
            @RequestParam(required = false) Long petId,
            @RequestParam(required = false) String status) {
        Long userId = StpUtil.getLoginIdAsLong();
        int count;
        if (petId != null) {
            count = reminderApplicationService.countRemindersByPet(userId, petId, status);
        } else {
            count = reminderApplicationService.countRemindersByUser(userId, status);
        }
        return Result.success(Map.of("count", count));
    }
}
