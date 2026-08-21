package com.fafa.interfaces.controller;

import com.fafa.interfaces.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Python 服务回调接口
 *
 * Python 处理完异步 AI 任务后，通过此组接口回写业务状态。
 * 由 InternalTokenInterceptor 校验 X-Internal-Token。
 */
@Slf4j
@Tag(name = "内部回调", description = "仅限 Python AI 服务调用")
@RestController
@RequestMapping("/api/callback")
public class CallbackController {

    /**
     * 照片分析结果回调
     *
     * TODO: 业务字段（photoId/description/recognizedPetId/tags）落库，M5 链路联调时实现
     */
    @Operation(summary = "照片分析结果回调")
    @PostMapping("/photo-analysis")
    public Result<Void> photoAnalysis(@RequestBody Map<String, Object> callback) {
        log.info("收到照片分析回调: photoId={}, keys={}",
                callback.get("photoId"), callback.keySet());
        return Result.ok();
    }
}
