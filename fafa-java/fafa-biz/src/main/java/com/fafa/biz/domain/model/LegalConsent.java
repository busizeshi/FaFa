package com.fafa.biz.domain.model;

import java.time.LocalDateTime;

/**
 * 协议同意记录值对象（追加式审计数据，无独立聚合）。
 *
 * @param documentType    文档类型：USER_AGREEMENT / PRIVACY_POLICY
 * @param documentVersion 同意的版本号（客户端内置文本版本）
 * @param consentedAt     同意时间（UTC）
 * @author FaFa 项目组
 * @version 1.0.0
 */
public record LegalConsent(String documentType, String documentVersion, LocalDateTime consentedAt) {
}
