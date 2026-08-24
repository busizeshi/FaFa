package com.fafa.application.service;

import com.fafa.application.dto.report.ReportContentDTO;
import com.fafa.common.BusinessException;
import com.fafa.common.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class ReportContentValidator {
    private static final java.util.List<String> FORBIDDEN_MEDICAL_WORDS = java.util.List.of(
            "诊断为", "确诊", "处方", "服用剂量", "治疗方案");

    public void validate(ReportContentDTO content) {
        if (content == null || blank(content.title()) || blank(content.summary())) {
            throw new BusinessException(ErrorCode.REPORT_CONTENT_INVALID, "报告标题和摘要不能为空");
        }
        if (content.title().length() > 100 || content.summary().length() > 2000
                || size(content.highlights()) > 8 || size(content.careSuggestions()) > 8
                || size(content.timeline()) > 20 || size(content.mediaSuggestions()) > 12
                || size(content.sourceKeys()) > 64) {
            throw new BusinessException(ErrorCode.REPORT_CONTENT_INVALID, "报告内容超出安全长度");
        }
        if (content.sourceKeys() != null && content.sourceKeys().stream()
                .anyMatch(key -> blank(key) || !allowedSourceKey(key))) {
            throw new BusinessException(ErrorCode.REPORT_CONTENT_INVALID, "报告引用来源不能为空");
        }
        String text = content.title() + content.summary() + String.join("", content.highlights() == null
                ? java.util.List.of() : content.highlights());
        if (FORBIDDEN_MEDICAL_WORDS.stream().anyMatch(text::contains)) {
            throw new BusinessException(ErrorCode.REPORT_CONTENT_INVALID, "报告包含不允许的医疗结论");
        }
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }
    private int size(java.util.Collection<?> value) { return value == null ? 0 : value.size(); }
    private boolean allowedSourceKey(String value) {
        return value.startsWith("media:") || value.startsWith("dailyStatuses:")
                || value.startsWith("weights:") || value.startsWith("feeding:")
                || value.startsWith("events:") || value.startsWith("reminders:")
                || value.startsWith("medicalVisits:") || value.startsWith("medications:")
                || value.startsWith("expenses:") || value.startsWith("healthAlerts:");
    }
}
