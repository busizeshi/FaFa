package com.fafa.domain.model.record;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 喂食记录单元测试
 *
 * @author FaFa Team
 * @since 2.0
 */
class FeedingRecordTest {

    private static final Long PET_ID = 1L;
    private static final LocalDateTime TIME = LocalDateTime.of(2026, 8, 22, 8, 30);
    private static final Long OPERATOR_ID = 100L;

    @Test
    @DisplayName("create：正常创建，单位缺省为 g")
    void create_shouldDefaultUnitToGram() {
        FeedingRecord record = FeedingRecord.create(PET_ID, TIME, FoodType.MAIN_FOOD,
                new BigDecimal("50"), null, null, null, OPERATOR_ID);

        assertThat(record.getUnit()).isEqualTo("g");
        assertThat(record.getFoodType()).isEqualTo(FoodType.MAIN_FOOD);
        assertThat(record.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("create：单位大小写归一化")
    void create_shouldNormalizeUnitCase() {
        FeedingRecord record = FeedingRecord.create(PET_ID, TIME, FoodType.CAN,
                new BigDecimal("85"), "ML", null, null, OPERATOR_ID);

        assertThat(record.getUnit()).isEqualTo("ml");
    }

    @Test
    @DisplayName("create：非法单位抛出异常")
    void create_shouldRejectInvalidUnit() {
        assertThatThrownBy(() -> FeedingRecord.create(PET_ID, TIME, FoodType.MAIN_FOOD,
                new BigDecimal("50"), "kg", null, null, OPERATOR_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("单位仅支持 g 或 ml");
    }

    @Test
    @DisplayName("create：必填字段为空抛出 NPE")
    void create_shouldRejectNullRequiredFields() {
        assertThatThrownBy(() -> FeedingRecord.create(null, TIME, FoodType.MAIN_FOOD,
                null, null, null, null, OPERATOR_ID))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("petId");

        assertThatThrownBy(() -> FeedingRecord.create(PET_ID, null, FoodType.MAIN_FOOD,
                null, null, null, null, OPERATOR_ID))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("feedingTime");

        assertThatThrownBy(() -> FeedingRecord.create(PET_ID, TIME, null,
                null, null, null, null, OPERATOR_ID))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("foodType");
    }

    @Test
    @DisplayName("create：喂食量为负抛出异常")
    void create_shouldRejectNegativeAmount() {
        assertThatThrownBy(() -> FeedingRecord.create(PET_ID, TIME, FoodType.SNACK,
                new BigDecimal("-1"), "g", null, null, OPERATOR_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("喂食量不能为负");
    }

    @Test
    @DisplayName("create：备注超长抛出异常")
    void create_shouldRejectLongNotes() {
        assertThatThrownBy(() -> FeedingRecord.create(PET_ID, TIME, FoodType.SNACK,
                null, null, null, "a".repeat(501), OPERATOR_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("备注不能超过");
    }

    @Test
    @DisplayName("update：覆盖更新字段")
    void update_shouldOverwriteFields() {
        FeedingRecord record = FeedingRecord.create(PET_ID, TIME, FoodType.MAIN_FOOD,
                new BigDecimal("50"), "g", null, null, OPERATOR_ID);

        LocalDateTime newTime = TIME.plusHours(4);
        record.update(newTime, FoodType.SNACK, new BigDecimal("10"), "g", 9L, "加餐");

        assertThat(record.getFeedingTime()).isEqualTo(newTime);
        assertThat(record.getFoodType()).isEqualTo(FoodType.SNACK);
        assertThat(record.getAmount()).isEqualByComparingTo("10");
        assertThat(record.getFoodId()).isEqualTo(9L);
        assertThat(record.getNotes()).isEqualTo("加餐");
    }

    @Test
    @DisplayName("delete：逻辑删除标记")
    void delete_shouldMarkDeleted() {
        FeedingRecord record = FeedingRecord.create(PET_ID, TIME, FoodType.MAIN_FOOD,
                null, null, null, null, OPERATOR_ID);
        record.delete();
        assertThat(record.isDeleted()).isTrue();
    }
}
