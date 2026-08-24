package com.fafa.application.dto.report;

import java.util.List;

public record ReportContentDTO(String title, String summary, List<String> highlights,
                               List<String> careSuggestions, List<ReportTimelineItem> timeline,
                               List<ReportMediaSuggestion> mediaSuggestions,
                               List<String> sourceKeys) {
    public record ReportTimelineItem(String date, String title, String detail, String sourceKey) {}
    public record ReportMediaSuggestion(Long mediaId, String reason, String sourceKey) {}
}
