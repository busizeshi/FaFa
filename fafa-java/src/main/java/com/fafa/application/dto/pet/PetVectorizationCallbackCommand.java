package com.fafa.application.dto.pet;

/**
 * 宠物三视图向量化回调命令（由 Python 服务回调触发）
 *
 * @author FaFa Team
 * @since 1.0
 */
public record PetVectorizationCallbackCommand(
    Long petId,
    Boolean success,
    String message
) {
}
