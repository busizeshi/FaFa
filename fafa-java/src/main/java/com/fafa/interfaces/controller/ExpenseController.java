package com.fafa.interfaces.controller;

import com.fafa.application.dto.PageDTO;
import com.fafa.application.dto.expense.ExpenseCategoryDTO;
import com.fafa.application.dto.expense.ExpenseCommand;
import com.fafa.application.dto.expense.ExpenseDTO;
import com.fafa.application.dto.expense.ExpenseStatisticsDTO;
import com.fafa.application.service.ExpenseApplicationService;
import com.fafa.application.service.ExpenseStatisticsApplicationService;
import com.fafa.common.Result;
import com.fafa.interfaces.dto.expense.SaveExpenseRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** 宠物开支记录与统计接口。 */
@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
@Tag(name = "宠物记账", description = "宠物日常开支和费用统计")
public class ExpenseController {

    private final ExpenseApplicationService expenseService;
    private final ExpenseStatisticsApplicationService statisticsService;

    @PostMapping
    @Operation(summary = "创建手工开支")
    public Result<ExpenseDTO> create(@Valid @RequestBody SaveExpenseRequest request) {
        return Result.ok(expenseService.create(toCommand(request)));
    }

    @GetMapping
    @Operation(summary = "分页查询开支")
    public Result<PageDTO<ExpenseDTO>> list(
                                            @Parameter(description = "宠物ID", required = true, example = "1") @RequestParam Long petId,
                                            @Parameter(description = "统计开始日期", required = true, example = "2026-08-01") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                            LocalDate startDate,
                                            @Parameter(description = "统计结束日期", required = true, example = "2026-08-31") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                            LocalDate endDate,
                                            @Parameter(description = "分类过滤条件，可选", example = "food") @RequestParam(required = false) String category,
                                            @Parameter(description = "页码，从1开始", example = "1") @RequestParam(defaultValue = "1") int page,
                                            @Parameter(description = "每页记录数", example = "20") @RequestParam(defaultValue = "20") int size) {
        return Result.ok(expenseService.list(petId, startDate, endDate, category, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询开支详情")
    public Result<ExpenseDTO> get(
            @Parameter(description = "开支记录ID", required = true, example = "1") @PathVariable Long id) {
        return Result.ok(expenseService.get(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新手工开支")
    public Result<ExpenseDTO> update(
                                     @Parameter(description = "开支记录ID", required = true, example = "1") @PathVariable Long id,
                                     @Valid @RequestBody SaveExpenseRequest request) {
        return Result.ok(expenseService.update(id, toCommand(request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除手工开支")
    public Result<Void> delete(
            @Parameter(description = "开支记录ID", required = true, example = "1") @PathVariable Long id) {
        expenseService.delete(id);
        return Result.ok();
    }

    @GetMapping("/categories")
    @Operation(summary = "查询开支分类")
    public Result<List<ExpenseCategoryDTO>> categories() {
        return Result.ok(expenseService.categories());
    }

    @GetMapping("/statistics")
    @Operation(summary = "查询开支统计")
    public Result<ExpenseStatisticsDTO> statistics(
                                                   @Parameter(description = "宠物ID", required = true, example = "1") @RequestParam Long petId,
                                                   @Parameter(description = "统计周期：month=月，year=年", example = "month") @RequestParam(defaultValue = "month") String period,
                                                   @Parameter(description = "统计周期锚点日期", required = true, example = "2026-08-31") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                   LocalDate anchorDate) {
        return Result.ok(statisticsService.statistics(petId, period, anchorDate));
    }

    private ExpenseCommand toCommand(SaveExpenseRequest request) {
        return new ExpenseCommand(request.getPetId(), request.getCategory(), request.getAmount(),
                request.getCurrency(), request.getExpenseDate(), request.getTitle(), request.getNotes(),
                request.getReceiptMediaId(), request.getFoodCardId());
    }
}
