package com.fafa.domain.model.record;

import com.fafa.domain.model.pet.PetType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 每日状态聚合根单元测试
 *
 * @author FaFa Team
 * @since 2.0
 */
class DailyStatusTest {

    private static final Long PET_ID = 1L;
    private static final LocalDate DATE = LocalDate.of(2026, 8, 22);
    private static final Long OPERATOR_ID = 100L;

    @Test
    @DisplayName("create：正常创建并默认呕吐为无")
    void create_shouldDefaultVomitingToNone() {
        DailyStatus status = DailyStatus.create(PET_ID, DATE,
                Appetite.NORMAL, Energy.NORMAL, WaterIntake.NORMAL,
                Defecation.NORMAL, null, Urination.NORMAL,
                null, null, null, OPERATOR_ID);

        assertThat(status.getVomiting()).isEqualTo(Vomiting.NONE);
        assertThat(status.getPetId()).isEqualTo(PET_ID);
        assertThat(status.getRecordDate()).isEqualTo(DATE);
    }

    @Test
    @DisplayName("create：petId 为空抛出 NPE")
    void create_shouldRejectNullPetId() {
        assertThatThrownBy(() -> DailyStatus.create(null, DATE,
                Appetite.NORMAL, Energy.NORMAL, WaterIntake.NORMAL,
                Defecation.NORMAL, Vomiting.NONE, Urination.NORMAL,
                null, null, null, OPERATOR_ID))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("petId");
    }

    @Test
    @DisplayName("create：进食量为负抛出异常")
    void create_shouldRejectNegativeAppetiteAmount() {
        assertThatThrownBy(() -> DailyStatus.create(PET_ID, DATE,
                Appetite.NORMAL, Energy.NORMAL, WaterIntake.NORMAL,
                Defecation.NORMAL, Vomiting.NONE, Urination.NORMAL,
                new BigDecimal("-1"), null, null, OPERATOR_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("进食克数不能为负");
    }

    @Test
    @DisplayName("create：饮水量为负抛出异常")
    void create_shouldRejectNegativeWaterAmount() {
        assertThatThrownBy(() -> DailyStatus.create(PET_ID, DATE,
                Appetite.NORMAL, Energy.NORMAL, WaterIntake.NORMAL,
                Defecation.NORMAL, Vomiting.NONE, Urination.NORMAL,
                null, new BigDecimal("-1"), null, OPERATOR_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("饮水毫升数不能为负");
    }

    @Test
    @DisplayName("create：备注超过500字符抛出异常")
    void create_shouldRejectNotesOver500Chars() {
        String longNotes = "a".repeat(501);
        assertThatThrownBy(() -> DailyStatus.create(PET_ID, DATE,
                Appetite.NORMAL, Energy.NORMAL, WaterIntake.NORMAL,
                Defecation.NORMAL, Vomiting.NONE, Urination.NORMAL,
                null, null, longNotes, OPERATOR_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("备注不能超过");
    }

    @Test
    @DisplayName("createNormal：猫填排尿正常档")
    void createNormal_cat_shouldFillUrinationNormal() {
        DailyStatus status = DailyStatus.createNormal(PET_ID, DATE, PetType.CAT, OPERATOR_ID);

        assertThat(status.getAppetite()).isEqualTo(Appetite.NORMAL);
        assertThat(status.getEnergy()).isEqualTo(Energy.NORMAL);
        assertThat(status.getWaterIntake()).isEqualTo(WaterIntake.NORMAL);
        assertThat(status.getDefecation()).isEqualTo(Defecation.NORMAL);
        assertThat(status.getVomiting()).isEqualTo(Vomiting.NONE);
        assertThat(status.getUrination()).isEqualTo(Urination.NORMAL);
        assertThat(status.isAllNormal()).isTrue();
    }

    @Test
    @DisplayName("createNormal：狗不填排尿（物种差异）")
    void createNormal_dog_shouldLeaveUrinationNull() {
        DailyStatus status = DailyStatus.createNormal(PET_ID, DATE, PetType.DOG, OPERATOR_ID);

        assertThat(status.getUrination()).isNull();
        assertThat(status.isAllNormal()).isTrue();
    }

    @Test
    @DisplayName("copyFrom：复制六维度与数值，不复制备注")
    void copyFrom_shouldCopyDimensionsButNotNotes() {
        DailyStatus previous = DailyStatus.create(PET_ID, DATE.minusDays(1),
                Appetite.DECREASED, Energy.LETHARGIC, WaterIntake.MORE,
                Defecation.DIARRHEA, Vomiting.ONCE, Urination.ABNORMAL,
                new BigDecimal("30.5"), new BigDecimal("120"), "昨天吐了", OPERATOR_ID);

        DailyStatus copy = DailyStatus.copyFrom(previous, DATE, OPERATOR_ID);

        assertThat(copy.getAppetite()).isEqualTo(Appetite.DECREASED);
        assertThat(copy.getEnergy()).isEqualTo(Energy.LETHARGIC);
        assertThat(copy.getWaterIntake()).isEqualTo(WaterIntake.MORE);
        assertThat(copy.getDefecation()).isEqualTo(Defecation.DIARRHEA);
        assertThat(copy.getVomiting()).isEqualTo(Vomiting.ONCE);
        assertThat(copy.getUrination()).isEqualTo(Urination.ABNORMAL);
        assertThat(copy.getAppetiteAmount()).isEqualByComparingTo("30.5");
        assertThat(copy.getWaterAmount()).isEqualByComparingTo("120");
        assertThat(copy.getNotes()).isNull();
        assertThat(copy.getRecordDate()).isEqualTo(DATE);
    }

    @Test
    @DisplayName("isAllNormal：全正常档返回 true")
    void isAllNormal_shouldReturnTrueWhenAllNormal() {
        DailyStatus status = DailyStatus.createNormal(PET_ID, DATE, PetType.CAT, OPERATOR_ID);
        assertThat(status.isAllNormal()).isTrue();
    }

    @Test
    @DisplayName("isAllNormal：存在异常档返回 false")
    void isAllNormal_shouldReturnFalseWhenAbnormal() {
        DailyStatus status = DailyStatus.create(PET_ID, DATE,
                Appetite.REFUSED, Energy.NORMAL, WaterIntake.NORMAL,
                Defecation.NORMAL, Vomiting.NONE, Urination.NORMAL,
                null, null, null, OPERATOR_ID);
        assertThat(status.isAllNormal()).isFalse();
    }

    @Test
    @DisplayName("hasAbnormalSignal：呕吐一次视为异常信号")
    void hasAbnormalSignal_shouldDetectVomiting() {
        DailyStatus status = DailyStatus.create(PET_ID, DATE,
                Appetite.NORMAL, Energy.NORMAL, WaterIntake.NORMAL,
                Defecation.NORMAL, Vomiting.ONCE, Urination.NORMAL,
                null, null, null, OPERATOR_ID);
        assertThat(status.hasAbnormalSignal()).isTrue();
        assertThat(status.getAbnormalDimensions()).containsExactly(RecordDimension.VOMITING);
    }

    @Test
    @DisplayName("getAbnormalDimensions：多维度异常按维度顺序返回")
    void getAbnormalDimensions_shouldReturnAllAbnormal() {
        DailyStatus status = DailyStatus.create(PET_ID, DATE,
                Appetite.DECREASED, Energy.LETHARGIC, WaterIntake.NOT_NOTICED,
                Defecation.NOT_SEEN, Vomiting.NONE, Urination.NOT_SEEN,
                null, null, null, OPERATOR_ID);

        List<RecordDimension> abnormal = status.getAbnormalDimensions();
        assertThat(abnormal).containsExactly(RecordDimension.APPETITE, RecordDimension.ENERGY);
    }

    @Test
    @DisplayName("getAbnormalDimensions：逃生口档位不算异常")
    void getAbnormalDimensions_shouldIgnoreEscapeHatches() {
        DailyStatus status = DailyStatus.create(PET_ID, DATE,
                Appetite.NORMAL, Energy.NORMAL, WaterIntake.NOT_NOTICED,
                Defecation.NOT_SEEN, Vomiting.NONE, Urination.NOT_SEEN,
                null, null, null, OPERATOR_ID);

        assertThat(status.hasAbnormalSignal()).isFalse();
        assertThat(status.getAbnormalDimensions()).isEmpty();
    }

    @Test
    @DisplayName("update：覆盖更新并校验数值")
    void update_shouldOverwriteAndValidate() {
        DailyStatus status = DailyStatus.createNormal(PET_ID, DATE, PetType.CAT, OPERATOR_ID);

        status.update(Appetite.REFUSED, Energy.LETHARGIC, WaterIntake.LESS,
                Defecation.CONSTIPATION, Vomiting.MULTIPLE, Urination.ABNORMAL,
                new BigDecimal("0"), new BigDecimal("10"), "情况恶化");

        assertThat(status.getAppetite()).isEqualTo(Appetite.REFUSED);
        assertThat(status.getVomiting()).isEqualTo(Vomiting.MULTIPLE);
        assertThat(status.hasAbnormalSignal()).isTrue();

        assertThatThrownBy(() -> status.update(Appetite.NORMAL, Energy.NORMAL, WaterIntake.NORMAL,
                Defecation.NORMAL, Vomiting.NONE, Urination.NORMAL,
                new BigDecimal("-5"), null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
