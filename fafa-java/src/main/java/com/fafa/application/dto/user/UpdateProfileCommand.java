package com.fafa.application.dto.user;

/**
 * 更新用户资料命令
 *
 * @author FaFa Team
 * @since 1.0
 */
public record UpdateProfileCommand(
    String nickname,
    String phone,
    String gender,
    Integer age,
    String city
) {
}
