package com.fafa.application.dto.media;

import java.time.LocalDate;
import java.util.List;

/**
 * 语义检索命令
 *
 * @author FaFa Team
 * @since 4.0
 */
public record SemanticSearchCommand(
    String query,
    Long petId,
    LocalDate startDate,
    LocalDate endDate,
    Integer limit
) {
}
