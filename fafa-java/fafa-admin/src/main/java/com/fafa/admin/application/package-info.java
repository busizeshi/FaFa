/**
 * admin 应用层（application）：运营用例编排、员工会话上下文与审计记录。
 *
 * <p>职责（架构规范 §4.2 / §8.1）：</p>
 * <ul>
 *   <li>每个方法对应一个运营用例（审核通过/驳回、处置举报、处理申诉等）；</li>
 *   <li>操作审计与业务写操作同事务保存；敏感字段只记录脱敏摘要；</li>
 *   <li>调用 biz 时优先评估领域事件，确需同步取得结果的用例使用 Dubbo。</li>
 * </ul>
 *
 * @author FaFa 项目组
 * @version 1.0.0
 */
package com.fafa.admin.application;
