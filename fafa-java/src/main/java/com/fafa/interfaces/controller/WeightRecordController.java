package com.fafa.interfaces.controller;

import com.fafa.application.dto.record.SaveWeightRecordCommand;
import com.fafa.application.dto.record.WeightRecordDTO;
import com.fafa.application.dto.record.WeightTrendDTO;
import com.fafa.application.service.WeightRecordApplicationService;
import com.fafa.common.Result;
import com.fafa.interfaces.dto.record.SaveWeightRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 体重记录接口控制器
 *
 * @author FaFa Team
 * @since 2.0
 */
@RestController
@RequestMapping("/api/weight-records")
@RequiredArgsConstructor
@Tag(name = "体重记录", description = "周期测量：新增联动 pet.weight、趋势查询")
public class WeightRecordController {

    private final WeightRecordApplicationService weightRecordApplicationService;

    @PostMapping
    @Operation(summary = "新增体重记录", description = "同事务联动更新宠物档案体重")
    public Result<WeightRecordDTO> create(@Valid @RequestBody SaveWeightRequest request) {
        SaveWeightRecordCommand command = new SaveWeightRecordCommand(
                request.getPetId(),
                request.getRecordDate(),
                request.getWeight(),
                request.getNotes()
        );
        return Result.ok(weightRecordApplicationService.create(command));
    }

    @GetMapping("/latest")
    @Operation(summary = "最近N条记录", description = "按日期倒序，每条含较上一条的变化值")
    public Result<List<WeightRecordDTO>> latest(
            @Parameter(description = "宠物ID") @RequestParam Long petId,
            @Parameter(description = "条数，默认10，上限100") @RequestParam(defaultValue = "10") int limit
    ) {
        return Result.ok(weightRecordApplicationService.latest(petId, limit));
    }

    @GetMapping("/trend")
    @Operation(summary = "体重趋势", description = "升序+逐条变化+区间总变化；区间缺省为最近90天")
    public Result<WeightTrendDTO> trend(
            @Parameter(description = "宠物ID") @RequestParam Long petId,
            @Parameter(description = "开始日期（可选）") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期（可选）") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return Result.ok(weightRecordApplicationService.trend(petId, startDate, endDate));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除体重记录", description = "逻辑删除，不回滚宠物档案体重")
    public Result<Void> delete(@Parameter(description = "记录ID") @PathVariable Long id) {
        weightRecordApplicationService.delete(id);
        return Result.ok();
    }
}
