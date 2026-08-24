package com.fafa.application.dto.medical;

import java.time.LocalDate;

public record MedicalTimelineItemDTO(String type, Long sourceId, LocalDate eventDate,
                                     String title, String summary, String status) {
}
