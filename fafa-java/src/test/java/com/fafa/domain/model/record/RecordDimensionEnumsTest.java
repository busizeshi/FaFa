package com.fafa.domain.model.record;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 记录维度枚举单元测试
 *
 * 覆盖六维度 + FoodType + RecordDimension 的 fromCode / 正常异常语义。
 *
 * @author FaFa Team
 * @since 2.0
 */
class RecordDimensionEnumsTest {

    @Test
    @DisplayName("Appetite：fromCode 与正常/异常语义")
    void appetite() {
        assertThat(Appetite.fromCode("excellent")).isEqualTo(Appetite.EXCELLENT);
        assertThat(Appetite.fromCode("NORMAL")).isEqualTo(Appetite.NORMAL);
        assertThatThrownBy(() -> Appetite.fromCode("unknown"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(Appetite.NORMAL.isNormal()).isTrue();
        assertThat(Appetite.EXCELLENT.isNormal()).isFalse();
        assertThat(Appetite.EXCELLENT.isAbnormal()).isFalse();
        assertThat(Appetite.DECREASED.isAbnormal()).isTrue();
        assertThat(Appetite.REFUSED.isAbnormal()).isTrue();
    }

    @Test
    @DisplayName("Energy：fromCode 与正常/异常语义")
    void energy() {
        assertThat(Energy.fromCode("active")).isEqualTo(Energy.ACTIVE);
        assertThatThrownBy(() -> Energy.fromCode("bad"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(Energy.NORMAL.isNormal()).isTrue();
        assertThat(Energy.ACTIVE.isAbnormal()).isFalse();
        assertThat(Energy.LETHARGIC.isAbnormal()).isTrue();
    }

    @Test
    @DisplayName("WaterIntake：逃生口档位既非正常也非异常")
    void waterIntake() {
        assertThat(WaterIntake.fromCode("not_noticed")).isEqualTo(WaterIntake.NOT_NOTICED);

        assertThat(WaterIntake.NORMAL.isNormal()).isTrue();
        assertThat(WaterIntake.MORE.isAbnormal()).isTrue();
        assertThat(WaterIntake.LESS.isAbnormal()).isTrue();
        assertThat(WaterIntake.NOT_NOTICED.isNormal()).isFalse();
        assertThat(WaterIntake.NOT_NOTICED.isAbnormal()).isFalse();
    }

    @Test
    @DisplayName("Defecation：逃生口档位既非正常也非异常")
    void defecation() {
        assertThat(Defecation.fromCode("diarrhea")).isEqualTo(Defecation.DIARRHEA);

        assertThat(Defecation.NORMAL.isNormal()).isTrue();
        assertThat(Defecation.SOFT.isAbnormal()).isTrue();
        assertThat(Defecation.DIARRHEA.isAbnormal()).isTrue();
        assertThat(Defecation.CONSTIPATION.isAbnormal()).isTrue();
        assertThat(Defecation.NOT_SEEN.isNormal()).isFalse();
        assertThat(Defecation.NOT_SEEN.isAbnormal()).isFalse();
    }

    @Test
    @DisplayName("Vomiting：none 为正常档，once/multiple 为异常档")
    void vomiting() {
        assertThat(Vomiting.fromCode("none")).isEqualTo(Vomiting.NONE);

        assertThat(Vomiting.NONE.isNormal()).isTrue();
        assertThat(Vomiting.NONE.isAbnormal()).isFalse();
        assertThat(Vomiting.ONCE.isAbnormal()).isTrue();
        assertThat(Vomiting.MULTIPLE.isAbnormal()).isTrue();
    }

    @Test
    @DisplayName("Urination：逃生口档位既非正常也非异常")
    void urination() {
        assertThat(Urination.fromCode("abnormal")).isEqualTo(Urination.ABNORMAL);

        assertThat(Urination.NORMAL.isNormal()).isTrue();
        assertThat(Urination.ABNORMAL.isAbnormal()).isTrue();
        assertThat(Urination.NOT_SEEN.isNormal()).isFalse();
        assertThat(Urination.NOT_SEEN.isAbnormal()).isFalse();
    }

    @Test
    @DisplayName("FoodType：fromCode 大小写不敏感")
    void foodType() {
        assertThat(FoodType.fromCode("main_food")).isEqualTo(FoodType.MAIN_FOOD);
        assertThat(FoodType.fromCode("SNACK")).isEqualTo(FoodType.SNACK);
        assertThat(FoodType.fromCode("can")).isEqualTo(FoodType.CAN);
        assertThat(FoodType.fromCode("canned")).isEqualTo(FoodType.CAN);
        assertThat(FoodType.fromCode("other")).isEqualTo(FoodType.OTHER);
        assertThatThrownBy(() -> FoodType.fromCode("raw"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("RecordDimension：fromCode 与 code/label")
    void recordDimension() {
        assertThat(RecordDimension.fromCode("appetite")).isEqualTo(RecordDimension.APPETITE);
        assertThat(RecordDimension.fromCode("WATER_INTAKE")).isEqualTo(RecordDimension.WATER_INTAKE);
        assertThatThrownBy(() -> RecordDimension.fromCode("weight"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(RecordDimension.URINATION.getCode()).isEqualTo("urination");
        assertThat(RecordDimension.URINATION.getLabel()).isEqualTo("排尿");
        assertThat(RecordDimension.values()).hasSize(6);
    }
}
