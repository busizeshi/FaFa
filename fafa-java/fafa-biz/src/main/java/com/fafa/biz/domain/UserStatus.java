package com.fafa.biz.domain;

/**
 * 宠主账号状态机（架构规范 §8.1：状态流转规则由领域层定义）。
 *
 * <p>流转：PENDING_VERIFICATION --verify--&gt; ACTIVE --suspend/resume--&gt; SUSPENDED。
 * 未验证账号不能登录业务 API；停用账号登录时返回明确停用错误。</p>
 *
 * @author FaFa 项目组
 * @version 1.0.0
 */
public enum UserStatus {

    /** 已注册但未完成邮箱验证：不可登录 */
    PENDING_VERIFICATION,

    /** 正常可用 */
    ACTIVE,

    /** 被运营停用：登录被拒绝并提示联系客服 */
    SUSPENDED
}
