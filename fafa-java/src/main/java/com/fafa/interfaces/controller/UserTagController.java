package com.fafa.interfaces.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fafa.application.service.UserTagApplicationService;
import com.fafa.common.result.Result;
import com.fafa.domain.model.tag.UserTag;
import com.fafa.interfaces.dto.request.CreateUserTagRequest;
import com.fafa.interfaces.dto.request.UpdateUserTagRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户标签控制器
 *
 * @author FaFa
 * @since 2026-08-20
 */
@Tag(name = "用户标签管理")
@RestController
@RequestMapping("/api/v1/user-tags")
@RequiredArgsConstructor
public class UserTagController {

    private final UserTagApplicationService userTagApplicationService;

    @Operation(summary = "创建标签")
    @PostMapping
    public Result<Long> createTag(@Validated @RequestBody CreateUserTagRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        UserTag userTag = userTagApplicationService.createTag(
                userId, 
                request.getTagName(), 
                request.getCategory()
        );
        return Result.success(userTag.getId().getValue());
    }

    @Operation(summary = "查询用户所有标签")
    @GetMapping
    public Result<List<UserTag>> getUserTags() {
        Long userId = StpUtil.getLoginIdAsLong();
        List<UserTag> tags = userTagApplicationService.getUserTags(userId);
        return Result.success(tags);
    }

    @Operation(summary = "查询热门标签")
    @GetMapping("/popular")
    public Result<List<UserTag>> getPopularTags(@RequestParam(defaultValue = "20") Integer limit) {
        Long userId = StpUtil.getLoginIdAsLong();
        List<UserTag> tags = userTagApplicationService.getPopularTags(userId, limit);
        return Result.success(tags);
    }

    @Operation(summary = "更新标签")
    @PutMapping("/{tagId}")
    public Result<Void> updateTag(
            @PathVariable Long tagId,
            @Validated @RequestBody UpdateUserTagRequest request) {
        userTagApplicationService.updateTag(tagId, request.getTagName(), request.getCategory());
        return Result.success();
    }

    @Operation(summary = "删除标签")
    @DeleteMapping("/{tagId}")
    public Result<Void> deleteTag(@PathVariable Long tagId) {
        userTagApplicationService.deleteTag(tagId);
        return Result.success();
    }

    @Operation(summary = "批量删除标签")
    @DeleteMapping("/batch")
    public Result<Void> batchDeleteTags(@RequestBody List<Long> tagIds) {
        userTagApplicationService.batchDeleteTags(tagIds);
        return Result.success();
    }

    @Operation(summary = "统计用户标签数量")
    @GetMapping("/count")
    public Result<Integer> countUserTags() {
        Long userId = StpUtil.getLoginIdAsLong();
        int count = userTagApplicationService.countUserTags(userId);
        return Result.success(count);
    }
}
