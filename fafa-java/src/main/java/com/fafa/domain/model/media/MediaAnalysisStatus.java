package com.fafa.domain.model.media;

import lombok.Getter;

/**
 * 媒体 AI 分析状态
 *
 * 状态机（回调驱动，见 phase4-implementation-plan.md 2.3）：
 * - photo/video：pending → completed / failed（processing 预留，本阶段不流转）
 * - 已终态（completed/failed/skipped）时重复回调幂等跳过
 *
 * @author FaFa Team
 * @since 4.0
 */
@Getter
public enum MediaAnalysisStatus {

    PENDING("pending", "待分析"),
    PROCESSING("processing", "分析中"),
    COMPLETED("completed", "已完成"),
    FAILED("failed", "失败"),
    SKIPPED("skipped", "跳过");

    private final String code;
    private final String label;

    MediaAnalysisStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static MediaAnalysisStatus fromCode(String code) {
        for (MediaAnalysisStatus value : values()) {
            if (value.code.equalsIgnoreCase(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Invalid media analysis status: " + code);
    }

    /**
     * 是否为终态（completed/failed/skipped）
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == SKIPPED;
    }
}
