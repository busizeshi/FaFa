package com.fafa.biz.media.interfaces.http;

import com.fafa.biz.media.application.*;
import com.fafa.biz.media.domain.LibraryModels.*;
import com.fafa.biz.media.domain.MediaModels.*;
import com.fafa.common.api.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** 私有素材整理与任务状态接口，所有资源均从移动身份验证所有权。 */
@RestController
@RequestMapping("/v1")
@SecurityRequirement(name="bearerAuth")
public class LibraryController {
    private final MediaLibrary library;private final MultipartUploads multipart;private final MediaJobService jobs;
    public LibraryController(MediaLibrary library,MultipartUploads multipart,MediaJobService jobs) {
        this.library=library;this.multipart=multipart;this.jobs=jobs;
    }
    @GetMapping("/media") @Operation(operationId="listMedia")
    public R<CursorPage<AssetView>> list(@RequestParam(required=false) String petId,@RequestParam(required=false) String albumId,
            @RequestParam(required=false) MediaType type,@RequestParam(required=false) String tag,@RequestParam(required=false) String q,
            @RequestParam(required=false) Instant from,@RequestParam(required=false) Instant to,@RequestParam(required=false) String cursor,
            @RequestParam(defaultValue="0") int limit) {
        return R.ok(library.list(new LibraryQuery(petId,albumId,type,tag,q,from,to,cursor,limit)));
    }
    @PatchMapping("/media/{id}") @Operation(operationId="updateMediaMetadata")
    public R<AssetView> update(@PathVariable String id,@Valid @RequestBody MetadataChange body) { return R.ok(library.update(id,body)); }
    @GetMapping("/media/{id}/deletion-impact") @Operation(operationId="previewMediaDeletion")
    public R<DeletionImpact> impact(@PathVariable String id) { return R.ok(library.impact(id)); }
    @DeleteMapping("/media/{id}") @Operation(operationId="deleteMedia")
    public R<DeletionReceipt> delete(@PathVariable String id,@Valid @RequestBody DeleteMedia body) { return R.ok(library.delete(id,body)); }
    @GetMapping("/albums") @Operation(operationId="listAlbums")
    public R<CursorPage<AlbumView>> listAlbums(@RequestParam(required=false) String cursor,@RequestParam(defaultValue="0") int limit) { return R.ok(library.albums(cursor,limit)); }
    @PostMapping("/albums") @ResponseStatus(HttpStatus.CREATED) @Operation(operationId="createAlbum")
    public R<AlbumView> create(@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody AlbumCreate body) { return R.ok(library.createAlbum(key,body)); }
    @GetMapping("/albums/{id}") @Operation(operationId="getAlbum")
    public R<AlbumView> album(@PathVariable String id) { return R.ok(library.album(id)); }
    @PatchMapping("/albums/{id}") @Operation(operationId="updateAlbum")
    public R<AlbumView> updateAlbum(@PathVariable String id,@Valid @RequestBody AlbumChange body) { return R.ok(library.updateAlbum(id,body)); }
    @DeleteMapping("/albums/{id}") @Operation(operationId="deleteAlbum")
    public R<Void> deleteAlbum(@PathVariable String id,@RequestParam long version) { library.deleteAlbum(id,version);return R.ok(null); }
    @PutMapping("/albums/{id}/items/{mediaId}") @Operation(operationId="addAlbumItem")
    public R<AlbumView> add(@PathVariable String id,@PathVariable String mediaId) { return R.ok(library.item(id,mediaId,true)); }
    @DeleteMapping("/albums/{id}/items/{mediaId}") @Operation(operationId="removeAlbumItem")
    public R<AlbumView> remove(@PathVariable String id,@PathVariable String mediaId) { return R.ok(library.item(id,mediaId,false)); }
    @GetMapping("/upload-sessions/{id}/parts") @Operation(operationId="listUploadParts")
    public R<MultipartView> parts(@PathVariable String id) { return R.ok(multipart.view(id)); }
    @PostMapping("/upload-sessions/{id}/part-policies") @Operation(operationId="createUploadPartPolicy")
    public R<PartTicket> part(@PathVariable String id,@Valid @RequestBody PartRequest body) { return R.ok(multipart.policy(id,body.partNumber())); }
    @GetMapping("/upload-sessions/{id}/processing-job") @Operation(operationId="getMediaProcessingJob")
    public R<JobView> job(@PathVariable String id) { return R.ok(jobs.view(id)); }
    @PostMapping("/upload-sessions/{id}/processing-job:retry") @Operation(operationId="retryMediaProcessingJob")
    public R<JobView> retry(@PathVariable String id) { return R.ok(jobs.retry(id)); }
}
