package com.fafa.biz.media.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.*;

/** 图片大小、解码成本及上传有效期的服务端上限。 */
@Validated
@ConfigurationProperties("fafa.media")
public record MediaPolicy(@Min(1) @Max(20971520) long maxImageBytes,
        @Min(1) @Max(40000000) long maxPixels, @Min(32) @Max(2048) int previewEdge,
        @Min(30) @Max(900) int uploadSeconds, @Min(10) @Max(120) int accessSeconds,
        boolean cleanupEnabled) {
    public MediaPolicy {
        if(maxImageBytes==0) maxImageBytes=20971520;
        if(maxPixels==0) maxPixels=16000000;
        if(previewEdge==0) previewEdge=1024;
        if(uploadSeconds==0) uploadSeconds=300;
        if(accessSeconds==0) accessSeconds=60;
    }
}
