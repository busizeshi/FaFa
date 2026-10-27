package com.fafa.biz.interfaces.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 注册请求（契约 AUTH /auth/register）。
 *
 * <p>协议同意由客户端内置文本提供版本号，服务端校验与当前版本一致后落库记录。</p>
 *
 * @param email         登录邮箱
 * @param password      密码（8-64 位，需含字母和数字，服务端再校验强度）
 * @param acceptedLegal 已同意的协议版本列表
 * @author FaFa 项目组
 * @version 1.0.0
 */
public record RegisterRequest(
        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        String email,

        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 64, message = "密码长度需 8-64 位")
        String password,

        @Valid
        @NotEmpty(message = "需要同意用户协议与隐私政策")
        List<LegalConsentItem> acceptedLegal) {

    /**
     * 协议同意项。
     *
     * @param documentType    文档类型：USER_AGREEMENT / PRIVACY_POLICY
     * @param documentVersion 客户端内置文本版本
     */
    public record LegalConsentItem(
            @NotBlank(message = "协议类型不能为空") String documentType,
            @NotBlank(message = "协议版本不能为空") String documentVersion) {
    }
}
