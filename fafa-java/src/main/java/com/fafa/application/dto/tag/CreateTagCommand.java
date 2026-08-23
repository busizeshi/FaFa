package com.fafa.application.dto.tag;

/**
 * 创建标签命令
 *
 * @author FaFa Team
 * @since 4.0
 */
public record CreateTagCommand(
    String name,
    String category
) {
}
