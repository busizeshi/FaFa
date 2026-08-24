package com.fafa.interfaces.controller;

import com.fafa.application.dto.food.FoodSafetyScoreDTO;
import com.fafa.application.service.FoodSafetyScoreApplicationService;
import com.fafa.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 食品配方信息提示分接口。 */
@RestController
@RequestMapping("/api/foods")
@RequiredArgsConstructor
@Tag(name = "食品提示分", description = "可解释的食品配方信息提示")
public class FoodSafetyScoreController {

    private final FoodSafetyScoreApplicationService service;

    @GetMapping("/{id}/safety-score")
    @Operation(summary = "查询食品配方提示分")
    public Result<FoodSafetyScoreDTO> get(@PathVariable Long id) {
        return Result.ok(service.get(id));
    }
}
