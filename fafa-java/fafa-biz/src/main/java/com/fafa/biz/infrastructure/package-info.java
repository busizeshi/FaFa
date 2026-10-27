/**
 * biz 基础设施层（infrastructure）：外部技术适配器。
 *
 * <p>包含（架构规范 §5.1 / §8.1）：</p>
 * <ul>
 *   <li>{@code persistence}：MyBatis-Plus Mapper / DO 与仓储端口实现，领域对象与 DO 显式映射；</li>
 *   <li>{@code config}：MyBatis-Plus、OpenAPI、Jackson 等框架装配；</li>
 *   <li>{@code oss} / {@code mq} / {@code cache} / {@code client}：OSS 签名、Outbox 发布、
 *       Redis 缓存、对 Python AI 服务的内网客户端等适配器（随模块开发补充）。</li>
 * </ul>
 *
 * <p>注意：本层实现 domain 层定义的端口，方向永远是 infrastructure → domain，
 * 领域对象不得反向依赖本层任何类。</p>
 *
 * @author FaFa 项目组
 * @version 1.0.0
 */
package com.fafa.biz.infrastructure;
