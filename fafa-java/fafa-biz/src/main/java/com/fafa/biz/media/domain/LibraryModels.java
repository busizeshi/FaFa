package com.fafa.biz.media.domain;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import com.fafa.biz.media.domain.MediaModels.*;

/** 相册、媒体整理和删除的类型契约。 */
public final class LibraryModels {
    private LibraryModels() { }
    public record MetadataChange(@NotNull @Min(1) Long version, @NotNull @Size(max=200) String title,
            @NotNull @Size(max=2000) String description,
            @NotNull @Size(max=20) List<@NotBlank @Size(max=30) String> tags, Instant capturedAt, String petId) { }
    public record AlbumCreate(@NotBlank @Size(max=80) String name,@NotNull @Size(max=1000) String description) { }
    public record AlbumChange(@NotNull @Min(1) Long version,@NotBlank @Size(max=80) String name,
                              @NotNull @Size(max=1000) String description) { }
    public record AlbumView(String id,String name,String description,long version,long itemCount) { }
    public record LibraryQuery(String petId,String albumId,MediaType type,String tag,String q,Instant from,Instant to,String cursor,int limit) { }
    public record DeletionImpact(String id,long version,long avatarReferences,long albumReferences,long businessReferences,String pendingReleaseBytes) { }
    public record DeleteMedia(@NotNull @Min(1) Long version,boolean acknowledgedReferences) { }
    public record DeletionReceipt(String id,State state,String pendingReleaseBytes) { }
    public record VersionBody(@NotNull @Min(1) Long version) { }
    public record PartRequest(@NotNull @Min(1) @Max(1000) Integer partNumber) { }
    public record PartView(int partNumber,String bytes,String etag) { }
    public record MultipartView(String sessionId,String uploadId,long partSize,List<PartView> parts) { }
    public record PartTicket(int partNumber,String uploadUrl,java.util.Map<String,String> formFields,Instant expiresAt,String bytes) { }
    public record JobView(String id,String state,int attempts,String errorCode,Instant retainUntil) { }
}
