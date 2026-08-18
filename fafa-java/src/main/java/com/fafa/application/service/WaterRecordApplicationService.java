package com.fafa.application.service;

import com.fafa.application.dto.water.*;
import com.fafa.common.exception.BusinessException;
import com.fafa.domain.model.pet.Pet;
import com.fafa.domain.model.record.WaterRecord;
import com.fafa.domain.repository.PetRepository;
import com.fafa.domain.repository.WaterRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 饮水记录应用服务
 */
@Service
@RequiredArgsConstructor
public class WaterRecordApplicationService {
    
    private final WaterRecordRepository waterRecordRepository;
    private final PetRepository petRepository;
    
    /**
     * 创建饮水记录
     */
    @Transactional
    public Long createWaterRecord(Long userId, CreateWaterRecordRequest request) {
        // 验证宠物归属
        Pet pet = petRepository.findById(request.getPetId())
                .orElseThrow(() -> new BusinessException("宠物不存在"));
        
        if (!pet.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该宠物"));
        }
        
        // 创建记录
        WaterRecord waterRecord = WaterRecord.create(
                request.getPetId(),
                userId,
                request.getWaterTime(),
                request.getAmount()
        );
        waterRecord.setRemarks(request.getRemarks());
        
        WaterRecord savedRecord = waterRecordRepository.save(waterRecord);
        
        return savedRecord.getRecordId().getValue();
    }
    
    /**
     * 查询饮水记录列表
     */
    public List<WaterRecordResponse> listWaterRecords(Long userId, Long petId, LocalDate startDate, LocalDate endDate) {
        // 验证宠物归属
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new BusinessException("宠物不存在"));
        
        if (!pet.getUserId().equals(userId)) {
            throw new BusinessException("无权查看该宠物的记录"));
        }
        
        List<WaterRecord> records = waterRecordRepository.findByPetId(petId, startDate, endDate);
        
        return records.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * 查询饮水记录详情
     */
    public WaterRecordResponse getWaterRecord(Long userId, Long id) {
        WaterRecord record = waterRecordRepository.findById(id)
                .orElseThrow(() -> new BusinessException("记录不存在"));
        
        if (!record.getUserId().equals(userId)) {
            throw new BusinessException("无权查看该记录"));
        }
        
        return convertToResponse(record);
    }
    
    /**
     * 删除饮水记录
     */
    @Transactional
    public void deleteWaterRecord(Long userId, Long id) {
        WaterRecord record = waterRecordRepository.findById(id)
                .orElseThrow(() -> new BusinessException("记录不存在"));
        
        if (!record.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该记录"));
        }
        
        waterRecordRepository.deleteById(id);
    }
    
    /**
     * 统计饮水量
     */
    public WaterStatisticsResponse getWaterStatistics(Long userId, Long petId, LocalDate startDate, LocalDate endDate) {
        // 验证宠物归属
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new BusinessException("宠物不存在"));
        
        if (!pet.getUserId().equals(userId)) {
            throw new BusinessException("无权查看该宠物的记录"));
        }
        
        // 统计总量
        int totalAmount = waterRecordRepository.sumAmountByPetIdAndDateRange(petId, startDate, endDate);
        
        // 计算天数和平均值
        long days = startDate != null && endDate != null 
                ? java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1 
                : 1;
        
        int averageAmount = days > 0 ? (int) (totalAmount / days) : 0;
        
        return WaterStatisticsResponse.builder()
                .totalAmount(totalAmount)
                .averageAmount(averageAmount)
                .days((int) days)
                .build();
    }
    
    /**
     * 转换为响应对象
     */
    private WaterRecordResponse convertToResponse(WaterRecord record) {
        return WaterRecordResponse.builder()
                .id(record.getRecordId().getValue())
                .petId(record.getPetId())
                .waterTime(record.getWaterTime())
                .amount(record.getAmount())
                .remarks(record.getRemarks())
                .build();
    }
}
