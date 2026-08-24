package com.fafa.application.dto.feeding;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 指定日期的喂食清单。 */
public record FeedingChecklistDTO(Long planId, LocalDate date, String status,
                                  List<ItemDTO> items, int completedCount, int totalCount) {
    public record ItemDTO(Long planItemId, String itemKey, Long foodId, String foodName,
                          LocalDateTime plannedAt, BigDecimal amount, String unit,
                          String completionStatus, Long feedingRecordId, LocalDateTime actualFeedingTime) {
    }
}
