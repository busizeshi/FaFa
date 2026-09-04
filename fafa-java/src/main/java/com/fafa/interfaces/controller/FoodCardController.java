package com.fafa.interfaces.controller;

import com.fafa.application.dto.food.FoodCardCommand;
import com.fafa.application.dto.food.FoodCardDTO;
import com.fafa.application.service.FoodCardApplicationService;
import com.fafa.common.Result;
import com.fafa.interfaces.dto.food.SaveFoodCardRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 食品管理卡接口。 */
@RestController
@RequestMapping("/api/foods")
@RequiredArgsConstructor
@Tag(name = "食品管理", description = "食品卡的创建、编辑、归档和营养信息")
public class FoodCardController {

    private final FoodCardApplicationService service;

//    todo 需要增加视觉扫描的接口，用来一键识别宠物粮食外包装解析为食品卡

    @PostMapping
    @Operation(summary = "创建食品卡", description = "为指定宠物创建一张食品管理卡，食品卡默认处于 active（使用中）状态")
    public Result<FoodCardDTO> create(@Valid @RequestBody SaveFoodCardRequest request) {
        return Result.ok(service.create(toCommand(request)));
    }

    @GetMapping
    @Operation(summary = "查询食品卡列表", description = "按更新时间倒序返回指定宠物的食品卡列表，逻辑删除的食品卡不会返回")
    public Result<List<FoodCardDTO>> list(
            @Parameter(description = "所属宠物ID", required = true, example = "1")
            @RequestParam Long petId,
            @Parameter(description = "食品卡状态筛选：active=使用中，archived=已归档；不传表示不按状态筛选", example = "active")
            @RequestParam(required = false) String status) {
        return Result.ok(service.list(petId, status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询食品卡详情")
    public Result<FoodCardDTO> get(
            @Parameter(description = "食品卡ID", required = true, example = "1") @PathVariable Long id) {
        return Result.ok(service.get(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新食品卡", description = "全量更新食品卡信息，petId 必须与路径中的食品卡所属宠物一致")
    public Result<FoodCardDTO> update(
            @Parameter(description = "食品卡ID", required = true, example = "1") @PathVariable Long id,
                                      @Valid @RequestBody SaveFoodCardRequest request) {
        return Result.ok(service.update(id, toCommand(request)));
    }

    @PatchMapping("/{id}/archive")
    @Operation(summary = "归档食品卡")
    public Result<FoodCardDTO> archive(
            @Parameter(description = "食品卡ID", required = true, example = "1") @PathVariable Long id) {
        return Result.ok(service.archive(id));
    }

    @PostMapping("/{id}/activate")
    @Operation(summary = "恢复食品卡")
    public Result<FoodCardDTO> activate(
            @Parameter(description = "食品卡ID", required = true, example = "1") @PathVariable Long id) {
        return Result.ok(service.activate(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除食品卡", description = "逻辑删除")
    public Result<Void> delete(
            @Parameter(description = "食品卡ID", required = true, example = "1") @PathVariable Long id) {
        service.delete(id);
        return Result.ok();
    }

    private FoodCardCommand toCommand(SaveFoodCardRequest request) {
        return new FoodCardCommand(request.getPetId(), request.getName(), request.getBrand(), request.getFoodType(),
                request.getSpecification(), request.getPackageWeight(), request.getPackageWeightUnit(),
                request.getPrice(), request.getCurrency(), request.getOpenedAt(), request.getExpiresAt(),
                request.getNutritionBasis(), request.getProteinPercent(), request.getFatPercent(),
                request.getFiberPercent(), request.getMoisturePercent(), request.getAshPercent(),
                request.getCalciumPercent(), request.getPhosphorusPercent(), request.getEnergyKcal(),
                request.getEnergyUnit(), request.getProteinSources(), request.getIngredients(),
                request.getAdditiveFlags(), request.getLifeStageLabel(), request.getSourceMediaId(),
                request.getSourceRecognitionTaskId(), request.getNotes());
    }
}
