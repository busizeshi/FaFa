package com.fafa.common.error;

/** 不回显提交字段值的校验错误。
 *
 * @param field 无效字段的名称或路径
 * @param message 可安全返回给客户端的校验消息
 */
public record FieldViolation(String field, String code, String message) {
    public FieldViolation(String field, String message) {
        this(field, "INVALID_VALUE", message);
    }
}
