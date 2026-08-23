package com.fafa.application.dto.quota;

/**
 * 配额与用量DTO（「我的」页用量进度条数据源）
 *
 * @param storageUsed   存储已用（字节）
 * @param storageLimit  存储上限（字节）
 * @param aiSearchUsed  本月 AI 语义检索已用次数
 * @param aiSearchLimit 本月 AI 语义检索上限（VIP 为 -1 表示不限）
 * @param isVip         是否有效会员
 * @author FaFa Team
 * @since 4.0
 */
public record QuotaDTO(
    long storageUsed,
    long storageLimit,
    long aiSearchUsed,
    long aiSearchLimit,
    boolean isVip
) {
}
