package com.fafa.domain.model.pet;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 宠物三视图向量化状态
 * <p>
 * 与 pet.vectorization_status 列对应：
 * - 派发 Python 向量化前落库 PENDING（记录状态先于派发）；
 * - Python 回调成功置 COMPLETED，失败置 FAILED（失败可由重试翻转回 COMPLETED）；
 * - 存量数据 DEFAULT 'completed'，视为已完成向量化。
 *
 * @author FaFa Team
 * @since 4.0
 */
@Getter
@RequiredArgsConstructor
public enum PetVectorizationStatus {

    PENDING("pending", "待向量化"),
    PROCESSING("processing", "向量化中"),
    COMPLETED("completed", "已完成"),
    FAILED("failed", "向量化失败");

    private final String code;
    private final String description;

    public static PetVectorizationStatus fromCode(String code) {
        if (code == null || code.isBlank()) {
            // 理论上列为 NOT NULL DEFAULT 'completed'，此处兜底视为已完成
            return COMPLETED;
        }
        for (PetVectorizationStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的向量化状态: " + code);
    }

    /**
     * 终态判断：COMPLETED / FAILED 为终态
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED;
    }
}
