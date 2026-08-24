package com.fafa.interfaces.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fafa.application.dto.content.*;
import com.fafa.application.service.ContentDraftApplicationService;
import com.fafa.common.Result;
import io.swagger.v3.oas.annotations.Operation;
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
    public Result<List<ContentDraftDTO>> list(@RequestParam Long petId,
                                              @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return Result.ok(draftService.list(StpUtil.getLoginIdAsLong(), petId, page, size));
    }

    @GetMapping("/{id}")
    public Result<ContentDraftDTO> get(@PathVariable Long id) {
        return Result.ok(draftService.get(StpUtil.getLoginIdAsLong(), id));
    }

    @PutMapping("/{id}")
    public Result<ContentDraftDTO> update(@PathVariable Long id, @RequestBody UpdateBody body) {
        return Result.ok(draftService.update(new UpdateContentDraftCommand(StpUtil.getLoginIdAsLong(), id,
                body.draft == null ? null : body.draft.toString())));
    }

    @PostMapping("/{id}/regenerate")
    public Result<ContentDraftDTO> regenerate(@PathVariable Long id) {
        return Result.ok(draftService.regenerate(StpUtil.getLoginIdAsLong(), id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        draftService.delete(StpUtil.getLoginIdAsLong(), id);
        return Result.ok();
    }

    @Data
    public static class CreateBody {
        private Long petId;
        private String contentType = "share_note";
        private String topicType = "monthly_summary";
        private String customTopic;
        private String periodType = "month";
        private LocalDate anchorDate;
        private List<Long> selectedMediaIds;
        private String idempotencyKey;
    }

    @Data
    public static class UpdateBody {
        private JsonNode draft;
    }
}
