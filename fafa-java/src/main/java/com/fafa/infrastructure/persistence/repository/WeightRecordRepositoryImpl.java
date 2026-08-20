package com.fafa.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fafa.domain.model.record.WeightRecord;
import com.fafa.domain.repository.WeightRecordRepository;
import com.fafa.infrastructure.persistence.converter.WeightRecordConverter;
import com.fafa.infrastructure.persistence.dataobject.WeightRecordDO;
import com.fafa.infrastructure.persistence.mapper.WeightRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 体重记录仓储实现
 */
@Repository
@RequiredArgsConstructor
public class WeightRecordRepositoryImpl implements WeightRecordRepository {
    
    private final WeightRecordMapper weightRecordMapper;
    
    @Override
    public WeightRecord save(WeightRecord weightRecord) {
        WeightRecordDO weightRecordDO = WeightRecordConverter.toDO(weightRecord);
        
        if (weightRecordDO.getId() == null) {
            weightRecordMapper.insert(weightRecordDO);
        } else {
            weightRecordMapper.updateById(weightRecordDO);
        }
        
        return WeightRecordConverter.toDomain(weightRecordDO);
    }
    
    @Override
    public Optional<WeightRecord> findById(Long id) {
        WeightRecordDO weightRecordDO = weightRecordMapper.selectById(id);
        return Optional.ofNullable(WeightRecordConverter.toDomain(weightRecordDO));
    }
    
    @Override
    public List<WeightRecord> findByPetId(Long petId, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<WeightRecordDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WeightRecordDO::getPetId, petId);
        
        if (startDate != null) {
            wrapper.ge(WeightRecordDO::getRecordDate, startDate);
        }
        if (endDate != null) {
            wrapper.le(WeightRecordDO::getRecordDate, endDate);
        }
        
        wrapper.orderByDesc(WeightRecordDO::getRecordDate);
        
        return weightRecordMapper.selectList(wrapper).stream()
                .map(WeightRecordConverter::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public Optional<WeightRecord> findLatestByPetId(Long petId) {
        LambdaQueryWrapper<WeightRecordDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WeightRecordDO::getPetId, petId)
                .orderByDesc(WeightRecordDO::getRecordDate)
                .last("LIMIT 1");
        
        WeightRecordDO weightRecordDO = weightRecordMapper.selectOne(wrapper);
        return Optional.ofNullable(WeightRecordConverter.toDomain(weightRecordDO));
    }
    
    @Override
    public void deleteById(Long id) {
        weightRecordMapper.deleteById(id);
    }
    
    @Override
    public void deleteByPetId(Long petId) {
        LambdaQueryWrapper<WeightRecordDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WeightRecordDO::getPetId, petId);
        weightRecordMapper.delete(wrapper);
    }
    
    @Override
    public Optional<WeightRecord> findByPetIdAndDate(Long petId, LocalDate recordDate) {
        LambdaQueryWrapper<WeightRecordDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WeightRecordDO::getPetId, petId)
                .eq(WeightRecordDO::getRecordDate, recordDate);
        
        WeightRecordDO weightRecordDO = weightRecordMapper.selectOne(wrapper);
        return Optional.ofNullable(WeightRecordConverter.toDomain(weightRecordDO));
    }
}
