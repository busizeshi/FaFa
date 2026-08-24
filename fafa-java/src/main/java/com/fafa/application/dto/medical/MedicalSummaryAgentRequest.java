package com.fafa.application.dto.medical;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

/** Java 传给 Python 医疗摘要编排器的内部请求。 */
public record MedicalSummaryAgentRequest(
        @JsonProperty("user_id") Long userId,
        @JsonProperty("pet_id") Long petId,
        @JsonProperty("start_date") LocalDate startDate,
        @JsonProperty("end_date") LocalDate endDate,
        @JsonProperty("include_media") boolean includeMedia,
        @JsonProperty("trace_id") String traceId) {
}
