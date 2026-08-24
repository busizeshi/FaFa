package com.fafa.application.dto.medical;

import java.util.List;
import java.util.Map;

public record MedicalSummaryDTO(Map<String, Object> pet, Map<String, String> range,
                                String overview, List<Map<String, Object>> symptomTimeline,
                                List<Map<String, Object>> medicalVisits,
                                List<Map<String, Object>> medications,
                                List<String> attentionItems, List<String> warnings,
                                String plainText, String disclaimer) {
}
