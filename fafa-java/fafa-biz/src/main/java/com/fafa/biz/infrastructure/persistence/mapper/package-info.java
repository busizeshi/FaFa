/**
 * MyBatis-Plus Mapper 接口包。
 *
 * <p>约定（架构规范 §3 / §8.2）：</p>
 * <ul>
 *   <li>Mapper/DO 只存在于本包（infrastructure.persistence.mapper / entity），
 *       禁止泄露到 interfaces/application/domain 层；</li>
 *   <li>常规 CRUD 与分页使用 MyBatis-Plus 能力；复杂查询写显式 SQL（XML 或注解），
 *       动态排序只允许字段白名单；</li>
 *   <li>DO 与领域对象在仓储实现中显式映射，不共享同一类型。</li>
 * </ul>
 *
 * @author FaFa 项目组
 * @version 1.0.0
 */
package com.fafa.biz.infrastructure.persistence.mapper;
