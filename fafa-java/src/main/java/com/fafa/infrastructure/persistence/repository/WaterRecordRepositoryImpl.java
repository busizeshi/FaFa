package com.fafa.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fafa.domain.model.record.WaterRecord;
import com.fafa.domain.repository.WaterRecordRepository;
import com.fafa.infrastructure.persistence.converter.WaterRecordConverter;
import com.fafa.infrastructure.persistence.dataobject.WaterRecordDO;
import com.fafa.infrastructure.persistence.mapper.WaterRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 饮水记录仓储实现
 */
@Repository
@RequiredArgsConstructor
public class WaterRecordRepositoryImpl implements WaterRecordRepository {
    
    private final WaterRecordMapper waterRecordMapper;
    
    @Override
    public WaterRecord save(WaterRecord waterRecord) {
        WaterRecordDO waterRecordDO = WaterRecordConverter.toDO(waterRecord);
        
        if (waterRecordDO.getId() == null) {
            waterRecordMapper.insert(waterRecordDO);
        } else {
            waterRecordMapper.updateById(waterRecordDO);
        }
        
        return WaterRecordConverter.toDomain(waterRecordDO);
    }
    
    @Override
    public Optional<WaterRecord> findById(Long id) {
        WaterRecordDO waterRecordDO = waterRecordMapper.selectById(id);
        return Optional.ofNullable(WaterRecordConverter.toDomain(waterRecordDO));
    }
    
    @Override
    public List<WaterRecord> findByPetId(Long petId, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<WaterRecordDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WaterRecordDO::getPetId, petId);
        
        if (startDate != null) {
            wrapper.ge(WaterRecordDO::getWaterTime, LocalDateTime.of(startDate, LocalTime.MIN));
        }
        if (endDate != null) {
            wrapper.le(WaterRecordDO::getWaterTime, LocalDateTime.of(endDate, LocalTime.MAX));
        }
        
        wrapper.orderByDesc(WaterRecordDO::getWaterTime);
        
        return waterRecordMapper.selectList(wrapper).stream()
                .map(WaterRecordConverter::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public void deleteById(Long id) {
        waterRecordMapper.deleteById(id);
    }
    
    @Override
    public void deleteByPetId(Long petId) {
        LambdaQueryWrapper<WaterRecordDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WaterRecordDO::getPetId, petId);
        waterRecordMapper.delete(wrapper);
    }
    
    @Override
    public int sumAmountByPetIdAndDateRange(Long petId, LocalDate startDate, LocalDate endDate) {
        return waterRecordMapper.sumAmountByPetIdAndDateRange(petId, startDate, endDate);
    }
}
