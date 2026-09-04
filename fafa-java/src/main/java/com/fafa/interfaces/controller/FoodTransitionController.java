package com.fafa.interfaces.controller;

import com.fafa.application.dto.food.FoodTransitionDTO;
import com.fafa.application.service.FoodTransitionApplicationService;
import com.fafa.common.Result;
import com.fafa.interfaces.dto.food.SaveFoodTransitionRequest;
import com.fafa.interfaces.dto.food.UpdateFoodReactionRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 换粮过渡接口。 */
@RestController
@RequestMapping("/api/food-transitions")
@RequiredArgsConstructor
@Tag(name = "换粮过渡", description = "新旧食品比例和宠物反应记录")
public class FoodTransitionController {

    private final FoodTransitionApplicationService service;

    @PostMapping
    @Operation(summary = "创建换粮计划")
    public Result<FoodTransitionDTO> create(@Valid @RequestBody SaveFoodTransitionRequest request) {
        return Result.ok(service.create(request));
    }

    @GetMapping
    @Operation(summary = "查询换粮计划")
    public Result<List<FoodTransitionDTO>> list(
                                                @Parameter(description = "宠物ID", required = true, example = "1") @RequestParam Long petId,
                                                @Parameter(description = "计划状态过滤：draft=草稿，active=进行中，completed=已完成，cancelled=已取消", example = "active") @RequestParam(required = false) String status) {
        return Result.ok(service.list(petId, status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询换粮详情")
    public Result<FoodTransitionDTO> get(
            @Parameter(description = "换粮计划ID", required = true, example = "1") @PathVariable Long id) {
        return Result.ok(service.get(id));
    }

    @PutMapping("/{id}/reaction")
    @Operation(summary = "记录每日换粮反应")
    public Result<FoodTransitionDTO> updateReaction(
                                                     @Parameter(description = "换粮计划ID", required = true, example = "1") @PathVariable Long id,
                                                     @Valid @RequestBody UpdateFoodReactionRequest request) {
        return Result.ok(service.updateReaction(id, request));
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "完成换粮计划")
    public Result<FoodTransitionDTO> complete(
            @Parameter(description = "换粮计划ID", required = true, example = "1") @PathVariable Long id) {
        return Result.ok(service.complete(id));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消换粮计划")
    public Result<FoodTransitionDTO> cancel(
            @Parameter(description = "换粮计划ID", required = true, example = "1") @PathVariable Long id) {
        return Result.ok(service.cancel(id));
    }
}
