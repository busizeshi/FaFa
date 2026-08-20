package com.fafa.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fafa.domain.model.record.ExcretionRecord;
import com.fafa.domain.repository.ExcretionRecordRepository;
import com.fafa.infrastructure.persistence.converter.ExcretionRecordConverter;
import com.fafa.infrastructure.persistence.dataobject.ExcretionRecordDO;
import com.fafa.infrastructure.persistence.mapper.ExcretionRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 排便记录仓储实现
 */
@Repository
@RequiredArgsConstructor
public class ExcretionRecordRepositoryImpl implements ExcretionRecordRepository {
    
    private final ExcretionRecordMapper excretionRecordMapper;
    
    @Override
    public ExcretionRecord save(ExcretionRecord excretionRecord) {
        ExcretionRecordDO excretionRecordDO = ExcretionRecordConverter.toDO(excretionRecord);
        
        if (excretionRecordDO.getId() == null) {
            excretionRecordMapper.insert(excretionRecordDO);
        } else {
            excretionRecordMapper.updateById(excretionRecordDO);
        }
        
        return ExcretionRecordConverter.toDomain(excretionRecordDO);
    }
    
    @Override
    public Optional<ExcretionRecord> findById(Long id) {
        ExcretionRecordDO excretionRecordDO = excretionRecordMapper.selectById(id);
        return Optional.ofNullable(ExcretionRecordConverter.toDomain(excretionRecordDO));
    }
    
    @Override
    public List<ExcretionRecord> findByPetId(Long petId, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<ExcretionRecordDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExcretionRecordDO::getPetId, petId);
        
        if (startDate != null) {
            wrapper.ge(ExcretionRecordDO::getExcretionTime, LocalDateTime.of(startDate, LocalTime.MIN));
        }
        if (endDate != null) {
            wrapper.le(ExcretionRecordDO::getExcretionTime, LocalDateTime.of(endDate, LocalTime.MAX));
        }
        
        wrapper.orderByDesc(ExcretionRecordDO::getExcretionTime);
        
        return excretionRecordMapper.selectList(wrapper).stream()
                .map(ExcretionRecordConverter::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public void deleteById(Long id) {
        excretionRecordMapper.deleteById(id);
    }
    
    @Override
    public void deleteByPetId(Long petId) {
        LambdaQueryWrapper<ExcretionRecordDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExcretionRecordDO::getPetId, petId);
        excretionRecordMapper.delete(wrapper);
    }
    
    @Override
    public int countByPetIdAndTypeAndDateRange(Long petId, String type, LocalDate startDate, LocalDate endDate) {
        return excretionRecordMapper.countByPetIdAndTypeAndDateRange(petId, type, startDate, endDate);
    }
}
