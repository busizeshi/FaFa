package com.fafa.biz.media.application;
import jakarta.validation.constraints.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** 文件处理、重试和存储成本的统一上限；不得由请求覆盖。 */
@Validated
@ConfigurationProperties("fafa.media.processing")
public record ProcessingPolicy(@Min(1) @Max(20971520) long maxDocumentBytes,
        @Min(1) @Max(209715200) long maxVideoBytes,@Min(1) @Max(67108864) long maxPlaybackBytes,
        @Min(1) @Max(180) int maxVideoSeconds,@Min(1) @Max(200) int maxPdfPages,
        @Min(30) @Max(240) int processSeconds,@Min(600) int leaseSeconds,
        @Min(1) @Max(5) int maxAttempts,@Min(3600) int retentionSeconds,
        String ffmpeg,String ffprobe) {
    public ProcessingPolicy {
        if(maxDocumentBytes==0) maxDocumentBytes=20971520;
        if(maxVideoBytes==0) maxVideoBytes=209715200;
        if(maxPlaybackBytes==0) maxPlaybackBytes=67108864;
        if(maxVideoSeconds==0) maxVideoSeconds=180;
        if(maxPdfPages==0) maxPdfPages=200;
        if(processSeconds==0) processSeconds=180;
        if(leaseSeconds==0) leaseSeconds=600;
        if(maxAttempts==0) maxAttempts=3;
        if(retentionSeconds==0) retentionSeconds=86400;
        if(ffmpeg==null||ffmpeg.isBlank()) ffmpeg="ffmpeg";
        if(ffprobe==null||ffprobe.isBlank()) ffprobe="ffprobe";
    }
}
