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

    @PostMapping
    @Operation(summary = "创建食品卡")
    public Result<FoodCardDTO> create(@Valid @RequestBody SaveFoodCardRequest request) {
        return Result.ok(service.create(toCommand(request)));
    }

    @GetMapping
    @Operation(summary = "查询食品卡列表")
    public Result<List<FoodCardDTO>> list(@RequestParam Long petId,
                                          @RequestParam(required = false) String status) {
        return Result.ok(service.list(petId, status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询食品卡详情")
    public Result<FoodCardDTO> get(@Parameter(description = "食品卡ID") @PathVariable Long id) {
        return Result.ok(service.get(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新食品卡")
    public Result<FoodCardDTO> update(@PathVariable Long id,
                                      @Valid @RequestBody SaveFoodCardRequest request) {
        return Result.ok(service.update(id, toCommand(request)));
    }

    @PatchMapping("/{id}/archive")
    @Operation(summary = "归档食品卡")
    public Result<FoodCardDTO> archive(@PathVariable Long id) {
        return Result.ok(service.archive(id));
    }

    @PostMapping("/{id}/activate")
    @Operation(summary = "恢复食品卡")
    public Result<FoodCardDTO> activate(@PathVariable Long id) {
        return Result.ok(service.activate(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除食品卡", description = "逻辑删除")
    public Result<Void> delete(@PathVariable Long id) {
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
