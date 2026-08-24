package com.fafa.interfaces.controller;

import com.fafa.application.dto.PageDTO;
import com.fafa.application.dto.media.ConfirmPetCommand;
import com.fafa.application.dto.media.MediaDTO;
import com.fafa.application.dto.media.SemanticSearchCommand;
import com.fafa.application.dto.media.SemanticSearchResultDTO;
import com.fafa.application.dto.media.UpdateMediaCommand;
import com.fafa.application.service.MediaApplicationService;
import com.fafa.common.Result;
import com.fafa.interfaces.dto.media.ConfirmPetRequest;
import com.fafa.interfaces.dto.media.SemanticSearchRequest;
import com.fafa.interfaces.dto.media.UpdateMediaRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 素材接口控制器（照片/视频）
 *
 * 路径沿用 /api/photos/**（springdoc group photo 已预置），素材语义含视频。
 *
 * @author FaFa Team
 * @since 4.0
 */
@RestController
@RequestMapping("/api/photos")
@RequiredArgsConstructor
@Tag(name = "素材", description = "素材上传/检索/语义检索/归属确认/编辑/删除")
public class PhotoController {

    private final MediaApplicationService mediaApplicationService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "批量上传素材", description = "单批≤20个；tags 应用到全部素材；照片和视频自动进入 AI 分析")
    public Result<List<MediaDTO>> upload(
            @Parameter(description = "素材文件（图片/视频）") @RequestParam("files") List<MultipartFile> files,
            @Parameter(description = "标签（逗号分隔，可选）") @RequestParam(value = "tags", required = false) List<String> tags,
            @Parameter(description = "归属宠物ID（可选，指定即确认归属）") @RequestParam(value = "petId", required = false) Long petId,
            @Parameter(description = "拍摄时间（可选，缺省为上传时刻）") @RequestParam(value = "takenAt", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime takenAt
    ) {
        return Result.ok(mediaApplicationService.upload(files, tags, petId, takenAt));
    }

    @GetMapping
    @Operation(summary = "确定性检索", description = "不限次；tags 多选为 OR 语义；按拍摄时间倒序分页")
    public Result<PageDTO<MediaDTO>> search(
            @Parameter(description = "宠物过滤（可选）") @RequestParam(required = false) Long petId,
            @Parameter(description = "标签过滤（可选，逗号分隔）") @RequestParam(required = false) List<String> tags,
            @Parameter(description = "开始日期（可选）") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期（可选）") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "页码（从1开始）") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int size
    ) {
        return Result.ok(mediaApplicationService.search(petId, tags, startDate, endDate, page, size));
    }

    @PostMapping("/search/semantic")
    @Operation(summary = "AI 语义检索", description = "自然语言检索；普通用户月限额，VIP 不限；失败请求不占额度")
    public Result<SemanticSearchResultDTO> semanticSearch(@Valid @RequestBody SemanticSearchRequest request) {
        return Result.ok(mediaApplicationService.semanticSearch(new SemanticSearchCommand(
                request.getQuery(),
                request.getPetId(),
                request.getStartDate(),
                request.getEndDate(),
                request.getLimit()
        )));
    }

    @GetMapping("/pending-recognition")
    @Operation(summary = "识别待确认列表", description = "AI 识别出疑似归属但未确认的素材（含相似度）")
    public Result<List<MediaDTO>> pendingRecognition() {
        return Result.ok(mediaApplicationService.findUnconfirmed());
    }

    @GetMapping("/{id}")
    @Operation(summary = "素材详情", description = "含 AI 描述、AI 标签、识别状态")
    public Result<MediaDTO> getById(@Parameter(description = "素材ID") @PathVariable Long id) {
        return Result.ok(mediaApplicationService.getById(id));
    }

    @PostMapping("/{id}/reanalyze")
    @Operation(summary = "重新分析素材", description = "重置为 pending 并重新投递照片/视频 AI 分析")
    public Result<MediaDTO> reanalyze(@Parameter(description = "素材ID") @PathVariable Long id) {
        return Result.ok(mediaApplicationService.reanalyze(id));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "编辑素材", description = "编辑标签与拍摄时间")
    public Result<MediaDTO> update(
            @Parameter(description = "素材ID") @PathVariable Long id,
            @Valid @RequestBody UpdateMediaRequest request
    ) {
        return Result.ok(mediaApplicationService.update(id,
                new UpdateMediaCommand(request.getTags(), request.getTakenAt())));
    }

    @PostMapping("/{id}/confirm-pet")
    @Operation(summary = "确认/修正归属", description = "petId 传 null 表示清除归属")
    public Result<MediaDTO> confirmPet(
            @Parameter(description = "素材ID") @PathVariable Long id,
            @Valid @RequestBody(required = false) ConfirmPetRequest request
    ) {
        Long petId = request != null ? request.getPetId() : null;
        return Result.ok(mediaApplicationService.confirmPet(id, new ConfirmPetCommand(petId)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除素材", description = "逻辑删除 + 释放存储配额 + 清理对象与向量")
    public Result<Void> delete(@Parameter(description = "素材ID") @PathVariable Long id) {
        mediaApplicationService.delete(id);
        return Result.ok();
    }
}
