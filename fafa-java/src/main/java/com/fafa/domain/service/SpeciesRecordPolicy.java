package com.fafa.domain.service;

import com.fafa.domain.model.pet.PetType;
import com.fafa.domain.model.record.*;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 物种记录配置领域服务
 *
 * 替代 pet_record_config 表：配置与代码强耦合（新增维度必然伴随枚举与前端改造），
 * 硬编码于策略内，前端通过 dimensions 接口获取渲染配置。
 *
 * 物种差异：
 * - 猫（other 复用）：排尿为必填重点维度（公猫排尿困难是按小时计的急症）
 * - 狗：不展示排尿维度
 *
 * @author FaFa Team
 * @since 2.0
 */
@Service
public class SpeciesRecordPolicy {

    /** 维度配置：维度 + 是否必填 + 特殊提示 */
    public record DimensionConfig(RecordDimension dimension, boolean required, String tip) {
    }

    private static final String TIP_WATER_INTAKE =
            "多饮多尿可能是肾病/糖尿病/甲亢的早期信号";
    private static final String TIP_URINATION =
            "排尿困难（频繁蹲姿、尿量少、嚎叫）是按小时计的急症，请立即就医";

    /** 猫维度配置（含顺序）：排尿必填 */
    private static final List<DimensionConfig> CAT_DIMENSIONS = List.of(
            new DimensionConfig(RecordDimension.APPETITE, true, null),
            new DimensionConfig(RecordDimension.ENERGY, true, null),
            new DimensionConfig(RecordDimension.WATER_INTAKE, true, TIP_WATER_INTAKE),
            new DimensionConfig(RecordDimension.DEFECATION, true, null),
            new DimensionConfig(RecordDimension.VOMITING, false, null),
            new DimensionConfig(RecordDimension.URINATION, true, TIP_URINATION)
    );

    /** 狗维度配置（含顺序）：不展示排尿 */
    private static final List<DimensionConfig> DOG_DIMENSIONS = List.of(
            new DimensionConfig(RecordDimension.APPETITE, true, null),
            new DimensionConfig(RecordDimension.ENERGY, true, null),
            new DimensionConfig(RecordDimension.DEFECATION, true, null),
            new DimensionConfig(RecordDimension.WATER_INTAKE, true, null),
            new DimensionConfig(RecordDimension.VOMITING, false, null)
    );

    /**
     * 按物种取维度配置（含展示顺序）；other 复用猫配置
     */
    public List<DimensionConfig> dimensionsFor(PetType type) {
        return type == PetType.DOG ? DOG_DIMENSIONS : CAT_DIMENSIONS;
    }

    /**
     * 按物种取必填维度集合（upsert 校验用）
     */
    public Set<RecordDimension> requiredDimensions(PetType type) {
        Set<RecordDimension> required = EnumSet.noneOf(RecordDimension.class);
        for (DimensionConfig config : dimensionsFor(type)) {
            if (config.required()) {
                required.add(config.dimension());
            }
        }
        return required;
    }

    /**
     * 校验一条记录是否满足物种必填要求
     *
     * not_seen/not_noticed 等逃生口档位满足必填（漏记不惩罚，但「没注意」也是有效记录）。
     *
     * @throws IllegalStateException 缺失必填维度时抛出，消息指明维度名称
     */
    public void validateRequired(PetType type, DailyStatus status) {
        List<String> missing = new ArrayList<>();
        for (RecordDimension dimension : requiredDimensions(type)) {
            if (valueFor(status, dimension) == null) {
                missing.add(dimension.getLabel());
            }
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException("必填记录维度缺失: " + String.join("、", missing));
        }
    }

    /** 取记录中指定维度的值（null 表示未填） */
    private Object valueFor(DailyStatus status, RecordDimension dimension) {
        return switch (dimension) {
            case APPETITE -> status.getAppetite();
            case ENERGY -> status.getEnergy();
            case WATER_INTAKE -> status.getWaterIntake();
            case DEFECATION -> status.getDefecation();
            case VOMITING -> status.getVomiting();
            case URINATION -> status.getUrination();
        };
    }
}
