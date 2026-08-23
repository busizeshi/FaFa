package com.fafa.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fafa.domain.model.record.FeedingRecord;
import com.fafa.domain.model.record.FoodType;
import com.fafa.domain.repository.FeedingRecordRepository;
import com.fafa.infrastructure.persistence.mapper.FeedingRecordMapper;
import com.fafa.infrastructure.persistence.po.FeedingRecordPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 喂食记录仓储实现
 *
 * @author FaFa Team
 * @since 2.0
 */
@Repository
@RequiredArgsConstructor
public class FeedingRecordRepositoryImpl implements FeedingRecordRepository {

    private final FeedingRecordMapper feedingRecordMapper;

    @Override
    public Optional<FeedingRecord> findById(Long id) {
        return Optional.ofNullable(toDomain(feedingRecordMapper.selectById(id)));
    }

    @Override
    public FeedingRecord save(FeedingRecord record) {
        FeedingRecordPO po = toPO(record);

        if (po.getId() == null) {
            feedingRecordMapper.insert(po);
        } else {
            feedingRecordMapper.updateById(po);
        }

        return toDomain(feedingRecordMapper.selectById(po.getId()));
    }

    @Override
    public List<FeedingRecord> findByPetIdAndDate(Long petId, LocalDate date) {
        LambdaQueryWrapper<FeedingRecordPO> wrapper = new LambdaQueryWrapper<>();
        // 左闭右开：[date 00:00, date+1 00:00)
        wrapper.eq(FeedingRecordPO::getPetId, petId)
               .ge(FeedingRecordPO::getFeedingTime, date.atStartOfDay())
               .lt(FeedingRecordPO::getFeedingTime, date.plusDays(1).atStartOfDay())
               .orderByAsc(FeedingRecordPO::getFeedingTime);
        return feedingRecordMapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<FeedingRecord> findByPetIdAndDateRange(Long petId, LocalDate start, LocalDate end) {
        LambdaQueryWrapper<FeedingRecordPO> wrapper = new LambdaQueryWrapper<>();
        // 含起止两端：[start 00:00, end 23:59:59.999999999]
        wrapper.eq(FeedingRecordPO::getPetId, petId)
               .ge(FeedingRecordPO::getFeedingTime, start.atStartOfDay())
               .le(FeedingRecordPO::getFeedingTime, end.atTime(LocalTime.MAX))
               .orderByAsc(FeedingRecordPO::getFeedingTime);
        return feedingRecordMapper.selectList(wrapper).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        // @TableLogic 自动转为逻辑删除
        feedingRecordMapper.deleteById(id);
    }

    /**
     * 持久化对象转领域对象
     */
    private FeedingRecord toDomain(FeedingRecordPO po) {
        if (po == null) {
            return null;
        }
        return FeedingRecord.restore(
                po.getId(),
                po.getPetId(),
                po.getFeedingTime(),
                FoodType.fromCode(po.getFoodType()),
                po.getAmount(),
                po.getUnit(),
                po.getFoodId(),
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
    private FeedingRecordPO toPO(FeedingRecord record) {
        FeedingRecordPO po = new FeedingRecordPO();
        po.setId(record.getId());
        po.setPetId(record.getPetId());
        po.setFeedingTime(record.getFeedingTime());
        po.setFoodType(record.getFoodType().getCode());
        po.setAmount(record.getAmount());
        po.setUnit(record.getUnit());
        po.setFoodId(record.getFoodId());
        po.setNotes(record.getNotes());
        po.setOperatorId(record.getOperatorId());
        po.setIsDeleted(record.isDeleted());
        return po;
    }
}
