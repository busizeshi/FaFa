package com.fafa.application.service;

import cn.hutool.core.util.StrUtil;
import com.fafa.common.exception.BusinessException;
import com.fafa.domain.model.pet.Pet;
import com.fafa.domain.model.pet.PetId;
import com.fafa.domain.model.record.FeedRecord;
import com.fafa.domain.model.record.RecordId;
import com.fafa.domain.repository.FeedRecordRepository;
import com.fafa.domain.repository.PetRepository;
import com.fafa.interfaces.dto.request.CreateFeedRecordRequest;
import com.fafa.interfaces.dto.request.UpdateFeedRecordRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 喂食记录应用服务
 * 
 * @author FaFa Team
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedRecordApplicationService {
    
    private final FeedRecordRepository feedRecordRepository;
    private final PetRepository petRepository;
    
    /**
     * 创建喂食记录
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createFeedRecord(Long userId, CreateFeedRecordRequest request) {
        log.info("创建喂食记录: userId={}, petId={}", userId, request.getPetId());
        
        // 1. 校验宠物归属
        Pet pet = petRepository.findById(new PetId(request.getPetId()))
                .orElseThrow(() -> new BusinessException("宠物不存在"));
        
        if (!pet.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该宠物");
        }
        
        // 2. 创建记录
        FeedRecord feedRecord = FeedRecord.builder()
                .petId(request.getPetId())
                .userId(userId)
                .feedTime(request.getFeedTime() != null ? request.getFeedTime() : LocalDateTime.now())
                .foodName(request.getFoodName())
                .foodType(request.getFoodType())
                .amount(request.getAmount())
                .unit(request.getUnit())
                .brand(request.getBrand())
                .remarks(request.getRemarks())
                .build();
        
        FeedRecord savedRecord = feedRecordRepository.save(feedRecord);
        
        log.info("喂食记录创建成功: recordId={}", savedRecord.getRecordId().getValue());
        return savedRecord.getRecordId().getValue();
    }
    
    /**
     * 快捷喂食（使用上次数据）
     */
    @Transactional(rollbackFor = Exception.class)
    public Long quickFeed(Long userId, Long petId) {
        log.info("快捷喂食: userId={}, petId={}", userId, petId);
        
        // 1. 校验宠物归属
        Pet pet = petRepository.findById(new PetId(petId))
                .orElseThrow(() -> new BusinessException("宠物不存在"));
        
        if (!pet.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该宠物");
        }
        
        // 2. 获取最近一次喂食记录
        Optional<FeedRecord> latestOpt = feedRecordRepository.findLatestByPetId(petId);
        
        if (latestOpt.isEmpty()) {
            throw new BusinessException("没有历史喂食记录，请使用完整记录");
        }
        
        FeedRecord latest = latestOpt.get();
        
        // 3. 复制数据，更新时间
        FeedRecord newRecord = FeedRecord.builder()
                .petId(petId)
                .userId(userId)
                .feedTime(LocalDateTime.now())
                .foodName(latest.getFoodName())
                .foodType(latest.getFoodType())
                .amount(latest.getAmount())
                .unit(latest.getUnit())
                .brand(latest.getBrand())
                .build();
        
        FeedRecord savedRecord = feedRecordRepository.save(newRecord);
        
        log.info("快捷喂食成功: recordId={}", savedRecord.getRecordId().getValue());
        return savedRecord.getRecordId().getValue();
    }
    
    /**
     * 查询喂食记录列表
     */
    public List<FeedRecord> listFeedRecords(Long userId, Long petId, LocalDate startDate, LocalDate endDate, int page, int size) {
        // 校验宠物归属
        Pet pet = petRepository.findById(new PetId(petId))
                .orElseThrow(() -> new BusinessException("宠物不存在"));
        
        if (!pet.getUserId().equals(userId)) {
            throw new BusinessException("无权查看该宠物记录");
        }
        
        return feedRecordRepository.findByPetId(petId, startDate, endDate, page, size);
    }
    
    /**
     * 查询喂食记录详情
     */
    public FeedRecord getFeedRecord(Long userId, Long recordId) {
        FeedRecord feedRecord = feedRecordRepository.findById(RecordId.of(recordId))
                .orElseThrow(() -> new BusinessException("记录不存在"));
        
        // 校验权限
        if (!feedRecord.getUserId().equals(userId)) {
            throw new BusinessException("无权查看该记录");
        }
        
        return feedRecord;
    }
    
    /**
     * 更新喂食记录
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateFeedRecord(Long userId, Long recordId, UpdateFeedRecordRequest request) {
        log.info("更新喂食记录: userId={}, recordId={}", userId, recordId);
        
        // 1. 加载记录
        FeedRecord feedRecord = feedRecordRepository.findById(RecordId.of(recordId))
                .orElseThrow(() -> new BusinessException("记录不存在"));
        
        // 2. 权限校验
        if (!feedRecord.getUserId().equals(userId)) {
            throw new BusinessException("无权修改该记录");
        }
        
        // 3. 更新字段
        if (request.getFeedTime() != null) {
            feedRecord.setFeedTime(request.getFeedTime());
        }
        if (StrUtil.isNotBlank(request.getFoodName())) {
            feedRecord.setFoodName(request.getFoodName());
        }
        if (StrUtil.isNotBlank(request.getFoodType())) {
            feedRecord.setFoodType(request.getFoodType());
        }
        if (StrUtil.isNotBlank(request.getAmount())) {
            feedRecord.setAmount(request.getAmount());
        }
        if (StrUtil.isNotBlank(request.getUnit())) {
            feedRecord.setUnit(request.getUnit());
        }
        if (StrUtil.isNotBlank(request.getBrand())) {
            feedRecord.setBrand(request.getBrand());
        }
        if (request.getRemarks() != null) {
            feedRecord.setRemarks(request.getRemarks());
        }
        
        // 4. 持久化
        feedRecordRepository.save(feedRecord);
        
        log.info("喂食记录更新成功: recordId={}", recordId);
    }
    
    /**
     * 删除喂食记录
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteFeedRecord(Long userId, Long recordId) {
        log.info("删除喂食记录: userId={}, recordId={}", userId, recordId);
        
        // 1. 加载记录
        FeedRecord feedRecord = feedRecordRepository.findById(RecordId.of(recordId))
                .orElseThrow(() -> new BusinessException("记录不存在"));
        
        // 2. 权限校验
        if (!feedRecord.getUserId().equals(userId)) {
            throw new BusinessException("无权删除该记录");
        }
        
        // 3. 删除
        feedRecordRepository.deleteById(RecordId.of(recordId));
        
        log.info("喂食记录删除成功: recordId={}", recordId);
    }
    
    /**
     * 统计喂食次数
     */
    public int countFeedRecords(Long userId, Long petId, LocalDate startDate, LocalDate endDate) {
        // 校验宠物归属
        Pet pet = petRepository.findById(new PetId(petId))
                .orElseThrow(() -> new BusinessException("宠物不存在"));
        
        if (!pet.getUserId().equals(userId)) {
            throw new BusinessException("无权查看该宠物记录");
        }
        
        return feedRecordRepository.countByPetId(petId, startDate, endDate);
    }
}
