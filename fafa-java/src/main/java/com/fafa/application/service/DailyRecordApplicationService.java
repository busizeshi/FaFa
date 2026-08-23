package com.fafa.application.service;

import cn.dev33.satoken.stp.StpUtil;
import com.fafa.application.dto.record.*;
import com.fafa.common.BusinessException;
import com.fafa.common.ErrorCode;
import com.fafa.domain.model.pet.Pet;
import com.fafa.domain.model.pet.PetType;
import com.fafa.domain.model.record.*;
import com.fafa.domain.repository.DailyStatusRepository;
import com.fafa.domain.service.SpeciesRecordPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * 每日记录应用服务
 *
 * 负责协调每日状态打卡相关业务：
 * - upsert 每日状态（一天一格，重复提交覆盖）
 * - 「今天一切正常」/「同上次」一键记录
 * - 物种维度配置下发（前端渲染打卡页）
 *
 * @author FaFa Team
 * @since 2.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DailyRecordApplicationService {

    private static final int MAX_RANGE_DAYS = 366;

    private final DailyStatusRepository dailyStatusRepository;
    private final PetApplicationService petApplicationService;
    private final SpeciesRecordPolicy speciesRecordPolicy;

    /**
     * upsert 每日状态：校验所有权 + 日期不晚于今天 + 物种必填维度，同日重复提交覆盖
     */
    @Transactional
    public DailyStatusDTO upsertStatus(SaveDailyStatusCommand command) {
        Pet pet = petApplicationService.checkPetOwnership(command.petId());
        validateNotFuture(command.recordDate());

        DailyStatus status = DailyStatus.create(
                command.petId(),
                command.recordDate(),
                parseEnum(Appetite::fromCode, command.appetite(), "食欲"),
                parseEnum(Energy::fromCode, command.energy(), "精神活动"),
                parseEnum(WaterIntake::fromCode, command.waterIntake(), "饮水"),
                parseEnum(Defecation::fromCode, command.defecation(), "排便"),
                parseEnum(Vomiting::fromCode, command.vomiting(), "呕吐"),
                parseEnum(Urination::fromCode, command.urination(), "排尿"),
                command.appetiteAmount(),
                command.waterAmount(),
                command.notes(),
                getCurrentUserId()
        );

        // 物种必填维度校验（仅手动记录强制，一键操作天然齐全）
        try {
            speciesRecordPolicy.validateRequired(pet.getType(), status);
        } catch (IllegalStateException e) {
            throw new BusinessException(ErrorCode.DAILY_DIMENSION_MISSING, e.getMessage());
        }

        DailyStatus saved = dailyStatusRepository.save(status);
        log.info("每日状态记录成功: petId={}, date={}, allNormal={}",
                command.petId(), command.recordDate(), saved.isAllNormal());
        return toDTO(saved);
    }

    /**
     * 「今天一切正常」一键记录：按物种填正常档
     */
    @Transactional
    public DailyStatusDTO recordNormal(Long petId, LocalDate date) {
        Pet pet = petApplicationService.checkPetOwnership(petId);
        validateNotFuture(date);

        DailyStatus status = DailyStatus.createNormal(petId, date, pet.getType(), getCurrentUserId());
        DailyStatus saved = dailyStatusRepository.save(status);
        log.info("一键记录一切正常: petId={}, date={}", petId, date);
        return toDTO(saved);
    }

    /**
     * 最近一条记录（before 可选，缺省含今天；「同上次」预览回填用）
     */
    public Optional<DailyStatusDTO> getLatest(Long petId, LocalDate before) {
        petApplicationService.checkPetOwnership(petId);
        LocalDate boundary = before != null ? before : LocalDate.now().plusDays(1);
        return dailyStatusRepository.findLatestBefore(petId, boundary).map(this::toDTO);
    }

    /**
     * 「同上次」一键：取严格早于 date 的最近一条，复制维度后 upsert
     */
    @Transactional
    public DailyStatusDTO copyFromLast(Long petId, LocalDate date) {
        petApplicationService.checkPetOwnership(petId);
        validateNotFuture(date);

        DailyStatus previous = dailyStatusRepository.findLatestBefore(petId, date)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECORD_NO_PREVIOUS));

        DailyStatus copy = DailyStatus.copyFrom(previous, date, getCurrentUserId());
        DailyStatus saved = dailyStatusRepository.save(copy);
        log.info("同上次记录成功: petId={}, date={}, 来源日期={}", petId, date, previous.getRecordDate());
        return toDTO(saved);
    }

    /**
     * 查询某天记录（未记录返回 empty，前端显示灰格）
     */
    public Optional<DailyStatusDTO> getByDate(Long petId, LocalDate date) {
        petApplicationService.checkPetOwnership(petId);
        return dailyStatusRepository.findByPetIdAndDate(petId, date).map(this::toDTO);
    }

    /**
     * 日期范围列表（升序，范围 ≤366 天）
     */
    public List<DailyStatusDTO> listByRange(Long petId, LocalDate start, LocalDate end) {
        petApplicationService.checkPetOwnership(petId);
        validateRange(start, end);
        return dailyStatusRepository.findByPetIdAndDateRange(petId, start, end).stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * 删除某天记录（物理删除）
     */
    @Transactional
    public void delete(Long petId, LocalDate date) {
        petApplicationService.checkPetOwnership(petId);
        boolean deleted = dailyStatusRepository.deleteByPetIdAndDate(petId, date);
        if (!deleted) {
            throw new BusinessException(ErrorCode.RECORD_NOT_FOUND);
        }
        log.info("删除每日状态记录: petId={}, date={}", petId, date);
    }

    /**
     * 物种维度配置（打卡页渲染：顺序/必填/提示/选项）
     *
     * @param petType 宠物类型 code，缺省按 other（复用猫配置）
     */
    public List<DimensionConfigDTO> getDimensionConfigs(String petType) {
        PetType type = (petType == null || petType.isBlank())
                ? PetType.OTHER
                : parseEnum(PetType::fromCode, petType, "宠物类型");

        return speciesRecordPolicy.dimensionsFor(type).stream()
                .map(config -> new DimensionConfigDTO(
                        config.dimension().getCode(),
                        config.dimension().getLabel(),
                        config.required(),
                        config.tip(),
                        optionsFor(config.dimension())))
                .toList();
    }

    /**
     * 枚举 code 解析：非法值转 PARAM_INVALID（空值返回 null，必填校验由物种策略负责）
     */
    private <E> E parseEnum(Function<String, E> parser, String code, String fieldName) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return parser.apply(code);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, fieldName + "取值非法: " + code);
        }
    }

    /** 维度的可选档位列表 */
    private List<DimensionOptionDTO> optionsFor(RecordDimension dimension) {
        return switch (dimension) {
            case APPETITE -> Arrays.stream(Appetite.values())
                    .map(v -> new DimensionOptionDTO(v.getCode(), v.getLabel())).toList();
            case ENERGY -> Arrays.stream(Energy.values())
                    .map(v -> new DimensionOptionDTO(v.getCode(), v.getLabel())).toList();
            case WATER_INTAKE -> Arrays.stream(WaterIntake.values())
                    .map(v -> new DimensionOptionDTO(v.getCode(), v.getLabel())).toList();
            case DEFECATION -> Arrays.stream(Defecation.values())
                    .map(v -> new DimensionOptionDTO(v.getCode(), v.getLabel())).toList();
            case VOMITING -> Arrays.stream(Vomiting.values())
                    .map(v -> new DimensionOptionDTO(v.getCode(), v.getLabel())).toList();
            case URINATION -> Arrays.stream(Urination.values())
                    .map(v -> new DimensionOptionDTO(v.getCode(), v.getLabel())).toList();
        };
    }

    private void validateNotFuture(LocalDate date) {
        if (date != null && date.isAfter(LocalDate.now())) {
            throw new BusinessException(ErrorCode.RECORD_DATE_IN_FUTURE);
        }
    }

    private void validateRange(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "起止日期不能为空");
        }
        if (start.isAfter(end)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "开始日期不能晚于结束日期");
        }
        if (ChronoUnit.DAYS.between(start, end) > MAX_RANGE_DAYS) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "查询范围不能超过 " + MAX_RANGE_DAYS + " 天");
        }
    }

    private Long getCurrentUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    /**
     * 领域对象转DTO
     */
    private DailyStatusDTO toDTO(DailyStatus s) {
        return new DailyStatusDTO(
                s.getId(),
                s.getPetId(),
                s.getRecordDate(),
                s.getAppetite() != null ? s.getAppetite().getCode() : null,
                s.getEnergy() != null ? s.getEnergy().getCode() : null,
                s.getWaterIntake() != null ? s.getWaterIntake().getCode() : null,
                s.getDefecation() != null ? s.getDefecation().getCode() : null,
                s.getVomiting() != null ? s.getVomiting().getCode() : null,
                s.getUrination() != null ? s.getUrination().getCode() : null,
                s.getAppetiteAmount(),
                s.getWaterAmount(),
                s.getNotes(),
                s.isAllNormal(),
                s.getAbnormalDimensions().stream().map(RecordDimension::getCode).toList(),
                s.getCreatedAt(),
                s.getUpdatedAt()
        );
    }
}
