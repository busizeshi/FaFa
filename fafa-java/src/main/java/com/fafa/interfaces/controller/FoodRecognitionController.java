package com.fafa.interfaces.controller;

import com.fafa.application.dto.food.FoodCardDTO;
import com.fafa.application.dto.food.FoodRecognitionDTO;
import com.fafa.application.service.FoodRecognitionApplicationService;
import com.fafa.common.Result;
import com.fafa.interfaces.dto.food.ConfirmFoodRecognitionRequest;
import com.fafa.interfaces.dto.food.CreateFoodRecognitionRequest;
import com.fafa.interfaces.dto.food.RejectFoodRecognitionRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 食品包装识别接口。 */
@RestController
@RequestMapping("/api/foods/recognitions")
@RequiredArgsConstructor
@Tag(name = "食品包装识别", description = "包装图片识别、草稿确认和拒绝")
public class FoodRecognitionController {

    private final FoodRecognitionApplicationService service;

    @PostMapping
    @Operation(summary = "创建包装识别任务")
    public Result<FoodRecognitionDTO> create(@Valid @RequestBody CreateFoodRecognitionRequest request) {
        return Result.ok(service.create(request.getPetId(), request.getMediaId(), request.getIdempotencyKey()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询识别任务")
    public Result<FoodRecognitionDTO> get(@PathVariable Long id) {
        return Result.ok(service.get(id));
    }

    @PostMapping("/{id}/confirm")
    @Operation(summary = "确认识别结果")
    public Result<FoodCardDTO> confirm(@PathVariable Long id,
                                       @Valid @RequestBody ConfirmFoodRecognitionRequest request) {
        return Result.ok(service.confirm(id, request));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "拒绝识别结果")
    public Result<Void> reject(@PathVariable Long id,
                               @Valid @RequestBody(required = false) RejectFoodRecognitionRequest request) {
        service.reject(id, request == null ? null : request.getReason());
        return Result.ok();
    }
}
