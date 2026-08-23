package com.fafa.domain.model.record;

import com.fafa.domain.model.pet.PetType;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 每日状态记录（聚合根）
 *
 * 一天一格 upsert 语义：同一宠物同一天只有一条记录，重复提交覆盖。
 * 六维度观察 + 进食/饮水量数值 + 备注。
 *
 * 业务规则：
 * 1. petId 与 recordDate 必填
 * 2. appetiteAmount/waterAmount 不可为负
 * 3. notes 最长 500 字符
 * 4. 「一切正常」按物种填正常档（狗不填排尿）
 * 5. 「同上次」复制六维度与两个数值，不复制 notes（备注是当天特定信息）
 *
 * @author FaFa Team
 * @since 2.0
 */
@Getter
public class DailyStatus {

    private static final int MAX_NOTES_LENGTH = 500;

    private Long id;
    private final Long petId;
    private final LocalDate recordDate;

    private Appetite appetite;
    private Energy energy;
    private WaterIntake waterIntake;
    private Defecation defecation;
    private Vomiting vomiting;
    private Urination urination;

    /** 进食克数（选填） */
    private BigDecimal appetiteAmount;
    /** 饮水毫升数（选填） */
    private BigDecimal waterAmount;

    private String notes;
    private final Long operatorId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private DailyStatus(Long id, Long petId, LocalDate recordDate,
                        Appetite appetite, Energy energy, WaterIntake waterIntake,
                        Defecation defecation, Vomiting vomiting, Urination urination,
                        BigDecimal appetiteAmount, BigDecimal waterAmount,
                        String notes, Long operatorId,
                        LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.petId = petId;
        this.recordDate = recordDate;
        this.appetite = appetite;
        this.energy = energy;
        this.waterIntake = waterIntake;
        this.defecation = defecation;
        this.vomiting = vomiting;
        this.urination = urination;
        this.appetiteAmount = appetiteAmount;
        this.waterAmount = waterAmount;
        this.notes = notes;
        this.operatorId = operatorId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 创建每日状态记录（手动打卡）
     */
    public static DailyStatus create(Long petId, LocalDate recordDate,
                                     Appetite appetite, Energy energy, WaterIntake waterIntake,
                                     Defecation defecation, Vomiting vomiting, Urination urination,
                                     BigDecimal appetiteAmount, BigDecimal waterAmount,
                                     String notes, Long operatorId) {
        Objects.requireNonNull(petId, "petId 不能为空");
        Objects.requireNonNull(recordDate, "recordDate 不能为空");
        validateAmounts(appetiteAmount, waterAmount);
        validateNotes(notes);

        return new DailyStatus(null, petId, recordDate,
                appetite, energy, waterIntake, defecation,
                vomiting != null ? vomiting : Vomiting.NONE,
                urination, appetiteAmount, waterAmount, notes, operatorId,
                null, null);
    }

    /**
     * 「今天一切正常」一键记录
     *
     * 按物种填正常档：呕吐=无；狗不展示排尿维度，保持 null。
     */
    public static DailyStatus createNormal(Long petId, LocalDate recordDate,
                                           PetType petType, Long operatorId) {
        Objects.requireNonNull(petId, "petId 不能为空");
        Objects.requireNonNull(recordDate, "recordDate 不能为空");

        // 狗不填排尿（物种差异），其余物种填正常
        Urination urination = petType == PetType.DOG ? null : Urination.NORMAL;

        return new DailyStatus(null, petId, recordDate,
                Appetite.NORMAL, Energy.NORMAL, WaterIntake.NORMAL,
                Defecation.NORMAL, Vomiting.NONE, urination,
                null, null, null, operatorId, null, null);
    }

    /**
     * 「同上次」一键记录：复制六维度与两个数值，不复制 notes
     */
    public static DailyStatus copyFrom(DailyStatus previous, LocalDate newDate, Long operatorId) {
        Objects.requireNonNull(previous, "历史记录不能为空");
        Objects.requireNonNull(newDate, "newDate 不能为空");

        return new DailyStatus(null, previous.petId, newDate,
                previous.appetite, previous.energy, previous.waterIntake,
                previous.defecation, previous.vomiting, previous.urination,
                previous.appetiteAmount, previous.waterAmount,
                null, operatorId, null, null);
    }

    /**
     * 仓储重建
     */
    public static DailyStatus restore(Long id, Long petId, LocalDate recordDate,
                                      Appetite appetite, Energy energy, WaterIntake waterIntake,
                                      Defecation defecation, Vomiting vomiting, Urination urination,
                                      BigDecimal appetiteAmount, BigDecimal waterAmount,
                                      String notes, Long operatorId,
                                      LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new DailyStatus(id, petId, recordDate,
                appetite, energy, waterIntake, defecation, vomiting, urination,
                appetiteAmount, waterAmount, notes, operatorId, createdAt, updatedAt);
    }

    /**
     * 覆盖更新（upsert 的 update 分支）
     */
    public void update(Appetite appetite, Energy energy, WaterIntake waterIntake,
                       Defecation defecation, Vomiting vomiting, Urination urination,
                       BigDecimal appetiteAmount, BigDecimal waterAmount, String notes) {
        validateAmounts(appetiteAmount, waterAmount);
        validateNotes(notes);

        this.appetite = appetite;
        this.energy = energy;
        this.waterIntake = waterIntake;
        this.defecation = defecation;
        this.vomiting = vomiting != null ? vomiting : Vomiting.NONE;
        this.urination = urination;
        this.appetiteAmount = appetiteAmount;
        this.waterAmount = waterAmount;
        this.notes = notes;
    }

    /**
     * 所有已填维度是否均为正常档
     *
     * 未填（null）维度不参与判断；呕吐默认无=正常。
     */
    public boolean isAllNormal() {
        return (appetite == null || appetite.isNormal())
                && (energy == null || energy.isNormal())
                && (waterIntake == null || waterIntake.isNormal())
                && (defecation == null || defecation.isNormal())
                && (vomiting == null || vomiting.isNormal())
                && (urination == null || urination.isNormal());
    }

    /**
     * 是否存在任一明确异常信号（Phase 4 健康预警的领域层落位）
     */
    public boolean hasAbnormalSignal() {
        return !getAbnormalDimensions().isEmpty();
    }

    /**
     * 异常维度清单（前端高亮 / 预警展示用）
     */
    public List<RecordDimension> getAbnormalDimensions() {
        List<RecordDimension> result = new ArrayList<>();
        if (appetite != null && appetite.isAbnormal()) {
            result.add(RecordDimension.APPETITE);
        }
        if (energy != null && energy.isAbnormal()) {
            result.add(RecordDimension.ENERGY);
        }
        if (waterIntake != null && waterIntake.isAbnormal()) {
            result.add(RecordDimension.WATER_INTAKE);
        }
        if (defecation != null && defecation.isAbnormal()) {
            result.add(RecordDimension.DEFECATION);
        }
        if (vomiting != null && vomiting.isAbnormal()) {
            result.add(RecordDimension.VOMITING);
        }
        if (urination != null && urination.isAbnormal()) {
            result.add(RecordDimension.URINATION);
        }
        return result;
    }

    /**
     * 仓储保存后回填 ID
     */
    public void assignId(Long id) {
        this.id = id;
    }

    public void assignCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void assignUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    private static void validateAmounts(BigDecimal appetiteAmount, BigDecimal waterAmount) {
        if (appetiteAmount != null && appetiteAmount.signum() < 0) {
            throw new IllegalArgumentException("进食克数不能为负");
        }
        if (waterAmount != null && waterAmount.signum() < 0) {
            throw new IllegalArgumentException("饮水毫升数不能为负");
        }
    }

    private static void validateNotes(String notes) {
        if (notes != null && notes.length() > MAX_NOTES_LENGTH) {
            throw new IllegalArgumentException("备注不能超过 " + MAX_NOTES_LENGTH + " 字符");
        }
    }
}
