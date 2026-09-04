package com.fafa.interfaces.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fafa.application.dto.content.*;
import com.fafa.application.service.ContentDraftApplicationService;
import com.fafa.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/content-drafts")
@RequiredArgsConstructor
@Tag(name = "内容草稿", description = "分享笔记与小红书文案生成草稿")
public class ContentDraftController {
    private final ContentDraftApplicationService draftService;

    @PostMapping
    @Operation(summary = "生成内容草稿")
    public Result<ContentDraftDTO> create(@RequestBody CreateBody body) {
        return Result.ok(draftService.create(new CreateContentDraftCommand(StpUtil.getLoginIdAsLong(), body.petId,
                body.contentType, body.topicType, body.customTopic, body.periodType, body.anchorDate,
                body.selectedMediaIds, body.idempotencyKey, "manual")));
    }

    @GetMapping
    @Operation(summary = "查询内容草稿列表")
    public Result<List<ContentDraftDTO>> list(
                                              @Parameter(description = "宠物ID", required = true, example = "1") @RequestParam Long petId,
                                              @Parameter(description = "页码，从1开始", example = "1") @RequestParam(defaultValue = "1") int page,
                                              @Parameter(description = "每页草稿数", example = "20") @RequestParam(defaultValue = "20") int size) {
        return Result.ok(draftService.list(StpUtil.getLoginIdAsLong(), petId, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询内容草稿详情")
    public Result<ContentDraftDTO> get(
            @Parameter(description = "内容草稿ID", required = true, example = "1") @PathVariable Long id) {
        return Result.ok(draftService.get(StpUtil.getLoginIdAsLong(), id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新内容草稿")
    public Result<ContentDraftDTO> update(
            @Parameter(description = "内容草稿ID", required = true, example = "1") @PathVariable Long id,
            @RequestBody UpdateBody body) {
        return Result.ok(draftService.update(new UpdateContentDraftCommand(StpUtil.getLoginIdAsLong(), id,
                body.draft == null ? null : body.draft.toString())));
    }

    @PostMapping("/{id}/regenerate")
    @Operation(summary = "重新生成内容草稿")
    public Result<ContentDraftDTO> regenerate(
            @Parameter(description = "内容草稿ID", required = true, example = "1") @PathVariable Long id) {
        return Result.ok(draftService.regenerate(StpUtil.getLoginIdAsLong(), id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除内容草稿")
    public Result<Void> delete(
            @Parameter(description = "内容草稿ID", required = true, example = "1") @PathVariable Long id) {
        draftService.delete(StpUtil.getLoginIdAsLong(), id);
        return Result.ok();
    }

    @Data
    @io.swagger.v3.oas.annotations.media.Schema(description = "生成内容草稿请求")
    public static class CreateBody {
        @io.swagger.v3.oas.annotations.media.Schema(description = "所属宠物ID", example = "1")
        private Long petId;
        @io.swagger.v3.oas.annotations.media.Schema(description = "内容类型", example = "share_note")
        private String contentType = "share_note";
        @io.swagger.v3.oas.annotations.media.Schema(description = "主题类型", example = "monthly_summary")
        private String topicType = "monthly_summary";
        @io.swagger.v3.oas.annotations.media.Schema(description = "自定义主题；使用自定义主题时填写")
        private String customTopic;
        @io.swagger.v3.oas.annotations.media.Schema(description = "统计周期类型", example = "month")
        private String periodType = "month";
        @io.swagger.v3.oas.annotations.media.Schema(description = "统计锚点日期；不传时由服务端推断", example = "2026-08-31")
        private LocalDate anchorDate;
        @io.swagger.v3.oas.annotations.media.Schema(description = "选中的素材ID列表")
        private List<Long> selectedMediaIds;
        @io.swagger.v3.oas.annotations.media.Schema(description = "幂等键，用于避免重复生成")
        private String idempotencyKey;
    }

    @Data
    @io.swagger.v3.oas.annotations.media.Schema(description = "更新内容草稿请求")
    public static class UpdateBody {
        @io.swagger.v3.oas.annotations.media.Schema(description = "更新后的草稿 JSON 内容")
        private JsonNode draft;
    }
}
