package com.fafa.application.dto.chat;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Agent 素材语义检索请求。 */
public record AiMediaSearchRequest(@NotNull @JsonAlias("user_id") Long userId,
                                   @NotBlank String query,
                                   @JsonAlias("pet_id") Long petId,
                                   @JsonAlias("start_date") LocalDate startDate,
                                   @JsonAlias("end_date") LocalDate endDate,
                                   Integer limit) {
}
