/**
 * admin 领域层（domain）：员工账号、角色权限、审核工单等聚合与规则。
 *
 * <p>约束与 biz 相同（架构规范 §8.1）：不依赖任何技术框架；
 * 员工角色只做粗粒度授权，资源级归属仍由 biz 判定。</p>
 *
 * @author FaFa 项目组
 * @version 1.0.0
 */
package com.fafa.admin.domain;
