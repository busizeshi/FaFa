package com.fafa.biz.infrastructure.persistence.repository;

import com.fafa.biz.domain.model.PasswordHasherPort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * BCrypt 哈希适配器（实现领域端口 {@link PasswordHasherPort}）。
 *
 * <p>BCrypt 自动生成随机盐并编码进哈希串，无需额外存盐列；
 * cost 取默认 10，验证码哈希同理（6 位数字熵低，尝试次数已在挑战层限制）。</p>
 *
 * @author FaFa 项目组
 * @version 1.0.0
 */
@Component
public class BCryptPasswordHasher implements PasswordHasherPort {

    /** BCrypt 编码器：线程安全，可单例复用 */
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /** {@inheritDoc} */
    @Override
    public String hash(String raw) {
        return encoder.encode(raw);
    }

    /** {@inheritDoc} */
    @Override
    public boolean matches(String raw, String hash) {
        return encoder.matches(raw, hash);
    }
}
