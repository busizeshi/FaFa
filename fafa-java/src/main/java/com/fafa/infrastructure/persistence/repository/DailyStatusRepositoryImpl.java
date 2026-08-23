package com.fafa.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fafa.domain.model.record.*;
import com.fafa.domain.repository.DailyStatusRepository;
import com.fafa.infrastructure.persistence.mapper.DailyStatusMapper;
import com.fafa.infrastructure.persistence.po.DailyStatusPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 每日状态记录仓储实现
 *
 * save 为 upsert 语义：先查后写，极端并发下 insert 撞唯一键 uk_pet_date
 * 时捕获 DuplicateKeyException 转为 update 兜底。
 *
 * @author FaFa Team
 * @since 2.0
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class DailyStatusRepositoryImpl implements DailyStatusRepository {

    private final DailyStatusMapper dailyStatusMapper;

    @Override
    public Optional<DailyStatus> findByPetIdAndDate(Long petId, LocalDate date) {
        LambdaQueryWrapper<DailyStatusPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DailyStatusPO::getPetId, petId)
               .eq(DailyStatusPO::getRecordDate, date);
        return Optional.ofNullable(toDomain(dailyStatusMapper.selectOne(wrapper)));
    }

    @Override
    public Optional<DailyStatus> findLatestBefore(Long petId, LocalDate exclusiveDate) {
        LambdaQueryWrapper<DailyStatusPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DailyStatusPO::getPetId, petId)
               .lt(DailyStatusPO::getRecordDate, exclusiveDate)
               .orderByDesc(DailyStatusPO::getRecordDate)
               .last("LIMIT 1");
        return Optional.ofNullable(toDomain(dailyStatusMapper.selectOne(wrapper)));
    }

    @Override
    public List<DailyStatus> findByPetIdAndDateRange(Long petId, LocalDate start, LocalDate end) {
        LambdaQueryWrapper<DailyStatusPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DailyStatusPO::getPetId, petId)
               .ge(DailyStatusPO::getRecordDate, start)
               .le(DailyStatusPO::getRecordDate, end)
               .orderByAsc(DailyStatusPO::getRecordDate);
        return dailyStatusMapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public DailyStatus save(DailyStatus status) {
        // upsert：当天已有记录则覆盖更新，否则插入
        Optional<DailyStatus> existing = findByPetIdAndDate(status.getPetId(), status.getRecordDate());

        if (existing.isPresent()) {
            DailyStatusPO po = toPO(status);
            po.setId(existing.get().getId());
            dailyStatusMapper.updateById(po);
            return toDomain(dailyStatusMapper.selectById(po.getId()));
        }

        DailyStatusPO po = toPO(status);
        try {
            dailyStatusMapper.insert(po);
        } catch (DuplicateKeyException e) {
            // 并发兜底：另一请求已插入同天记录，转为更新
            log.warn("每日状态 upsert 并发冲突，转为更新: petId={}, date={}",
                    status.getPetId(), status.getRecordDate());
            DailyStatusPO conflict = toPO(status);
            LambdaQueryWrapper<DailyStatusPO> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(DailyStatusPO::getPetId, status.getPetId())
                   .eq(DailyStatusPO::getRecordDate, status.getRecordDate());
            conflict.setId(dailyStatusMapper.selectOne(wrapper).getId());
            dailyStatusMapper.updateById(conflict);
            return toDomain(dailyStatusMapper.selectById(conflict.getId()));
        }

        return toDomain(dailyStatusMapper.selectById(po.getId()));
    }

    @Override
    public boolean deleteByPetIdAndDate(Long petId, LocalDate date) {
        LambdaQueryWrapper<DailyStatusPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DailyStatusPO::getPetId, petId)
               .eq(DailyStatusPO::getRecordDate, date);
        // 物理删除：daily_status 无逻辑删除列，delete 即真实删除
        return dailyStatusMapper.delete(wrapper) > 0;
    }

    /**
     * 持久化对象转领域对象
     */
    private DailyStatus toDomain(DailyStatusPO po) {
        if (po == null) {
            return null;
        }
        return DailyStatus.restore(
                po.getId(),
                po.getPetId(),
                po.getRecordDate(),
                po.getAppetite() != null ? Appetite.fromCode(po.getAppetite()) : null,
                po.getEnergy() != null ? Energy.fromCode(po.getEnergy()) : null,
                po.getWaterIntake() != null ? WaterIntake.fromCode(po.getWaterIntake()) : null,
                po.getDefecation() != null ? Defecation.fromCode(po.getDefecation()) : null,
                po.getVomiting() != null ? Vomiting.fromCode(po.getVomiting()) : null,
                po.getUrination() != null ? Urination.fromCode(po.getUrination()) : null,
                po.getAppetiteAmount(),
                po.getWaterAmount(),
                po.getNotes(),
                po.getOperatorId(),
                po.getCreatedAt(),
                po.getUpdatedAt()
        );
    }

    /**
     * 领域对象转持久化对象
     */
    private DailyStatusPO toPO(DailyStatus status) {
        DailyStatusPO po = new DailyStatusPO();
        po.setPetId(status.getPetId());
        po.setRecordDate(status.getRecordDate());
        po.setAppetite(status.getAppetite() != null ? status.getAppetite().getCode() : null);
        po.setEnergy(status.getEnergy() != null ? status.getEnergy().getCode() : null);
        po.setWaterIntake(status.getWaterIntake() != null ? status.getWaterIntake().getCode() : null);
        po.setDefecation(status.getDefecation() != null ? status.getDefecation().getCode() : null);
        po.setVomiting(status.getVomiting() != null ? status.getVomiting().getCode() : null);
        po.setUrination(status.getUrination() != null ? status.getUrination().getCode() : null);
        po.setAppetiteAmount(status.getAppetiteAmount());
        po.setWaterAmount(status.getWaterAmount());
        po.setNotes(status.getNotes());
        po.setOperatorId(status.getOperatorId());
        return po;
    }
}
