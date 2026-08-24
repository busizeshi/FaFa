package com.fafa.domain.model.health;

/** 预警触发事实的最小引用，不保存原始病历正文。 */
public record HealthEvidence(String sourceType, String sourceId, String field, String value,
                             String observedDate) {
}
