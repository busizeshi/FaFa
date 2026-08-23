package com.fafa.application.dto.media;

/**
 * 确认/修正归属命令
 *
 * @param petId 新归属宠物；null 表示清除归属
 * @author FaFa Team
 * @since 4.0
 */
public record ConfirmPetCommand(
    Long petId
) {
}
