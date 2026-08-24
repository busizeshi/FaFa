package com.fafa.application.dto.chat;

import com.fafa.application.dto.media.MediaDTO;

import java.util.List;

/** Agent 素材语义检索响应。 */
public record AiMediaSearchDTO(List<MediaDTO> results, boolean charged, String quotaType,
                               Integer remaining) {
}
