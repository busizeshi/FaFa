package com.fafa.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fafa.domain.model.record.FeedRecord;
import com.fafa.domain.model.record.RecordId;
import com.fafa.domain.repository.FeedRecordRepository;
import com.fafa.infrastructure.persistence.converter.FeedRecordConverter;
import com.fafa.infrastructure.persistence.dataobject.FeedRecordDO;
import com.fafa.infrastructure.persistence.mapper.FeedRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 喂食记录仓储实现
 * 
 * @author FaFa Team
 * @since 1.0
 */
@Repository
@RequiredArgsConstructor
public class FeedRecordRepositoryImpl implements FeedRecordRepository {
    
    private final FeedRecordMapper feedRecordMapper;
    private final FeedRecordConverter feedRecordConverter;
    
    @Override
    public FeedRecord save(FeedRecord feedRecord) {
        FeedRecordDO feedRecordDO = feedRecordConverter.toDO(feedRecord);
        
        if (feedRecordDO.getId() == null) {
            feedRecordMapper.insert(feedRecordDO);
        } else {
            feedRecordMapper.updateById(feedRecordDO);
        }
        
        return feedRecordConverter.toDomain(feedRecordDO);
    }
    
    @Override
    public Optional<FeedRecord> findById(RecordId recordId) {
        FeedRecordDO feedRecordDO = feedRecordMapper.selectById(recordId.getValue());
        return Optional.ofNullable(feedRecordConverter.toDomain(feedRecordDO));
    }
    
    @Override
    public List<FeedRecord> findByPetId(Long petId, LocalDate startDate, LocalDate endDate, int page, int size) {
        LambdaQueryWrapper<FeedRecordDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FeedRecordDO::getPetId, petId);
        
        if (startDate != null) {
            wrapper.ge(FeedRecordDO::getFeedTime, LocalDateTime.of(startDate, LocalTime.MIN));
        }
        if (endDate != null) {
            wrapper.le(FeedRecordDO::getFeedTime, LocalDateTime.of(endDate, LocalTime.MAX));
        }
        
        wrapper.orderByDesc(FeedRecordDO::getFeedTime);
        
        Page<FeedRecordDO> pageParam = new Page<>(page, size);
        Page<FeedRecordDO> result = feedRecordMapper.selectPage(pageParam, wrapper);
        
        return result.getRecords().stream()
                .map(feedRecordConverter::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public Optional<FeedRecord> findLatestByPetId(Long petId) {
        LambdaQueryWrapper<FeedRecordDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FeedRecordDO::getPetId, petId)
                .orderByDesc(FeedRecordDO::getFeedTime)
                .last("LIMIT 1");
        
        FeedRecordDO feedRecordDO = feedRecordMapper.selectOne(wrapper);
        return Optional.ofNullable(feedRecordConverter.toDomain(feedRecordDO));
    }
    
    @Override
    public void deleteById(RecordId recordId) {
        feedRecordMapper.deleteById(recordId.getValue());
    }
    
    @Override
    public void deleteByPetId(Long petId) {
        LambdaQueryWrapper<FeedRecordDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FeedRecordDO::getPetId, petId);
        feedRecordMapper.delete(wrapper);
    }
    
    @Override
    public int countByPetId(Long petId, LocalDate startDate, LocalDate endDate) {
        return feedRecordMapper.countByPetIdAndDateRange(petId, startDate, endDate);
    }
}
