package com.fafa.interfaces.controller;

import com.fafa.common.Result;
import com.fafa.application.dto.pet.PetVectorizationCallbackCommand;
import com.fafa.application.service.MediaApplicationService;
import com.fafa.application.service.PetApplicationService;
import com.fafa.interfaces.dto.callback.PetVectorizationCallback;
import com.fafa.interfaces.dto.callback.PhotoAnalysisCallback;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Python AI 服务回调接口
 *
 * 接收 fafa-python 异步任务的处理结果，回写业务状态。
 * 路径约定见《技术选型与项目框架搭建》7.3 服务间接口契约。
 *
 * @author FaFa Team
 * @since 1.0
 */
@Slf4j
@Tag(name = "内部回调", description = "仅限 fafa-python 内网调用，需携带 X-Internal-Token")
@RestController
@RequestMapping("/api/callback")
@RequiredArgsConstructor
public class CallbackController {

    private final MediaApplicationService mediaApplicationService;

    private final PetApplicationService petApplicationService;

    /** 素材分析结果回调 */
    @Operation(summary = "素材分析结果回调")
    @PostMapping("/photo-analysis")
    public Result<Void> onPhotoAnalysis(@RequestBody @Valid PhotoAnalysisCallback callback) {
        mediaApplicationService.handleAnalysisCallback(new com.fafa.application.dto.media.PhotoAnalysisCallback(
                callback.getMediaId(),
                Boolean.TRUE.equals(callback.getSuccess()),
                callback.getRecognizedPetIds() != null
                        ? callback.getRecognizedPetIds()
                        : callback.getRecognizedPetId() == null
                        ? java.util.List.of()
                        : java.util.List.of(callback.getRecognizedPetId()),
                callback.getRecognitionScore(),
                callback.getErrorMessage()
        ));
        log.info("素材向量化回调处理完成: mediaId={}, petIds={}, success={}",
                callback.getMediaId(), callback.getRecognizedPetIds(), callback.getSuccess());
        return Result.ok();
    }

    /** 宠物三视图向量化结果回调（Python 完成 embed_profile_photos 并写入 Qdrant 后调用） */
    @Operation(summary = "宠物三视图向量化结果回调")
    @PostMapping("/pet-vectorization")
    public Result<Void> onPetVectorization(@RequestBody @Valid PetVectorizationCallback callback) {
        petApplicationService.handleVectorizationCallback(new PetVectorizationCallbackCommand(
                callback.getPetId(),
                Boolean.TRUE.equals(callback.getSuccess()),
                callback.getMessage()
        ));
        log.info("宠物三视图向量化回调处理完成: petId={}, success={}, message={}",
                callback.getPetId(), callback.getSuccess(), callback.getMessage());
        return Result.ok();
    }
}
