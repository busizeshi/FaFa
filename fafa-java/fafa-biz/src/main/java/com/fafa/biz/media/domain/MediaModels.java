package com.fafa.biz.media.domain;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.Map;

/** 图片上传和访问的类型契约；字节以十进制字符串传输。 */
public final class MediaModels {
    private MediaModels() { }
    public enum State { INITIATED, PROCESSING, READY, CANCELLED, FAILED, EXPIRED, DELETION_PENDING, DELETED }
    public enum MediaType { IMAGE, VIDEO, DOCUMENT }
    public enum UploadMode { SINGLE, MULTIPART }
    public enum Variant { PREVIEW, PLAYBACK, DOCUMENT }
    public record CreateUpload(@NotBlank String petId, @NotBlank @Size(max=200) String fileName,
            @NotBlank @Pattern(regexp="image/(jpeg|png)|application/pdf|video/mp4") String mime,
            @NotBlank @Pattern(regexp="[1-9][0-9]{0,8}") String bytes,
            @NotBlank @Pattern(regexp="[a-f0-9]{64}") String sha256, UploadMode uploadMode) {
        public UploadMode mode() { return uploadMode==null?UploadMode.SINGLE:uploadMode; }
    }
    public record UploadView(String id, String petId, State state, String bytes, Instant expiresAt, String assetId) { }
    public record UploadPolicy(String sessionId, String uploadUrl, Map<String,String> formFields, Instant expiresAt) { }
    public record AssetView(String id, String petId, String bytes, String mime, String sha256, int width, int height,
            MediaType mediaType, State state, long version, String title, String description,
            java.util.List<String> tags, Instant capturedAt, String capturedAtSource,
            Long durationMillis, Integer pageCount, String chargedBytes) { }
    public record AvatarChange(@NotBlank String mediaId, @NotNull @Min(1) Long version) { }
    public record AvatarView(String petId, String mediaId, long version) { }
    public record AccessTicket(String url, Instant expiresAt) { }
    public record Session(String id, String owner, String petId, String mime, long bytes, String sha256,
                          Instant expiresAt, State state, String assetId, boolean cleaned, UploadMode uploadMode, String multipartId, long reservedBytes) {
        public UploadView view() { return new UploadView(id, petId, state, Long.toString(bytes), expiresAt, assetId); }
        public String stagingKey() { return "staging/" + owner + "/" + id; }
        public String originalKey() { return "private/" + owner + "/" + id + "/original"; }
        public String previewKey() { return "private/" + owner + "/" + id + "/preview.png"; }
        public String playbackKey() { return "private/" + owner + "/" + id + "/playback.mp4"; }
        public String reservationId() { return "media:" + id; }
    }
}
