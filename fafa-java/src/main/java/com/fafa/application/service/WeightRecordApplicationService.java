package com.fafa.application.service;

import com.fafa.application.dto.weight.*;
import com.fafa.common.exception.BusinessException;
import com.fafa.domain.model.pet.Pet;
import com.fafa.domain.model.pet.PetId;
import com.fafa.domain.model.record.WeightRecord;
import com.fafa.domain.repository.PetRepository;
import com.fafa.domain.repository.WeightRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 体重记录应用服务
 */
@Service
@RequiredArgsConstructor
public class WeightRecordApplicationService {
    
    private final WeightRecordRepository weightRecordRepository;
    private final PetRepository petRepository;
    
    /**
     * 创建体重记录
     */
    @Transactional
    public Long createWeightRecord(Long userId, CreateWeightRecordRequest request) {
        // 1. 验证宠物归属
        Pet pet = petRepository.findById(new PetId(request.getPetId()))
                .orElseThrow(() -> new BusinessException("宠物不存在"));
        
        if (!pet.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该宠物");
        }
        
        // 2. 检查当天是否已有记录
        LocalDate recordDate = request.getRecordDate();
        Optional<WeightRecord> existingOpt = weightRecordRepository.findByPetIdAndDate(request.getPetId(), recordDate);
        if (existingOpt.isPresent()) {
            throw new BusinessException("当天已有体重记录，请编辑或删除后重试");
        }
        
        // 3. 创建记录
        WeightRecord weightRecord = WeightRecord.create(
                request.getPetId(),
                userId,
                recordDate,
                request.getWeight()
        );
        weightRecord.setBcsScore(request.getBcsScore());
        weightRecord.setRemarks(request.getRemarks());
        
        WeightRecord savedRecord = weightRecordRepository.save(weightRecord);
        
        // 4. 更新宠物当前体重
        pet.updateWeight(request.getWeight().doubleValue());
        petRepository.save(pet);
        
        return savedRecord.getRecordId().getValue();
    }
    
    /**
     * 查询体重记录列表
     */
    public List<WeightRecordResponse> listWeightRecords(Long userId, Long petId, LocalDate startDate, LocalDate endDate) {
        // 验证宠物归属
        Pet pet = petRepository.findById(new PetId(petId))
                .orElseThrow(() -> new BusinessException("宠物不存在"));
        
        if (!pet.getUserId().equals(userId)) {
            throw new BusinessException("无权查看该宠物的记录");
        }
        
        List<WeightRecord> records = weightRecordRepository.findByPetId(petId, startDate, endDate);
        
        return records.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * 查询体重记录详情
     */
    public WeightRecordResponse getWeightRecord(Long userId, Long id) {
        WeightRecord record = weightRecordRepository.findById(id)
                .orElseThrow(() -> new BusinessException("记录不存在"));
        
        if (!record.getUserId().equals(userId)) {
            throw new BusinessException("无权查看该记录");
        }
        
        return convertToResponse(record);
    }
    
    /**
     * 更新体重记录
     */
    @Transactional
    public void updateWeightRecord(Long userId, Long id, UpdateWeightRecordRequest request) {
        // 1. 验证记录归属
        WeightRecord record = weightRecordRepository.findById(id)
                .orElseThrow(() -> new BusinessException("记录不存在"));
        
        if (!record.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该记录");
        }
        
        // 2. 如果修改了日期，检查新日期是否已有记录
        if (!request.getRecordDate().equals(record.getRecordDate())) {
            Optional<WeightRecord> existingOpt = weightRecordRepository.findByPetIdAndDate(
                    record.getPetId(), 
                    request.getRecordDate()
            );
            if (existingOpt.isPresent() && !existingOpt.get().getRecordId().getValue().equals(id)) {
                throw new BusinessException("目标日期已有体重记录");
            }
        }
        
        // 3. 更新记录
        record.setRecordDate(request.getRecordDate());
        record.updateWeight(request.getWeight(), request.getBcsScore(), request.getRemarks());
        
        weightRecordRepository.save(record);
        
        // 4. 如果是最新记录，更新宠物当前体重
        Optional<WeightRecord> latestOpt = weightRecordRepository.findLatestByPetId(record.getPetId());
        if (latestOpt.isPresent() && latestOpt.get().getRecordId().getValue().equals(id)) {
            Pet pet = petRepository.findById(new PetId(record.getPetId()))
                    .orElseThrow(() -> new BusinessException("宠物不存在"));
            pet.updateWeight(request.getWeight().doubleValue());
            petRepository.save(pet);
        }
    }
    
    /**
     * 删除体重记录
     */
    @Transactional
    public void deleteWeightRecord(Long userId, Long id) {
        WeightRecord record = weightRecordRepository.findById(id)
                .orElseThrow(() -> new BusinessException("记录不存在"));
        
        if (!record.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该记录");
        }
        
        weightRecordRepository.deleteById(id);
    }
    
    /**
     * 获取体重趋势分析
     */
    public WeightTrendResponse getWeightTrend(Long userId, Long petId, LocalDate startDate, LocalDate endDate) {
        // 验证宠物归属
        Pet pet = petRepository.findById(new PetId(petId))
                .orElseThrow(() -> new BusinessException("宠物不存在"));
        
        if (!pet.getUserId().equals(userId)) {
            throw new BusinessException("无权查看该宠物的记录");
        }
        
        // 查询记录
        List<WeightRecord> records = weightRecordRepository.findByPetId(petId, startDate, endDate);
        
        if (records.isEmpty()) {
            return WeightTrendResponse.builder()
                    .trend("stable")
                    .message("暂无体重记录")
                    .dataPoints(List.of())
                    .build();
        }
        
        // 转换数据点
        List<WeightDataPoint> dataPoints = records.stream()
                .map(r -> WeightDataPoint.builder()
                        .date(r.getRecordDate())
                        .weight(r.getWeight())
                        .bcsScore(r.getBcsScore())
                        .build())
                .collect(Collectors.toList());
        
        // 计算趋势
        String trend = calculateTrend(records);
        String message = generateTrendMessage(records, trend);
        
        // 统计信息
        BigDecimal maxWeight = records.stream()
                .map(WeightRecord::getWeight)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        
        BigDecimal minWeight = records.stream()
                .map(WeightRecord::getWeight)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        
        BigDecimal currentWeight = records.get(0).getWeight(); // 已按日期倒序
        
        return WeightTrendResponse.builder()
                .trend(trend)
                .message(message)
                .dataPoints(dataPoints)
                .maxWeight(maxWeight)
                .minWeight(minWeight)
                .currentWeight(currentWeight)
                .build();
    }
    
    /**
     * 计算趋势
     */
    private String calculateTrend(List<WeightRecord> records) {
        if (records.size() < 2) {
            return "stable";
        }
        
        // 获取最近和最早的记录（列表已按日期倒序）
        BigDecimal latestWeight = records.get(0).getWeight();
        BigDecimal earliestWeight = records.get(records.size() - 1).getWeight();
        
        BigDecimal diff = latestWeight.subtract(earliestWeight);
        
        // 阈值：0.2kg
        if (diff.compareTo(new BigDecimal("0.2")) > 0) {
            return "increasing";
        } else if (diff.compareTo(new BigDecimal("-0.2")) < 0) {
            return "decreasing";
        } else {
            return "stable";
        }
    }
    
    /**
     * 生成趋势消息
     */
    private String generateTrendMessage(List<WeightRecord> records, String trend) {
        if (records.size() < 2) {
            return "记录较少，暂无趋势分析";
        }
        
        BigDecimal latestWeight = records.get(0).getWeight();
        BigDecimal earliestWeight = records.get(records.size() - 1).getWeight();
        BigDecimal diff = latestWeight.subtract(earliestWeight).abs();
        
        switch (trend) {
            case "increasing":
                return String.format("体重呈上升趋势，增加了 %.2f kg", diff);
            case "decreasing":
                return String.format("体重呈下降趋势，减少了 %.2f kg", diff);
            default:
                return "体重保持稳定";
        }
    }
    
    /**
     * 转换为响应对象
     */
    private WeightRecordResponse convertToResponse(WeightRecord record) {
        return WeightRecordResponse.builder()
                .id(record.getRecordId().getValue())
                .petId(record.getPetId())
                .recordDate(record.getRecordDate())
                .weight(record.getWeight())
                .bcsScore(record.getBcsScore())
                .remarks(record.getRemarks())
                .build();
    }
}
