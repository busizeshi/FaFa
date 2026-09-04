package com.fafa.application.dto.quota;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 配额与用量DTO（「我的」页用量进度条数据源）
 *
 * @param storageUsed   存储已用（字节）
 * @param storageLimit  存储上限（字节）
 * @param aiSearchUsed  本月 AI 语义检索已用次数
 * @param aiSearchLimit 本月 AI 语义检索上限（VIP 为 -1 表示不限）
 * @param foodRecognitionUsed 本月食品包装识别已用次数
 * @param foodRecognitionLimit 本月食品包装识别上限（VIP 为 -1 表示不限）
 * @param isVip         是否有效会员
 * @author FaFa Team
 * @since 4.0
 */
@Schema(description = "配额与用量")
public record QuotaDTO(
    @Schema(description = "存储已使用量（字节）", example = "1048576")
    long storageUsed,
    @Schema(description = "存储上限（字节）", example = "1073741824")
    long storageLimit,
    @Schema(description = "本月 AI 语义检索已使用次数", example = "3")
    long aiSearchUsed,
    @Schema(description = "本月 AI 语义检索次数上限；-1 表示不限", example = "100")
    long aiSearchLimit,
    @Schema(description = "本月食品包装识别已使用次数", example = "1")
    long foodRecognitionUsed,
    @Schema(description = "本月食品包装识别次数上限；-1 表示不限", example = "5")
    long foodRecognitionLimit,
    @Schema(description = "是否为有效会员", example = "false")
    boolean isVip,
    @Schema(description = "本月周期报告内容生成已使用次数", example = "0")
    long reportContentUsed,
    @Schema(description = "本月周期报告内容生成次数上限；-1 表示不限", example = "10")
    long reportContentLimit
) {
    public QuotaDTO(long storageUsed, long storageLimit, long aiSearchUsed, long aiSearchLimit,
                    long foodRecognitionUsed, long foodRecognitionLimit, boolean isVip) {
        this(storageUsed, storageLimit, aiSearchUsed, aiSearchLimit, foodRecognitionUsed,
                foodRecognitionLimit, isVip, 0, -1);
    }
}
