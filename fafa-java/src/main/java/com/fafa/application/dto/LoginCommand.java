package com.fafa.application.dto;

/**
 * 微信登录命令
 *
 * @param code 小程序 wx.login() 获取的临时凭证
 * @author FaFa Team
 * @since 1.0
 */
public record LoginCommand(String code) {
}
