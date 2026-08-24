package com.fafa.interfaces.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fafa.application.dto.content.ContentScheduleDTO;
import com.fafa.application.dto.content.UpsertContentScheduleCommand;
import com.fafa.application.service.ContentScheduleApplicationService;
import com.fafa.common.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/content-schedules")
@RequiredArgsConstructor
@Tag(name = "内容周期配置")
public class ContentScheduleController {
    private final ContentScheduleApplicationService service;

    @GetMapping
    public Result<List<ContentScheduleDTO>> list(@RequestParam(required = false) Long petId) {
        return Result.ok(service.list(StpUtil.getLoginIdAsLong(), petId));
    }

    @PutMapping
    public Result<ContentScheduleDTO> upsert(@RequestBody Body body) {
        return Result.ok(service.upsert(new UpsertContentScheduleCommand(StpUtil.getLoginIdAsLong(), body.petId,
                body.contentType, body.topicType, body.intervalDays, body.nextRunAt)));
    }

    @PostMapping("/{id}/enable")
    public Result<ContentScheduleDTO> enable(@PathVariable Long id) {
        return Result.ok(service.enable(StpUtil.getLoginIdAsLong(), id, true));
    }

    @PostMapping("/{id}/disable")
    public Result<ContentScheduleDTO> disable(@PathVariable Long id) {
        return Result.ok(service.enable(StpUtil.getLoginIdAsLong(), id, false));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(StpUtil.getLoginIdAsLong(), id); return Result.ok();
    }

    @Data
    public static class Body {
        private Long petId;
        private String contentType = "share_note";
        private String topicType = "monthly_summary";
        private int intervalDays = 15;
        private LocalDateTime nextRunAt;
    }
}
