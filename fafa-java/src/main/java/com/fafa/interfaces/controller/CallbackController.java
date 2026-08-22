package com.fafa.interfaces.controller;

import com.fafa.common.Result;
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

    /**
     * 照片分析结果回调
     *
     * TODO P0 照片模块上线后：回写 photo 表（描述、auto_recognized、pet_id、tags）
     */
    @Operation(summary = "照片分析结果回调")
    @PostMapping("/photo-analysis")
    public Result<Void> onPhotoAnalysis(@RequestBody @Valid PhotoAnalysisCallback callback) {
        log.info("收到照片分析回调: photoId={}, petId={}, messageId={}",
                callback.getPhotoId(), callback.getRecognizedPetId(), callback.getMessageId());
        return Result.ok();
    }
}
