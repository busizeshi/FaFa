package com.fafa.biz.media.interfaces.http;

import com.fafa.biz.media.application.MediaService;
import com.fafa.biz.media.domain.MediaModels.*;
import com.fafa.common.api.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** 仅暴露媒体会话与素材身份，不接受客户端提供存储对象键。 */
@RestController
@RequestMapping("/v1")
@SecurityRequirement(name="bearerAuth")
public class MediaController {
    private final MediaService media;
    public MediaController(MediaService media) { this.media=media; }
    @PostMapping("/upload-sessions") @ResponseStatus(HttpStatus.CREATED) @Operation(operationId="createUploadSession")
    public R<UploadView> create(@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody CreateUpload body) {
        return R.ok(media.create(key,body));
    }
    @GetMapping("/upload-sessions/{id}") @Operation(operationId="getUploadSession")
    public R<UploadView> get(@PathVariable String id) { return R.ok(media.get(id)); }
    @PostMapping("/upload-sessions/{id}/upload-policy") @Operation(operationId="createUploadPolicy")
    public R<UploadPolicy> policy(@PathVariable String id) { return R.ok(media.uploadPolicy(id)); }
    @PostMapping("/upload-sessions/{id}:complete") @Operation(operationId="completeUpload")
    public R<UploadView> complete(@PathVariable String id) { return R.ok(media.complete(id)); }
    @PostMapping("/upload-sessions/{id}:cancel") @Operation(operationId="cancelUpload")
    public R<UploadView> cancel(@PathVariable String id) { return R.ok(media.cancel(id)); }
    @GetMapping("/pets/{id}/avatar") @Operation(operationId="getPetAvatar")
    public R<AvatarView> avatar(@PathVariable String id) { return R.ok(media.avatar(id)); }
    @PutMapping("/pets/{id}/avatar") @Operation(operationId="setPetAvatar")
    public R<AvatarView> avatar(@PathVariable String id,@RequestHeader("Idempotency-Key") String key,
            @Valid @RequestBody AvatarChange body) { return R.ok(media.bindAvatar(id,key,body)); }
    @GetMapping("/media/{id}") @Operation(operationId="getMedia")
    public R<AssetView> asset(@PathVariable String id) { return R.ok(media.asset(id)); }
    @PostMapping("/media/{id}/access-tickets") @Operation(operationId="createMediaAccessTicket")
    public R<AccessTicket> access(@PathVariable String id,@RequestParam(defaultValue="PREVIEW") Variant variant) { return R.ok(media.access(id,variant)); }
}
