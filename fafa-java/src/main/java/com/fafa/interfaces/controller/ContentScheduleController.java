package com.fafa.interfaces.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fafa.application.dto.content.ContentScheduleDTO;
import com.fafa.application.dto.content.UpsertContentScheduleCommand;
import com.fafa.application.service.ContentScheduleApplicationService;
import com.fafa.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/content-schedules")
@RequiredArgsConstructor
@Tag(name = "内容周期配置", description = "周期生成内容的配置管理")
public class ContentScheduleController {
    private final ContentScheduleApplicationService service;

    @GetMapping
    @Operation(summary = "查询内容周期配置")
    public Result<List<ContentScheduleDTO>> list(
            @Parameter(description = "宠物过滤条件；不传表示查询全部配置", example = "1")
            @RequestParam(required = false) Long petId) {
        return Result.ok(service.list(StpUtil.getLoginIdAsLong(), petId));
    }

    @PutMapping
    @Operation(summary = "保存内容周期配置")
    public Result<ContentScheduleDTO> upsert(@RequestBody Body body) {
        return Result.ok(service.upsert(new UpsertContentScheduleCommand(StpUtil.getLoginIdAsLong(), body.petId,
                body.contentType, body.topicType, body.intervalDays, body.nextRunAt)));
    }

    @PostMapping("/{id}/enable")
    @Operation(summary = "启用内容周期配置")
    public Result<ContentScheduleDTO> enable(
            @Parameter(description = "周期配置ID", required = true, example = "1") @PathVariable Long id) {
        return Result.ok(service.enable(StpUtil.getLoginIdAsLong(), id, true));
    }

    @PostMapping("/{id}/disable")
    @Operation(summary = "停用内容周期配置")
    public Result<ContentScheduleDTO> disable(
            @Parameter(description = "周期配置ID", required = true, example = "1") @PathVariable Long id) {
        return Result.ok(service.enable(StpUtil.getLoginIdAsLong(), id, false));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除内容周期配置")
    public Result<Void> delete(
            @Parameter(description = "周期配置ID", required = true, example = "1") @PathVariable Long id) {
        service.delete(StpUtil.getLoginIdAsLong(), id); return Result.ok();
    }

    @Data
    @io.swagger.v3.oas.annotations.media.Schema(description = "内容周期配置请求")
    public static class Body {
        @io.swagger.v3.oas.annotations.media.Schema(description = "所属宠物ID", example = "1")
        private Long petId;
        @io.swagger.v3.oas.annotations.media.Schema(description = "内容类型", example = "share_note")
        private String contentType = "share_note";
        @io.swagger.v3.oas.annotations.media.Schema(description = "主题类型", example = "monthly_summary")
        private String topicType = "monthly_summary";
        @io.swagger.v3.oas.annotations.media.Schema(description = "执行间隔天数", example = "15")
        private int intervalDays = 15;
        @io.swagger.v3.oas.annotations.media.Schema(description = "下次执行时间；不传时由服务端推断")
        private LocalDateTime nextRunAt;
    }
}
