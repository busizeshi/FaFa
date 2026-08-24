package com.fafa.domain.model.expense;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Objects;

/** 金额值对象，统一金额精度，避免 double 参与费用计算。 */
public record Money(BigDecimal amount, String currency) {

    public Money {
        Objects.requireNonNull(amount, "金额不能为空");
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("金额必须大于0");
        }
        try {
            amount = amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("金额最多保留两位小数", ex);
        }
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("货币不能为空");
        }
        currency = currency.trim().toUpperCase(Locale.ROOT);
    }
}
