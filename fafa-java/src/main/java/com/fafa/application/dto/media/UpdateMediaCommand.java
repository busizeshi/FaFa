package com.fafa.application.dto.media;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 素材编辑命令（tags / takenAt）
 *
 * @author FaFa Team
 * @since 4.0
 */
public record UpdateMediaCommand(
    List<String> tags,
    LocalDateTime takenAt
) {
}
