package com.fafa.biz.domain.model;

/**
 * 密码/验证码哈希端口（领域层定义，infrastructure 以 BCrypt 实现）。
 *
 * <p>统一用于用户密码哈希与邮箱验证码哈希：验证码 6 位数字熵有限，
 * 必须哈希存储并限制尝试次数，防止数据库泄露后直接读码。</p>
 *
 * @author FaFa 项目组
 * @version 1.0.0
 */
public interface PasswordHasherPort {

    /**
     * @param raw 明文（密码或验证码）
     * @return 带盐哈希（如 bcrypt：{$2a$10$...}）
     */
    String hash(String raw);

    /**
     * @param raw  明文
     * @param hash 已存哈希
     * @return 是否匹配
     */
    boolean matches(String raw, String hash);
}
