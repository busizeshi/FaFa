package com.fafa.interfaces.dto.medical;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class AttachMedicalMediaRequest {
    @NotEmpty
    @Size(max = 9)
    private List<Long> mediaIds;
    private String attachmentType = "other";
}
