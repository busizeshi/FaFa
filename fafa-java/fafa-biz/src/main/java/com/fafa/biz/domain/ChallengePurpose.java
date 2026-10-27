package com.fafa.biz.domain;

/**
 * 邮箱验证挑战用途（架构规范 §7-M00）。
 *
 * @author FaFa 项目组
 * @version 1.0.0
 */
public enum ChallengePurpose {

    /** 注册邮箱验证：verify 成功后激活账号并签发会话 */
    REGISTER_VERIFY,

    /** 找回密码第一步：verify 成功后签发一次性 resetCredential（存 Redis，短时效） */
    PASSWORD_RESET
}
