package com.fafa.interfaces.controller;

import com.fafa.application.dto.tag.CreateTagCommand;
import com.fafa.application.dto.tag.TagDTO;
import com.fafa.application.service.TagApplicationService;
import com.fafa.common.Result;
import com.fafa.interfaces.dto.tag.CreateTagRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 标签接口控制器
 *
 * 标签字典 CRUD；打标联动在素材/事件接口编排。
 *
 * @author FaFa Team
 * @since 4.0
 */
@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
@Tag(name = "标签", description = "标签字典：列表/创建/删除")
public class TagController {

    private final TagApplicationService tagApplicationService;

    @GetMapping
    @Operation(summary = "标签列表", description = "按使用次数降序、最后使用时间降序")
    public Result<List<TagDTO>> list() {
        return Result.ok(tagApplicationService.list());
    }

    @PostMapping
    @Operation(summary = "创建标签", description = "同名重复返回 TAG_DUPLICATE")
    public Result<TagDTO> create(@Valid @RequestBody CreateTagRequest request) {
        return Result.ok(tagApplicationService.create(
                new CreateTagCommand(request.getName(), request.getCategory())));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除标签", description = "物理删除标签定义，不影响素材/事件上的标签快照")
    public Result<Void> delete(@Parameter(description = "标签ID") @PathVariable Long id) {
        tagApplicationService.delete(id);
        return Result.ok();
    }
}
