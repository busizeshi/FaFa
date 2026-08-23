package com.fafa.application.dto.event;

import java.time.LocalDate;
import java.util.List;

/**
 * 手动编辑成长档案事件命令。
 *
 * @author FaFa Team
 * @since 4.0
 */
public record UpdateEventCommand(
        String eventType,
        String title,
        String content,
        LocalDate eventDate,
        List<String> materialUrls,
        List<String> tags
) {
}
