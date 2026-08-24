package com.fafa.domain.model.expense;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExpenseRecordTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 23);

    @Test
    void manualRecord_shouldNormalizeMoneyAndBeEditable() {
        ExpenseRecord record = ExpenseRecord.createManual(3L, 1L, ExpenseCategory.FOOD,
                new Money(new BigDecimal("19.9"), "cny"), TODAY, " 猫粮 ", " 备注 ",
                null, null, null, TODAY);

        assertThat(record.getMoney().amount()).isEqualByComparingTo("19.90");
        assertThat(record.getMoney().currency()).isEqualTo("CNY");
        assertThat(record.getTitle()).isEqualTo("猫粮");
        assertThat(record.isEditableByUser()).isTrue();
    }

    @Test
    void manualRecord_shouldRejectFutureDateAndExcessivePrecision() {
        assertThatThrownBy(() -> new Money(new BigDecimal("1.001"), "CNY"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ExpenseRecord.createManual(3L, 1L, ExpenseCategory.OTHER,
                new Money(new BigDecimal("1.00"), "CNY"), TODAY.plusDays(1), "其他", null,
                null, null, null, TODAY)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void medicalRecord_shouldBeImmutableForUser() {
        ExpenseRecord record = ExpenseRecord.createFromMedicalVisit(3L, 1L, 99L,
                new Money(new BigDecimal("200.00"), "CNY"), TODAY, "就诊费用", null, TODAY);

        assertThat(record.isEditableByUser()).isFalse();
        assertThatThrownBy(record::delete).isInstanceOf(IllegalStateException.class);
    }
}
