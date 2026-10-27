package com.fafa.biz.domain.repository;

import com.fafa.biz.domain.model.User;

import java.util.Optional;

/**
 * 宠主账号仓储端口（架构规范 §8.1：由 infrastructure 的 MyBatis-Plus 适配器实现）。
 *
 * @author FaFa 项目组
 * @version 1.0.0
 */
public interface UserRepositoryPort {

    /**
     * 新建账号（邮箱唯一约束兜底并发重复注册）。
     *
     * @param user 领域对象
     * @return 写入数据库后的对象（含 id）
     */
    User save(User user);

    /**
     * @param id 主键
     * @return 账号；不存在返回 empty
     */
    Optional<User> findById(Long id);

    /**
     * @param email 小写邮箱
     * @return 账号；不存在返回 empty
     */
    Optional<User> findByEmail(String email);
}
