package com.fafa.interfaces.dto.food;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/** 更新换粮反应请求。 */
@Data
public class UpdateFoodReactionRequest {
    @NotNull private LocalDate date;
    @NotBlank private String reaction;
    @Size(max = 500) private String note;
}
