package com.fafa.biz.usage.domain;

import com.fafa.common.error.CommonErrorCode;
import com.fafa.common.error.FafaException;

/** 字节额度采用整数；HTTP 和 RPC 契约使用十进制字符串避免客户端精度损失。 */
public final class QuotaAmounts {
    public static final String STORAGE = "STORAGE_BYTES";
    private QuotaAmounts() { }
    public static long positive(String input) {
        try {
            if (input == null || !input.matches("[1-9][0-9]{0,18}")) invalid();
            return Long.parseLong(input);
        } catch (NumberFormatException exception) { throw new FafaException(CommonErrorCode.INVALID_ARGUMENT, "额度必须为有效的正整数字节数"); }
    }
    public static void bucket(String bucket) { if (!STORAGE.equals(bucket)) invalid(); }
    public static void operation(String operation) {
        if (operation == null || !operation.matches("[A-Za-z0-9._:-]{1,128}")) invalid();
    }
    public static void reason(String reason) { if (reason == null || reason.isBlank() || reason.length() > 500) invalid(); }
    public static long add(long current, long amount) {
        try { return Math.addExact(current, amount); }
        catch (ArithmeticException exception) { throw new FafaException(CommonErrorCode.INVALID_ARGUMENT, "额度超出可支持范围"); }
    }
    private static void invalid() { throw new FafaException(CommonErrorCode.INVALID_ARGUMENT, "额度、操作标识或原因无效"); }
}
