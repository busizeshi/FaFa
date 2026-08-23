package com.fafa.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fafa.domain.model.record.WeightRecord;
import com.fafa.domain.repository.WeightRecordRepository;
import com.fafa.infrastructure.persistence.mapper.WeightRecordMapper;
import com.fafa.infrastructure.persistence.po.WeightRecordPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 体重记录仓储实现
 *
 * @author FaFa Team
 * @since 2.0
 */
@Repository
@RequiredArgsConstructor
public class WeightRecordRepositoryImpl implements WeightRecordRepository {

    private final WeightRecordMapper weightRecordMapper;

    @Override
    public Optional<WeightRecord> findById(Long id) {
        return Optional.ofNullable(toDomain(weightRecordMapper.selectById(id)));
    }

    @Override
    public WeightRecord save(WeightRecord record) {
        WeightRecordPO po = toPO(record);

        if (po.getId() == null) {
            weightRecordMapper.insert(po);
        } else {
            weightRecordMapper.updateById(po);
        }

        return toDomain(weightRecordMapper.selectById(po.getId()));
    }

    @Override
    public List<WeightRecord> findLatest(Long petId, int limit) {
        LambdaQueryWrapper<WeightRecordPO> wrapper = new LambdaQueryWrapper<>();
        // 同日多条时按 id 倒序保证稳定排序
        wrapper.eq(WeightRecordPO::getPetId, petId)
               .orderByDesc(WeightRecordPO::getRecordDate)
               .orderByDesc(WeightRecordPO::getId)
               .last("LIMIT " + limit);
        return weightRecordMapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<WeightRecord> findByPetIdAndDateRange(Long petId, LocalDate start, LocalDate end) {
        LambdaQueryWrapper<WeightRecordPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WeightRecordPO::getPetId, petId)
               .ge(WeightRecordPO::getRecordDate, start)
               .le(WeightRecordPO::getRecordDate, end)
               .orderByAsc(WeightRecordPO::getRecordDate)
               .orderByAsc(WeightRecordPO::getId);
        return weightRecordMapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<WeightRecord> findLatestByPetId(Long petId) {
        LambdaQueryWrapper<WeightRecordPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WeightRecordPO::getPetId, petId)
               .orderByDesc(WeightRecordPO::getRecordDate)
               .orderByDesc(WeightRecordPO::getId)
               .last("LIMIT 1");
        return Optional.ofNullable(toDomain(weightRecordMapper.selectOne(wrapper)));
    }

    @Override
    public Optional<WeightRecord> findLatestBefore(Long petId, LocalDate exclusiveDate) {
        LambdaQueryWrapper<WeightRecordPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WeightRecordPO::getPetId, petId)
               .lt(WeightRecordPO::getRecordDate, exclusiveDate)
               .orderByDesc(WeightRecordPO::getRecordDate)
               .orderByDesc(WeightRecordPO::getId)
               .last("LIMIT 1");
        return Optional.ofNullable(toDomain(weightRecordMapper.selectOne(wrapper)));
    }

    @Override
    public void deleteById(Long id) {
        // @TableLogic 自动转为逻辑删除
        weightRecordMapper.deleteById(id);
    }

    /**
     * 持久化对象转领域对象
     */
    private WeightRecord toDomain(WeightRecordPO po) {
        if (po == null) {
            return null;
        }
        return WeightRecord.restore(
                po.getId(),
                po.getPetId(),
                po.getRecordDate(),
                po.getWeight(),
                po.getNotes(),
                po.getOperatorId(),
                po.getIsDeleted() != null && po.getIsDeleted(),
                po.getCreatedAt(),
                po.getUpdatedAt()
        );
    }

    /**
     * 领域对象转持久化对象
     */
    private WeightRecordPO toPO(WeightRecord record) {
        WeightRecordPO po = new WeightRecordPO();
        po.setId(record.getId());
        po.setPetId(record.getPetId());
        po.setRecordDate(record.getRecordDate());
        po.setWeight(record.getWeight());
        po.setNotes(record.getNotes());
        po.setOperatorId(record.getOperatorId());
        po.setIsDeleted(record.isDeleted());
        return po;
    }
}
