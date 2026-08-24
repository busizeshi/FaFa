package com.fafa.application.dto.medical;

import com.fafa.application.dto.media.MediaDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record MedicalVisitDTO(Long id, Long petId, LocalDate visitDate, String hospitalName,
                              String doctorName, String chiefComplaint, String diagnosis,
                              String prescription, String treatment, BigDecimal totalCost,
                              String notes, List<MediaDTO> attachments,
                              LocalDateTime createdAt, LocalDateTime updatedAt) {
}
