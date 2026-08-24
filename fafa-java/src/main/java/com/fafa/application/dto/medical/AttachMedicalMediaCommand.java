package com.fafa.application.dto.medical;

import java.util.List;

public record AttachMedicalMediaCommand(List<Long> mediaIds, String attachmentType) {
}
