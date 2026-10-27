/**
 * biz 领域层（domain）：聚合、值对象、领域规则、仓储端口与领域事件。
 *
 * <p>硬性约束（架构规范 §8.1）：</p>
 * <ul>
 *   <li>不依赖 Spring、MyBatis-Plus、Redis、RabbitMQ、Dubbo 或 HTTP DTO；</li>
 *   <li>聚合根只覆盖必须强一致的事实并负责不变量校验（如 revision 乐观锁、状态机流转）；</li>
 *   <li>仓储端口（Repository 接口）定义于此，由 infrastructure 层用 MyBatis-Plus 实现。</li>
 * </ul>
 *
 * @author FaFa 项目组
 * @version 1.0.0
 */
package com.fafa.biz.domain;
