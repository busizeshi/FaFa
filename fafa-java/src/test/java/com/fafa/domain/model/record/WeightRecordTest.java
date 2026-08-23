package com.fafa.domain.model.record;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 体重记录单元测试
 *
 * @author FaFa Team
 * @since 2.0
 */
class WeightRecordTest {

    private static final Long PET_ID = 1L;
    private static final LocalDate DATE = LocalDate.of(2026, 8, 22);
    private static final Long OPERATOR_ID = 100L;

    @Test
    @DisplayName("create：正常创建")
    void create_shouldSucceed() {
        WeightRecord record = WeightRecord.create(PET_ID, DATE, new BigDecimal("4.35"), "备注", OPERATOR_ID);

        assertThat(record.getPetId()).isEqualTo(PET_ID);
        assertThat(record.getRecordDate()).isEqualTo(DATE);
        assertThat(record.getWeight()).isEqualByComparingTo("4.35");
        assertThat(record.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("create：体重为空抛出异常")
    void create_shouldRejectNullWeight() {
        assertThatThrownBy(() -> WeightRecord.create(PET_ID, DATE, null, null, OPERATOR_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("体重必须为正数");
    }

    @Test
    @DisplayName("create：体重为零或负抛出异常")
    void create_shouldRejectNonPositiveWeight() {
        assertThatThrownBy(() -> WeightRecord.create(PET_ID, DATE, BigDecimal.ZERO, null, OPERATOR_ID))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> WeightRecord.create(PET_ID, DATE, new BigDecimal("-0.1"), null, OPERATOR_ID))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("create：备注超长抛出异常")
    void create_shouldRejectLongNotes() {
        assertThatThrownBy(() -> WeightRecord.create(PET_ID, DATE, new BigDecimal("4.0"), "a".repeat(501), OPERATOR_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("备注不能超过");
    }

    @Test
    @DisplayName("getWeightChange：较上一条增重返回正值")
    void getWeightChange_shouldReturnPositiveDelta() {
        WeightRecord previous = WeightRecord.create(PET_ID, DATE.minusDays(30), new BigDecimal("4.00"), null, OPERATOR_ID);
        WeightRecord current = WeightRecord.create(PET_ID, DATE, new BigDecimal("4.35"), null, OPERATOR_ID);

        assertThat(current.getWeightChange(previous)).isEqualByComparingTo("0.35");
    }

    @Test
    @DisplayName("getWeightChange：较上一条减重返回负值")
    void getWeightChange_shouldReturnNegativeDelta() {
        WeightRecord previous = WeightRecord.create(PET_ID, DATE.minusDays(30), new BigDecimal("4.50"), null, OPERATOR_ID);
        WeightRecord current = WeightRecord.create(PET_ID, DATE, new BigDecimal("4.20"), null, OPERATOR_ID);

        assertThat(current.getWeightChange(previous)).isEqualByComparingTo("-0.30");
    }

    @Test
    @DisplayName("getWeightChange：首条记录返回 null")
    void getWeightChange_firstRecord_shouldReturnNull() {
        WeightRecord current = WeightRecord.create(PET_ID, DATE, new BigDecimal("4.00"), null, OPERATOR_ID);
        assertThat(current.getWeightChange(null)).isNull();
    }

    @Test
    @DisplayName("delete：逻辑删除标记")
    void delete_shouldMarkDeleted() {
        WeightRecord record = WeightRecord.create(PET_ID, DATE, new BigDecimal("4.00"), null, OPERATOR_ID);
        record.delete();
        assertThat(record.isDeleted()).isTrue();
    }
}
