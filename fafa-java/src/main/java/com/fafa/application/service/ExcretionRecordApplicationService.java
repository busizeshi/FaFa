package com.fafa.application.service;

import com.fafa.application.dto.excretion.*;
import com.fafa.common.exception.BusinessException;
import com.fafa.domain.model.pet.Pet;
import com.fafa.domain.model.pet.PetId;
import com.fafa.domain.model.record.ExcretionRecord;
import com.fafa.domain.repository.ExcretionRecordRepository;
import com.fafa.domain.repository.PetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 排便记录应用服务
 */
@Service
@RequiredArgsConstructor
public class ExcretionRecordApplicationService {
    
    private final ExcretionRecordRepository excretionRecordRepository;
    private final PetRepository petRepository;
    
    /**
     * 创建排便记录
     */
    @Transactional
    public Long createExcretionRecord(Long userId, CreateExcretionRecordRequest request) {
        // 验证宠物归属
        Pet pet = petRepository.findById(new PetId(request.getPetId()))
                .orElseThrow(() -> new BusinessException("宠物不存在"));
        
        if (!pet.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该宠物");
        }
        
        // 创建记录
        ExcretionRecord excretionRecord = ExcretionRecord.create(
                request.getPetId(),
                userId,
                request.getExcretionTime(),
                request.getType()
        );
        excretionRecord.setColor(request.getColor());
        excretionRecord.setShape(request.getShape());
        excretionRecord.setAbnormal(request.getAbnormal() != null ? request.getAbnormal() : false);
        excretionRecord.setRemarks(request.getRemarks());
        
        ExcretionRecord savedRecord = excretionRecordRepository.save(excretionRecord);
        
        return savedRecord.getRecordId().getValue();
    }
    
    /**
     * 查询排便记录列表
     */
    public List<ExcretionRecordResponse> listExcretionRecords(Long userId, Long petId, LocalDate startDate, LocalDate endDate) {
        // 验证宠物归属
        Pet pet = petRepository.findById(new PetId(petId))
                .orElseThrow(() -> new BusinessException("宠物不存在"));
        
        if (!pet.getUserId().equals(userId)) {
            throw new BusinessException("无权查看该宠物的记录");
        }
        
        List<ExcretionRecord> records = excretionRecordRepository.findByPetId(petId, startDate, endDate);
        
        return records.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * 查询排便记录详情
     */
    public ExcretionRecordResponse getExcretionRecord(Long userId, Long id) {
        ExcretionRecord record = excretionRecordRepository.findById(id)
                .orElseThrow(() -> new BusinessException("记录不存在"));
        
        if (!record.getUserId().equals(userId)) {
            throw new BusinessException("无权查看该记录");
        }
        
        return convertToResponse(record);
    }
    
    /**
     * 删除排便记录
     */
    @Transactional
    public void deleteExcretionRecord(Long userId, Long id) {
        ExcretionRecord record = excretionRecordRepository.findById(id)
                .orElseThrow(() -> new BusinessException("记录不存在"));
        
        if (!record.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该记录");
        }
        
        excretionRecordRepository.deleteById(id);
    }
    
    /**
     * 统计排便次数
     */
    public ExcretionStatisticsResponse getExcretionStatistics(Long userId, Long petId, 
                                                              String type, LocalDate startDate, LocalDate endDate) {
        // 验证宠物归属
        Pet pet = petRepository.findById(new PetId(petId))
                .orElseThrow(() -> new BusinessException("宠物不存在"));
        
        if (!pet.getUserId().equals(userId)) {
            throw new BusinessException("无权查看该宠物的记录");
        }
        
        // 统计次数
        int totalCount = excretionRecordRepository.countByPetIdAndTypeAndDateRange(petId, type, startDate, endDate);
        
        // 计算天数和平均值
        long days = startDate != null && endDate != null 
                ? java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1 
                : 1;
        
        double averageCount = days > 0 ? (double) totalCount / days : 0;
        
        return ExcretionStatisticsResponse.builder()
                .totalCount(totalCount)
                .averageCount(averageCount)
                .days((int) days)
                .type(type)
                .build();
    }
    
    /**
     * 转换为响应对象
     */
    private ExcretionRecordResponse convertToResponse(ExcretionRecord record) {
        return ExcretionRecordResponse.builder()
                .id(record.getRecordId().getValue())
                .petId(record.getPetId())
                .excretionTime(record.getExcretionTime())
                .type(record.getType())
                .color(record.getColor())
                .shape(record.getShape())
                .abnormal(record.getAbnormal())
                .remarks(record.getRemarks())
                .build();
    }
}
