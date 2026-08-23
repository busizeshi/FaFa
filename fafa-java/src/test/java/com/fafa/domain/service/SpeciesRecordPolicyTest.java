package com.fafa.domain.service;

import com.fafa.domain.model.pet.PetType;
import com.fafa.domain.model.record.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 物种记录配置领域服务单元测试
 *
 * @author FaFa Team
 * @since 2.0
 */
class SpeciesRecordPolicyTest {

    private static final Long PET_ID = 1L;
    private static final LocalDate DATE = LocalDate.of(2026, 8, 22);
    private static final Long OPERATOR_ID = 100L;

    private final SpeciesRecordPolicy policy = new SpeciesRecordPolicy();

    @Test
    @DisplayName("猫：六维度且排尿必填")
    void dimensionsFor_cat_shouldIncludeUrinationRequired() {
        List<SpeciesRecordPolicy.DimensionConfig> configs = policy.dimensionsFor(PetType.CAT);

        assertThat(configs).hasSize(6);
        assertThat(configs.stream().map(SpeciesRecordPolicy.DimensionConfig::dimension).toList())
                .containsExactly(RecordDimension.APPETITE, RecordDimension.ENERGY,
                        RecordDimension.WATER_INTAKE, RecordDimension.DEFECATION,
                        RecordDimension.VOMITING, RecordDimension.URINATION);

        SpeciesRecordPolicy.DimensionConfig urination = configs.stream()
                .filter(c -> c.dimension() == RecordDimension.URINATION)
                .findFirst().orElseThrow();
        assertThat(urination.required()).isTrue();
        assertThat(urination.tip()).contains("急症");

        SpeciesRecordPolicy.DimensionConfig water = configs.stream()
                .filter(c -> c.dimension() == RecordDimension.WATER_INTAKE)
                .findFirst().orElseThrow();
        assertThat(water.tip()).contains("多饮多尿");
    }

    @Test
    @DisplayName("狗：五维度且不展示排尿")
    void dimensionsFor_dog_shouldExcludeUrination() {
        List<SpeciesRecordPolicy.DimensionConfig> configs = policy.dimensionsFor(PetType.DOG);

        assertThat(configs).hasSize(5);
        assertThat(configs.stream().map(SpeciesRecordPolicy.DimensionConfig::dimension).toList())
                .doesNotContain(RecordDimension.URINATION);
    }

    @Test
    @DisplayName("other 复用猫配置")
    void dimensionsFor_other_shouldReuseCatConfig() {
        assertThat(policy.dimensionsFor(PetType.OTHER))
                .isEqualTo(policy.dimensionsFor(PetType.CAT));
    }

    @Test
    @DisplayName("requiredDimensions：呕吐非必填")
    void requiredDimensions_shouldExcludeVomiting() {
        Set<RecordDimension> catRequired = policy.requiredDimensions(PetType.CAT);
        assertThat(catRequired).contains(RecordDimension.APPETITE, RecordDimension.ENERGY,
                RecordDimension.WATER_INTAKE, RecordDimension.DEFECATION, RecordDimension.URINATION);
        assertThat(catRequired).doesNotContain(RecordDimension.VOMITING);

        Set<RecordDimension> dogRequired = policy.requiredDimensions(PetType.DOG);
        assertThat(dogRequired).doesNotContain(RecordDimension.URINATION, RecordDimension.VOMITING);
    }

    @Test
    @DisplayName("validateRequired：猫记录齐全不抛异常")
    void validateRequired_catComplete_shouldPass() {
        DailyStatus status = DailyStatus.createNormal(PET_ID, DATE, PetType.CAT, OPERATOR_ID);
        policy.validateRequired(PetType.CAT, status);
    }

    @Test
    @DisplayName("validateRequired：猫缺排尿抛出异常并指明维度")
    void validateRequired_catMissingUrination_shouldThrow() {
        DailyStatus status = DailyStatus.create(PET_ID, DATE,
                Appetite.NORMAL, Energy.NORMAL, WaterIntake.NORMAL,
                Defecation.NORMAL, Vomiting.NONE, null,
                null, null, null, OPERATOR_ID);

        assertThatThrownBy(() -> policy.validateRequired(PetType.CAT, status))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("排尿");
    }

    @Test
    @DisplayName("validateRequired：狗不要求排尿")
    void validateRequired_dogWithoutUrination_shouldPass() {
        DailyStatus status = DailyStatus.create(PET_ID, DATE,
                Appetite.NORMAL, Energy.NORMAL, WaterIntake.NORMAL,
                Defecation.NORMAL, Vomiting.NONE, null,
                null, null, null, OPERATOR_ID);

        policy.validateRequired(PetType.DOG, status);
    }

    @Test
    @DisplayName("validateRequired：逃生口档位满足必填")
    void validateRequired_escapeHatch_shouldSatisfyRequired() {
        DailyStatus status = DailyStatus.create(PET_ID, DATE,
                Appetite.NORMAL, Energy.NORMAL, WaterIntake.NOT_NOTICED,
                Defecation.NOT_SEEN, null, Urination.NOT_SEEN,
                null, null, null, OPERATOR_ID);

        policy.validateRequired(PetType.CAT, status);
    }

    @Test
    @DisplayName("validateRequired：缺失多个维度时消息全部列出")
    void validateRequired_multipleMissing_shouldListAll() {
        DailyStatus status = DailyStatus.create(PET_ID, DATE,
                Appetite.NORMAL, null, null,
                null, null, null,
                null, null, null, OPERATOR_ID);

        assertThatThrownBy(() -> policy.validateRequired(PetType.CAT, status))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("精神活动")
                .hasMessageContaining("饮水")
                .hasMessageContaining("排便")
                .hasMessageContaining("排尿");
    }
}
